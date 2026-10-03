# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real public-download container and browser proof against the validated native release bundle."""
import argparse
import hashlib
import json
import os
import subprocess
import sys
import tempfile
from pathlib import Path
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from control.check import ROOT, database, environment, keypair, ready, request, run

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--image',default='olo-toolgate-control:module07-downloads');args=parser.parse_args()
    (ROOT/'.dev').mkdir(parents=True, exist_ok=True)
    with database() as db,tempfile.TemporaryDirectory(prefix='client-downloads-',dir=ROOT/'.dev') as temp:
        work=Path(temp);_,public=keypair(work);env=environment(db,Path('/config/jwt-public.pem'))
        env.update(QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://127.0.0.1:5432/control?sslmode=disable',TOOLGATE_CONTROL_ENDPOINT_ENABLED='false')
        (work/'control.env').write_text('\n'.join(k+'='+v for k,v in env.items() if k.startswith(('QUARKUS_','MP_JWT_','TOOLGATE_CONTROL_')))+'\n')
        container=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--memory=768m','--network','container:'+db['container'],'--tmpfs','/tmp:rw,noexec,nosuid,size=64m,uid=65532,gid=65532','--env-file',str(work/'control.env'),'-v',f'{public.as_posix()}:/config/jwt-public.pem:ro',args.image],capture_output=True,text=True).stdout.strip()
        try:
            ready('http://127.0.0.1:'+db['managementPort']);runtime='http://127.0.0.1:'+db['runtimePort'];url=runtime+'/api/public/v1/clients'
            status,raw,_=request(url);assert status==200;manifest=json.loads(raw)
            assert {a['platform'] for a in manifest['artifacts']}=={'WINDOWS','MACOS','LINUX'}
            for artifact in manifest['artifacts']:
                status,data,headers=request(url+'/'+artifact['filename']);assert status==200
                assert len(data)==artifact['bytes'] and hashlib.sha256(data).hexdigest()==artifact['sha256']
                assert headers['X-Content-Type-Options']=='nosniff'
            assert request(runtime+'/api/control/v1/users')[0]==401
            for name in ['missing.zip','device-key','..%2F..%2Fetc%2Fpasswd']:
                assert request(url+'/'+name)[0] in (400,404)
            browser_env=dict(os.environ,UI_TEST_ORIGIN=runtime,UI_TEST_DOWNLOADS_EXPECTED='true')
            subprocess.run(['npm.cmd' if os.name=='nt' else 'npm','--workspace','@olo-labs/toolgate-admin-ui','run','e2e','--','downloads.spec.ts'],cwd=ROOT,env=browser_env,check=True)
            output=ROOT/'build/client';output.mkdir(parents=True,exist_ok=True)
            (output/'public-downloads-proof.json').write_text(json.dumps({'version':manifest['version'],'platforms':3,'anonymousArchiveChecksums':True,'privateApisStillProtected':True,'filenameAllowlist':True,'realBrowser':True},indent=2)+'\n')
            print('All three real native packages download anonymously; browser/roles/checksums/allowlist passed')
        finally:run(['docker','rm','-f',container],capture_output=True)

if __name__=='__main__':main()
