# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real PostgreSQL → production Control signer → production Gateway E2E.

Owns only temporary containers and credentials. Literal loopback HTTP is confined
to their shared isolated test network namespace; production chart requires TLS.
"""
import argparse
import base64
import hashlib
import json
import os
import secrets
import subprocess
import sys
import tempfile
import time
import urllib.error
from pathlib import Path
from cryptography.hazmat.primitives import serialization, hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from control.check import ROOT, database, environment, keypair, ready, request, run, token


def b64(data): return base64.urlsafe_b64encode(data).decode().rstrip('=')


def signing_key(work, name):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    path = work/(name+'.pem')
    path.write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    # The temporary parent is private (0700); only this read-only container mount
    # needs a file readable by UID 65532, which differs from the CI runner UID.
    path.chmod(0o444)
    numbers = key.public_key().public_numbers()
    return key, path, {'keyId':name, 'modulus':b64(numbers.n.to_bytes(256,'big')), 'exponent':'AQAB'}


def until(probe, expected, description, timeout=30):
    deadline = time.monotonic()+timeout
    while True:
        try:
            value=probe()
            if value==expected: return value
        except (urllib.error.URLError, ConnectionError, TimeoutError): pass
        if time.monotonic()>deadline: raise AssertionError('Timed out: '+description)
        time.sleep(.05)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--control-image',default='olo-toolgate-control:module05')
    parser.add_argument('--gateway-image',default='olo-toolgate-gateway:module05')
    parser.add_argument('--build',action='store_true',help='Build both production images before the real E2E gate')
    args=parser.parse_args()
    if args.build:
        version=(ROOT/'VERSION').read_text().strip()
        revision=run(['git','rev-parse','HEAD'],capture_output=True,text=True).stdout.strip()
        for name,image in [('control-plane',args.control_image),('gateway',args.gateway_image)]:
            run(['docker','build','-f','apps/'+name+'/Dockerfile','--build-arg','VERSION='+version,'--build-arg','REVISION='+revision,'-t',image,'.'])
    control_id=run(['docker','image','inspect','--format','{{.Id}}',args.control_image],capture_output=True,text=True).stdout.strip()
    gateway_id=run(['docker','image','inspect','--format','{{.Id}}',args.gateway_image],capture_output=True,text=True).stdout.strip()
    output=ROOT/'build/policy'; output.mkdir(parents=True,exist_ok=True)
    (ROOT/'.dev').mkdir(parents=True, exist_ok=True)
    with database() as db, tempfile.TemporaryDirectory(prefix='policy-e2e-',dir=ROOT/'.dev') as temp:
        work=Path(temp);control_work=work/'control';gateway_work=work/'gateway'
        control_work.mkdir(mode=0o755);gateway_work.mkdir(mode=0o755)
        identity, public=keypair(control_work)
        old, old_path, old_public=signing_key(control_work,'bundle-old')
        new, new_path, new_public=signing_key(control_work,'bundle-new')
        ring=gateway_work/'keyring.json';ring.write_text(json.dumps({'keys':[old_public,new_public]}),encoding='utf-8')
        admin=token(identity); reader=token(identity,groups=['toolgate-reader'])
        gateway_identity=token(identity,groups=['toolgate-bundle-reader'])
        access=gateway_work/'access-token';access.write_text(gateway_identity,encoding='utf-8');access.chmod(0o644)
        runtime_token=secrets.token_urlsafe(48)
        credential=[{'tokenSha256':hashlib.sha256(runtime_token.encode()).hexdigest(),'tenantId':'http-tenant',
                     'userId':'alice','agentId':'agent-demo','expiresAtUnixMs':int(time.time()*1000)+300000}]
        (gateway_work/'credentials.json').write_text(json.dumps(credential),encoding='utf-8')
        cfg=json.loads((ROOT/'docs/examples/gateway-static.json').read_text())
        cfg.update(listen='0.0.0.0:8081',managementListen='0.0.0.0:9091',trustedTlsProxy=True)
        cfg.pop('policy')
        cfg['bundleSource']={'url':'http://127.0.0.1:8082/api/control/v1/bundles/current','tenantId':'http-tenant',
                             'issuer':'control','audience':'gateway','keyringPath':'/config/keyring.json','tokenPath':'/config/access-token',
                             'minimumSequence':0,'maxGraceMs':5000,'pollIntervalMs':100,'fetchTimeoutMs':500,'developmentLoopbackHttp':True}
        (gateway_work/'gateway.json').write_text(json.dumps(cfg),encoding='utf-8')
        env=environment(db,Path('/config/jwt-public.pem'))
        env.update(QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://127.0.0.1:5432/control?sslmode=disable',
                   TOOLGATE_CONTROL_BUNDLE_ENABLED='true',TOOLGATE_CONTROL_BUNDLE_KEY_ID='bundle-old',
                   TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH='/config/bundle-old.pem')
        env_path=work/'control.env'
        def env_file():
            env_path.write_text('\n'.join(k+'='+v for k,v in env.items() if k.startswith(('QUARKUS_','MP_JWT_','TOOLGATE_CONTROL_')))+'\n',encoding='utf-8');env_path.chmod(0o600)
        containers=[]
        def start_control():
            env_file()
            container=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges',
                '--network','container:'+db['container'],'--memory=768m','--cpus=2','--tmpfs','/tmp:rw,noexec,nosuid,size=64m,uid=65532,gid=65532',
                '--env-file',str(env_path),'-v',f'{control_work.as_posix()}:/config:ro',control_id],capture_output=True,text=True).stdout.strip()
            containers.append(container);ready('http://127.0.0.1:'+db['managementPort']);return container
        def start_gateway():
            container=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges',
                '--network','container:'+db['container'],'--memory=256m','--cpus=1','-v',f'{gateway_work.as_posix()}:/config:ro',
                '-e','TOOLGATE_GATEWAY_CONFIG=/config/gateway.json','-e','TOOLGATE_GATEWAY_CREDENTIALS=/config/credentials.json',gateway_id],capture_output=True,text=True).stdout.strip()
            containers.append(container);return container
        api='http://127.0.0.1:'+db['runtimePort']+'/api/control/v1'
        gateway='http://127.0.0.1:'+db['gatewayPort'];management='http://127.0.0.1:'+db['gatewayManagementPort']
        payload={'toolId':'files.read','action':'read','arguments':{'path':'workspace/readme.txt','private':'secret-redaction-sentinel'}}
        def decision():
            status,body,_=request(gateway+'/v1/authorize',runtime_token,payload,'POST')
            assert status==200,(status,body)
            parsed=json.loads(body);return (parsed['policyVersion'],parsed['decision'])
        def publish(sequence,lifetime=60000,rollback=None,grace=0,read_grace=False):
            snapshot=json.loads(request(api+'/config/export',admin)[1])
            doc={'directoryRevision':snapshot['revision'],'expectedSequence':sequence,'lifetimeMs':lifetime,'graceMs':grace}
            if rollback is not None:doc['rollbackOf']=rollback
            if read_grace:doc['gracePolicyIds']=['read-policy']
            endpoint='/bundles/rollback' if rollback is not None else '/bundles/publish'
            return request(api+endpoint,admin,doc,'POST',{'Idempotency-Key':'publish-'+str(sequence)})
        try:
            control=start_control();gw=start_gateway()
            until(lambda:request(management+'/v1/health/live')[0],200,'Gateway startup')
            assert request(management+'/v1/health/ready')[0]==503
            assert decision()[1]=='BLOCK'
            user={'id':'alice','name':'Alice','enabled':True,'revision':1}
            tool={'id':'files.read','name':'Files','enabled':True,'revision':1,'definition':{'id':'files.read','name':'Files','description':'Test files capability',
                  'actions':[{'name':'read','resourceKinds':['FILE']}],'inputSchema':{},'outputSchema':{}}}
            policy={'id':'read-policy','name':'Read policy','enabled':True,'revision':1,'toolId':'files.read','action':'read',
                    'resource':{'kind':'FILE','locator':'workspace/readme.txt'},'decision':'ALLOW','userIds':['alice'],'teamIds':[],'agentIds':[],'deviceIds':[]}
            for kind,record in [('users',user),('tools',tool),('policies',policy)]:
                assert request(api+'/'+kind,admin,record,'POST',{'Idempotency-Key':'seed-'+kind})[0]==201
            req={'directoryRevision':3,'expectedSequence':0,'lifetimeMs':60000,'graceMs':0}
            for invalid in (None,token(identity,exp=int(time.time())-1),token(new)):
                assert request(api+'/bundles/publish',invalid,req,'POST',{'Idempotency-Key':'denied'})[0]==401
            assert request(api+'/bundles/publish',reader,req,'POST',{'Idempotency-Key':'reader'})[0]==403
            assert request(api+'/users',gateway_identity)[0]==403
            assert request(api+'/bundles/publish',admin,{**req,'bypass':True},'POST',{'Idempotency-Key':'unknown'})[0]==400
            assert request(api+'/bundles/publish',admin,req,'POST')[0]==400
            status,first,headers=publish(0);assert status==201,(status,first);assert headers['ETag']=='"1"'
            assert publish(0)[:2]==(status,first)
            assert request(api+'/bundles/publish',admin,req,'POST',{'Idempotency-Key':'stale'})[0]==409
            assert request(api+'/bundles/current',gateway_identity)[0]==200
            assert request(api+'/bundles/current',token(identity,tenant_id='other'))[0]==404
            parts=json.loads(first)['jws'].split('.')
            old.public_key().verify(base64.urlsafe_b64decode(parts[2]+'=='),(parts[0]+'.'+parts[1]).encode(),padding.PKCS1v15(),hashes.SHA256())
            claims=json.loads(base64.urlsafe_b64decode(parts[1]+'=='));raw=base64.urlsafe_b64decode(claims['policy']+'==')
            assert hashlib.sha256(raw).hexdigest()==claims['policySha256']
            until(decision,('1.0.1','ALLOW'),'adopt ALLOW')
            policy['decision']='BLOCK'
            assert request(api+'/policies/read-policy',admin,policy,'PUT',{'If-Match':'"1"','Idempotency-Key':'block-policy'})[0]==200
            assert publish(1)[0]==201
            until(decision,('1.0.2','BLOCK'),'atomic deny publication')
            assert publish(2,rollback=1)[0]==201
            until(decision,('1.0.3','ALLOW'),'forward rollback')
            assert request(api+'/bundles/versions/1',admin)[1]==first
            # Rotate Control while Gateway already trusts both public keys.
            run(['docker','stop','--time','30',control],capture_output=True)
            env.update(TOOLGATE_CONTROL_BUNDLE_KEY_ID='bundle-new',TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH='/config/bundle-new.pem')
            control=start_control()
            assert publish(3,rollback=1)[0]==201
            until(decision,('1.0.4','ALLOW'),'new key rotation')
            # Revoke the fetch identity: valid cached authorization remains available.
            access.write_text('invalid-fetch-identity',encoding='utf-8')
            until(lambda:'toolgate_bundle_fetch_failed_total 0\n' not in request(management+'/v1/metrics')[1].decode(),True,'fetch rejection')
            assert decision()==('1.0.4','ALLOW')
            access.write_text(token(identity,groups=['toolgate-bundle-reader']),encoding='utf-8')
            # Restore ALLOW configuration; explicitly classify only this read for grace.
            policy.update(decision='ALLOW',revision=2)
            assert request(api+'/policies/read-policy',admin,policy,'PUT',{'If-Match':'"2"','Idempotency-Key':'allow-policy'})[0]==200
            assert publish(4,lifetime=6000,grace=5000,read_grace=True)[0]==201
            until(decision,('1.0.5','ALLOW'),'short lived bundle')
            run(['docker','pause',control],capture_output=True)
            assert decision()==('1.0.5','ALLOW')
            until(lambda:'toolgate_bundle_state 2\n' in request(management+'/v1/metrics')[1].decode(),True,'signed grace',timeout=10)
            assert decision()==('1.0.5','ALLOW')
            until(lambda:request(management+'/v1/health/ready')[0],503,'grace deadline',timeout=20)
            assert decision()==('1.0.5','BLOCK')
            assert request(management+'/v1/health/live')[0]==200
            # Restart cannot revive expired LKG; fresh publication recovers both replicas.
            run(['docker','stop','--time','20',gw],capture_output=True);gw=start_gateway()
            until(lambda:request(management+'/v1/health/live')[0],200,'restart')
            assert request(management+'/v1/health/ready')[0]==503
            run(['docker','unpause',control],capture_output=True);assert publish(5)[0]==201
            until(decision,('1.0.6','ALLOW'),'fresh recovery')
            audit=json.loads(request(api+'/audit',admin)[1])
            assert len([item for item in audit['items'] if item['operation'].startswith('BUNDLE_')])==6
            for container in containers:
                logs=run(['docker','logs',container],capture_output=True,text=True)
                combined=logs.stdout+logs.stderr
                for sensitive in (admin,reader,gateway_identity,runtime_token,db['CONTROL_TEST_PASSWORD'],'secret-redaction-sentinel'):
                    assert sensitive not in combined,'Credential/argument logging'
            report={'realPostgres':True,'productionImages':True,'rsaCrossLanguage':True,'hash':True,'publishReplayRolesTenant':True,
                    'atomicUpdate':True,'forwardRollback':True,'keyRotation':True,'fetchCredentialRefresh':True,'controlUnavailable':True,
                    'readGrace':True,'expiryBlocks':True,'restartFailClosed':True,'freshRecovery':True,'durableAudit':True,'logsRedacted':True,
                    'controlImage':args.control_image,'gatewayImage':args.gateway_image,'controlImageId':control_id,'gatewayImageId':gateway_id}
            (output/'e2e.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
            print('Policy E2E: real PostgreSQL/Control/Gateway publication, signature/hash, swap, rollback, rotation, outage, grace, expiry and recovery passed')
        finally:
            for container in containers:
                subprocess.run(['docker','rm','-f',container],cwd=ROOT,capture_output=True,check=True)


if __name__=='__main__': main()
