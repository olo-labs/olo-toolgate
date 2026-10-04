# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Isolated Quickstart PostgreSQL/Redis option smoke against configured dev dependencies."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time
import urllib.error
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parents[2]

def run(args):return subprocess.run(args,check=True,capture_output=True,text=True).stdout.strip()

def request(origin,path,token=None,body=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(origin+path,headers=headers,data=json.dumps(body).encode() if body is not None else None)
    try:
        with urllib.request.urlopen(req,timeout=15) as response:return response.status,json.load(response)
    except urllib.error.HTTPError as error:return error.code,{}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image',required=True)
    parser.add_argument('--database-env-file',required=True)
    parser.add_argument('--cache-env-file',required=True)
    parser.add_argument('--network',required=True)
    parser.add_argument('--public-network',help='Bridge network when the dependency network is internal-only')
    parser.add_argument('--browser',action='store_true')
    args=parser.parse_args()
    name='toolgate-options-'+uuid.uuid4().hex[:12];volume=name+'-data'
    try:
        run(['docker','create','--name',name,'--network',args.public_network or args.network,'--read-only',
             '--cap-drop=ALL','--security-opt=no-new-privileges','--memory=1g',
             '--tmpfs','/tmp:rw,noexec,nosuid,size=128m,uid=65532,gid=65532',
             '-p','127.0.0.1::8080','-v',volume+':/data',
             '--env-file',args.database_env_file,'--env-file',args.cache_env_file,
             '-e','TOOLGATE_QUICKSTART_DATABASE_MODE=postgresql','-e','TOOLGATE_DISABLE_ADMIN_PASSWORD=true',args.image])
        if args.public_network:run(['docker','network','connect',args.network,name])
        run(['docker','start',name])
        info=json.loads(run(['docker','inspect',name]))[0]
        origin='http://127.0.0.1:'+info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
        def ready():
            deadline=time.monotonic()+100
            while time.monotonic()<deadline:
                try:
                    code,status=request(origin,'/api/quickstart/v1/status')
                    if code==200 and status['ready']:return status
                except (urllib.error.URLError,ConnectionError,TimeoutError):pass
                if not json.loads(run(['docker','inspect',name]))[0]['State']['Running']:
                    raise RuntimeError('Options startup failed; inspect isolated container logs')
                time.sleep(.25)
            raise RuntimeError('Options readiness timeout')
        status=ready();assert status['database']=='postgresql' and status['cache']=='redis' and not status['passwordRequired']
        print('External PostgreSQL/Redis composition ready.',flush=True)
        def token():return request(origin,'/api/quickstart/v1/login',body={})[1]['accessToken']
        session=token()
        assert request(origin,'/api/control/v1/users',session)[0]==200
        assert request(origin,'/api/quickstart/v1/tools')[0]==401
        assert request(origin,'/api/quickstart/v1/tools',session)[0]==200
        code,result=request(origin,'/api/quickstart/v1/invoke',session,{'toolId':'calculator.evaluate','arguments':{'expression':'2+3*4'}})
        assert code==200 and result['result']['value']==14
        assert request(origin,'/api/quickstart/v1/invoke',session,{'toolId':'hotfolder.read_text','arguments':{'path':'other.txt'}})[0]==403
        secret_name='options/'+name
        assert request(origin,'/api/quickstart/v1/vault',session,{'name':secret_name,'value':'isolated-encrypted-test-value'})[0]==201
        run(['docker','restart',name])
        info=json.loads(run(['docker','inspect',name]))[0]
        origin='http://127.0.0.1:'+info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
        ready();session=token()
        assert secret_name in request(origin,'/api/quickstart/v1/vault',session)[1]['names']
        if args.browser:
            command=['npx.cmd' if os.name=='nt' else 'npx','--no-install','playwright','test','--config','apps/admin-ui/playwright.config.ts','passwordless.spec.ts']
            subprocess.run(command,cwd=ROOT,env=dict(os.environ,UI_TEST_ORIGIN=origin,UI_PASSWORDLESS_TEST='1'),check=True)
        print('External PG/Redis, password-free session, protected API, Gateway BLOCK and encrypted vault restart verified.')
    except Exception:
        subprocess.run(['docker','exec',name,'/opt/quickstart-python/bin/python','-c',
            "import urllib.request,urllib.error;\nfor p in ('http://127.0.0.1:8080/api/quickstart/v1/status','http://127.0.0.1:9091/v1/health/ready','http://127.0.0.1:9091/v1/metrics'):\n try: print(urllib.request.urlopen(p).read().decode()[:2000])\n except urllib.error.HTTPError as e: print(e.code,e.read().decode())"],check=False)
        logs=subprocess.run(['docker','logs',name],capture_output=True,text=True)
        output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True)
        (output/'options-failure.log').write_text(logs.stdout+logs.stderr,encoding='utf-8')
        raise
    finally:
        subprocess.run(['docker','rm','-f',name],capture_output=True)
        subprocess.run(['docker','volume','rm',volume],capture_output=True)

if __name__=='__main__':main()
