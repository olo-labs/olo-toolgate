# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Isolated Kind install/authorization/upgrade/rollback smoke; no existing cluster."""
import argparse
import json
import os
import shutil
import subprocess
import tempfile
import time
import urllib.error
import uuid
from pathlib import Path

import yaml

from container import request
from local_credentials import prepare

ROOT=Path(__file__).resolve().parents[2]


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image',default='olo-toolgate-gateway:module05')
    args=parser.parse_args()
    names={k:os.environ.get('TOOLGATE_'+k.upper()+'_PATH') or shutil.which(k) for k in ('kind','kubectl','helm')}
    if any(not p for p in names.values()): raise SystemExit('Kind, kubectl and native Helm are required; see gateway deployment docs')
    cluster='gateway-module01-'+uuid.uuid4().hex[:8]
    image_id=subprocess.check_output(['docker','image','inspect','--format','{{.Id}}',args.image],text=True).strip()
    folder=ROOT/'.dev'/cluster; folder.mkdir(parents=True)
    config=folder/'kubeconfig'; env=dict(os.environ,KUBECONFIG=str(config))
    def run(tool,*rest,capture=False):
        print('+ '+tool+' '+' '.join(rest[:3]),flush=True)
        return subprocess.run([names[tool],*rest],cwd=ROOT,env=env,check=True,text=True,capture_output=capture)
    forward=None
    try:
        run('kind','create','cluster','--name',cluster,'--image','kindest/node:v1.32.2@sha256:142f543559cc55d64e1ab9341df08e5ced84bd2e893736da8f51320f26f5950b','--wait','120s','--kubeconfig',str(config))
        run('kind','load','docker-image',args.image,'--name',cluster)
        token,_=prepare(folder)
        # Secret creation reads a file; no credential material enters command args.
        run('kubectl','create','secret','generic','gateway-runtime','--from-file=credentials.json='+str(folder/'credentials.json'))
        cfg=json.loads((ROOT/'docs/examples/gateway-static.json').read_text())
        cfg.update(listen='0.0.0.0:8081',managementListen='0.0.0.0:9091',trustedTlsProxy=True)
        registry,tag=args.image.rsplit(':',1); namespace,repo=(registry.rsplit('/',1) if '/' in registry else ('docker.io/library',registry))
        values={'global':{'imageRegistry':namespace},'gateway':{'enabled':True,'credentialsSecret':'gateway-runtime','image':{'repository':repo,'tag':tag,'pullPolicy':'Never'},'config':cfg,'networkPolicy':{'runtimeFrom':[{'podSelector':{}}]}}}
        path=folder/'values.yaml'; path.write_text(yaml.safe_dump(values),encoding='utf-8')
        chart='deploy/helm/olo-toolgate'; release='gateway-smoke'; deployment='gateway-smoke-olo-toolgate-gateway'
        run('helm','upgrade','--install',release,chart,'-f',str(path),'--wait','--timeout','120s')
        run('kubectl','rollout','status','deployment/'+deployment,'--timeout=120s')
        def probe(expected):
            nonlocal forward
            if forward is not None:
                forward.terminate(); forward.wait(timeout=10)
            # Reopen after each rollout, selecting a current ready pod explicitly.
            pods=json.loads(run('kubectl','get','pods','-l','app.kubernetes.io/instance='+release,'-o','json',capture=True).stdout)
            active=[p for p in pods['items'] if not p['metadata'].get('deletionTimestamp')]
            assert len(active)==2
            assert all(any(c['type']=='Ready' and c['status']=='True' for c in p['status'].get('conditions',[])) for p in active)
            pod=active[0]['metadata']['name']
            forward=subprocess.Popen([names['kubectl'],'port-forward','pod/'+pod,'18081:8081'],cwd=ROOT,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
            deadline=time.monotonic()+30
            while True:
                try:
                    code,body=request('http://127.0.0.1:18081/v1/authorize',token,{'toolId':'files.read','action':'read','arguments':{'path':'workspace/readme.txt'}})
                    if code==200:
                        assert json.loads(body)['decision']==expected
                        break
                except (urllib.error.URLError,ConnectionError,TimeoutError): pass
                if forward.poll() is not None or time.monotonic()>deadline: raise RuntimeError('port-forward not ready')
                time.sleep(0.1)
            assert request('http://127.0.0.1:18081/v1/authorize',None,{'toolId':'files.read','action':'read','arguments':{}})[0]==401
        probe('ALLOW')
        values['gateway']['config']['policy']['emergencyBlock']=True
        path.write_text(yaml.safe_dump(values),encoding='utf-8')
        run('helm','upgrade',release,chart,'-f',str(path),'--wait','--timeout','120s')
        forward.terminate(); forward.wait(timeout=10); forward=None
        def verify_expected(expected):
            run('kubectl','rollout','status','deployment/'+deployment,'--timeout=120s')
            manifest=run('kubectl','get','configmap',deployment,'-o','json',capture=True)
            config=json.loads(json.loads(manifest.stdout)['data']['gateway.json']); assert config['policy']['emergencyBlock']==expected
            probe('BLOCK' if expected else 'ALLOW')
        verify_expected(True)
        run('helm','rollback',release,'1','--wait','--timeout','120s')
        verify_expected(False)
        output=ROOT/'build/gateway'; output.mkdir(parents=True,exist_ok=True)
        (output/'cluster-smoke.json').write_text(json.dumps({'image':args.image,'imageId':image_id,'kindVersion':'0.27.0','kubernetes':'1.32.2','installReady':True,'authorizationAllow':True,'authFailure':True,'emergencyDenyUpgrade':True,'rollbackReady':True},indent=2)+'\n',encoding='utf-8')
        print('Kind install, auth, emergency-deny upgrade and rollback passed')
    finally:
        if forward is not None: forward.terminate(); forward.wait(timeout=10)
        # Only the unique cluster created above is removed; never use the active
        # kubectl context or an existing cluster name for destructive cleanup.
        subprocess.run([names['kind'],'delete','cluster','--name',cluster],cwd=ROOT,env=env,check=True)


if __name__=='__main__': main()
