# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Isolated Kind smoke with real PostgreSQL: HA install, persistence, upgrade and rollback."""
import argparse
import json
import os
import secrets
import shutil
import subprocess
import time
import uuid
from pathlib import Path

import yaml
from check import ROOT, POSTGRES, free_port, http_tests, keypair, ready, request, token
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from approval.fixtures import seed_approvals


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--image',default='olo-toolgate-control:module05');parser.add_argument('--client-downloads',action='store_true');args=parser.parse_args()
    paths={name:os.environ.get('TOOLGATE_'+name.upper()+'_PATH') or shutil.which(name) for name in ('kind','kubectl','helm')}
    if any(not value for value in paths.values()): raise SystemExit('Native Kind, Helm and kubectl are required')
    cluster='control-module05-'+uuid.uuid4().hex[:8];folder=ROOT/'.dev'/cluster;folder.mkdir(parents=True)
    config=folder/'kubeconfig';env=dict(os.environ,KUBECONFIG=str(config))
    def run(tool,*rest,capture=False,input=None):
        print('+ '+tool+' '+' '.join(rest[:3]),flush=True)
        return subprocess.run([paths[tool],*rest],cwd=ROOT,env=env,check=True,text=True,capture_output=capture,input=input)
    forward=None
    try:
        run('kind','create','cluster','--name',cluster,'--image','kindest/node:v1.32.2@sha256:142f543559cc55d64e1ab9341df08e5ced84bd2e893736da8f51320f26f5950b','--wait','120s','--kubeconfig',str(config))
        run('kind','load','docker-image',args.image,'--name',cluster)
        # Docker's containerd store may retain a multi-platform index with only this
        # platform downloaded. Export the selected platform so Kind imports no absent digest.
        architecture=subprocess.check_output(['docker','info','--format','{{.Architecture}}'],text=True).strip()
        platform='linux/'+{'x86_64':'amd64','aarch64':'arm64','amd64':'amd64','arm64':'arm64'}[architecture]
        archive=folder/'postgres.tar'
        subprocess.run(['docker','image','save','--platform='+platform,'-o',str(archive),POSTGRES.split('@')[0]],cwd=ROOT,check=True)
        run('kind','load','image-archive',str(archive),'--name',cluster)
        archive.unlink()
        key,public=keypair(folder);password=secrets.token_urlsafe(32)
        secret_files={}
        for name,value in {'username':'control_app','password':password,'migrationUsername':'control_migrator','migrationPassword':password}.items():
            path=folder/name;path.write_text(value,encoding='utf-8');path.chmod(0o600);secret_files[name]=path
        run('kubectl','create','secret','generic','control-db',*['--from-file='+name+'='+str(path) for name,path in secret_files.items()])
        run('kubectl','create','secret','generic','control-identity','--from-file=public.pem='+str(public))
        from cryptography.hazmat.primitives.asymmetric import rsa
        from cryptography.hazmat.primitives import serialization
        signing=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        signing_path=folder/'policy-signing.pem'
        signing_path.write_bytes(signing.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        signing_path.chmod(0o600)
        run('kubectl','create','secret','generic','policy-signing','--from-file=private.pem='+str(signing_path))
        # This PostgreSQL dependency exists only in the owned smoke cluster, outside the production chart.
        db=[{'apiVersion':'v1','kind':'Pod','metadata':{'name':'control-postgres','labels':{'app':'control-postgres'}},'spec':{
            'containers':[{'name':'postgres','image':POSTGRES.split('@')[0],'imagePullPolicy':'Never',
                'env':[{'name':'POSTGRES_PASSWORD','valueFrom':{'secretKeyRef':{'name':'control-db','key':'password'}}}],
                'ports':[{'containerPort':5432}],'resources':{'requests':{'cpu':'100m','memory':'128Mi'},'limits':{'cpu':'1','memory':'512Mi'}},
                'readinessProbe':{'exec':{'command':['pg_isready','-h','127.0.0.1','-U','postgres']},'periodSeconds':2},
                'volumeMounts':[{'name':'data','mountPath':'/var/lib/postgresql/data'}]}],
            'volumes':[{'name':'data','emptyDir':{}}]}},
            {'apiVersion':'v1','kind':'Service','metadata':{'name':'control-postgres'},'spec':{'selector':{'app':'control-postgres'},'ports':[{'port':5432}]}}]
        run('kubectl','apply','-f','-',input=yaml.safe_dump_all(db))
        run('kubectl','wait','--for=condition=Ready','pod/control-postgres','--timeout=120s')
        sql=f"CREATE ROLE toolgate_control_runtime NOLOGIN; CREATE ROLE control_migrator LOGIN PASSWORD '{password}'; CREATE ROLE control_app LOGIN PASSWORD '{password}' IN ROLE toolgate_control_runtime; CREATE DATABASE control OWNER control_migrator;"
        run('kubectl','exec','-i','control-postgres','--','psql','-U','postgres','-v','ON_ERROR_STOP=1',input=sql,capture=True)
        registry,tag=args.image.rsplit(':',1);namespace,repo=registry.rsplit('/',1) if '/' in registry else ('docker.io/library',registry)
        values={'global':{'imageRegistry':namespace},'control':{'enabled':True,'developmentMode':True,'publicKeySecret':'control-identity',
            'bundle':{'enabled':True,'signingSecret':'policy-signing','keyId':'bundle-cluster-1'},
            'approval':{'enabled':True},
            'clientDownloads':{'enabled':args.client_downloads},
            'image':{'repository':repo,'tag':tag,'pullPolicy':'Never'},'database':{'host':'control-postgres','name':'control','credentialsSecret':'control-db','sslMode':'disable'},
            'networkPolicy':{'runtimeFrom':[{'podSelector':{}}],'databaseTo':[{'podSelector':{'matchLabels':{'app':'control-postgres'}}}],
                             'dnsTo':[{'namespaceSelector':{'matchLabels':{'kubernetes.io/metadata.name':'kube-system'}}}]}}}
        path=folder/'values.yaml';path.write_text(yaml.safe_dump(values),encoding='utf-8')
        release='control-smoke';deployment='control-smoke-olo-toolgate-control';chart='deploy/helm/olo-toolgate'
        run('helm','upgrade','--install',release,chart,'-f',str(path),'--wait','--timeout','180s')
        def connect():
            nonlocal forward
            if forward is not None: forward.terminate();forward.wait(timeout=10)
            run('kubectl','rollout','status','deployment/'+deployment,'--timeout=180s')
            pods=json.loads(run('kubectl','get','pods','-l','app.kubernetes.io/instance='+release,'-o','json',capture=True).stdout)
            active=[pod for pod in pods['items'] if not pod['metadata'].get('deletionTimestamp')]
            assert len(active)==2 and all(any(c['type']=='Ready' and c['status']=='True' for c in pod['status'].get('conditions',[])) for pod in active)
            api_port,management_port=free_port(),free_port()
            with (folder/'port-forward.log').open('a',encoding='utf-8') as log:
                forward=subprocess.Popen([paths['kubectl'],'port-forward','pod/'+active[0]['metadata']['name'],f'{api_port}:8082',f'{management_port}:9092'],cwd=ROOT,env=env,stdout=log,stderr=log)
            management=f'http://127.0.0.1:{management_port}';ready(management,forward)
            runtime=f'http://127.0.0.1:{api_port}'
            if args.client_downloads:
                status,document,_=request(runtime+'/api/public/v1/clients')
                assert status==200
                assert {a['platform'] for a in json.loads(document)['artifacts']}=={'WINDOWS','MACOS','LINUX'}
            return runtime,management
        runtime,management=connect();http_tests(runtime,management,key)
        # The transport oversize check above closes kubectl's tunnel; reconnect.
        runtime,management=connect()
        admin=token(key);api='/api/control/v1'
        revision=json.loads(request(runtime+api+'/config/export',admin)[1])['revision']
        published=request(runtime+api+'/bundles/publish',admin,{'directoryRevision':revision,'expectedSequence':0,'lifetimeMs':600000,'graceMs':0},'POST',{'Idempotency-Key':'cluster-publish'})
        assert published[0]==201,published[:2]
        signed=published[1]
        import base64
        from cryptography.hazmat.primitives import hashes
        from cryptography.hazmat.primitives.asymmetric import padding
        parts=json.loads(signed)['jws'].split('.')
        signing.public_key().verify(base64.urlsafe_b64decode(parts[2]+'=='),(parts[0]+'.'+parts[1]).encode(),padding.PKCS1v15(),hashes.SHA256())
        assert request(runtime+api+'/bundles/current',admin)[1]==signed
        approver,approval_ids=seed_approvals(runtime,key,tenant='cluster-approval')
        approval_api=runtime+api+'/approvals'
        once_id=approval_ids['once']
        decided=request(approval_api+'/'+once_id+'/decision',approver,{'decision':'APPROVE_ONCE','expectedRevision':1},'POST',{'Idempotency-Key':'cluster-approve'})
        assert decided[0]==200,decided[:2]
        record=json.loads(request(approval_api+'/'+once_id,approver)[1])
        machine=token(key,tenant_id='cluster-approval',groups=['toolgate-approval-gateway'])
        lease=request(approval_api+'/resolve',machine,{'input':record['input'],'policyVersion':record['policyVersion']},'POST')
        assert lease[0]==200,lease[:2]
        lease=json.loads(lease[1]);assert lease['state']=='CONSUMED'
        use={'input':lease['input'],'policyVersion':lease['policyVersion'],'approvalId':lease['approvalId'],'permitId':lease['permitId']}
        assert request(approval_api+'/permits/consume',machine,use,'POST')[0]==200
        assert request(approval_api+'/permits/consume',machine,use,'POST')[0]==409
        consumed_record=request(approval_api+'/'+once_id,approver)[1]
        values['control']['limits']={'maxRecords':511};path.write_text(yaml.safe_dump(values),encoding='utf-8')
        run('helm','upgrade',release,chart,'-f',str(path),'--wait','--timeout','180s')
        runtime,management=connect()
        assert request(runtime+'/api/control/v1/users/user',token(key))[0]==200
        assert request(runtime+api+'/bundles/current',token(key))[1]==signed
        assert request(runtime+api+'/approvals/'+once_id,approver)[1]==consumed_record
        run('helm','rollback',release,'1','--wait','--timeout','180s')
        runtime,management=connect()
        assert request(runtime+'/api/control/v1/users/user',token(key))[0]==200
        assert request(runtime+api+'/bundles/versions/1',token(key))[1]==signed
        assert request(runtime+api+'/approvals/'+once_id,approver)[1]==consumed_record
        configmap=json.loads(run('kubectl','get','configmap',deployment,'-o','json',capture=True).stdout)
        assert configmap['data']['TOOLGATE_CONTROL_MAX_RECORDS']=='512'
        output=ROOT/'build/control';output.mkdir(parents=True,exist_ok=True)
        image_id=subprocess.check_output(['docker','image','inspect','--format','{{.Id}}',args.image],text=True).strip()
        (output/'cluster-smoke.json').write_text(json.dumps({'image':args.image,'imageId':image_id,'postgres':True,'replicas':2,'installReady':True,'jwtRolesIsolation':True,'persistentUpgrade':True,'rollbackReady':True,'externalSigningSecret':True,'signedPublicationVerified':True,'immutableBundleSurvivesUpgradeRollback':True,'approvalAtomicConsume':True,'spentApprovalSurvivesUpgradeRollback':True,'kindVersion':'0.27.0','kubernetes':'1.32.2'},indent=2)+'\n',encoding='utf-8')
        print('Control Kind HA install, authenticated API, persistent upgrade and rollback passed')
    except Exception:
        # Preserve local diagnostics without printing pod environment or Secret data.
        for command,name in [(('get','pods','-o','json'),'pods.json'),(('logs','-l','app.kubernetes.io/instance=control-smoke','--all-containers=true','--tail=100'),'application.log')]:
            result=subprocess.run([paths['kubectl'],*command],cwd=ROOT,env=env,text=True,capture_output=True)
            (folder/name).write_text(result.stdout+result.stderr,encoding='utf-8')
        raise
    finally:
        if forward is not None: forward.terminate();forward.wait(timeout=10)
        # Exact uniquely owned name; never act on the user's current cluster/context.
        subprocess.run([paths['kind'],'delete','cluster','--name',cluster],cwd=ROOT,env=env,check=True)


if __name__=='__main__':main()
