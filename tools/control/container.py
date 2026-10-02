# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Production image build and signed JWT/PostgreSQL smoke with non-root, read-only runtime."""
import argparse
import json
import tempfile
from pathlib import Path

from check import ROOT, database, environment, http_tests, keypair, ready, request, run


def smoke(image):
    with database() as db, tempfile.TemporaryDirectory(prefix='control-image-',dir=ROOT/'.dev') as temp:
        work = Path(temp); key, public = keypair(work)
        env = environment(db,Path('/etc/toolgate/public.pem'))
        env['QUARKUS_DATASOURCE_JDBC_URL']='jdbc:postgresql://127.0.0.1:5432/control?sslmode=disable'
        env_path = work/'environment'
        env_path.write_text('\n'.join(k+'='+v for k,v in env.items() if k.startswith(('QUARKUS_','MP_JWT_','TOOLGATE_CONTROL_')))+'\n',encoding='utf-8')
        env_path.chmod(0o600)
        container = run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges',
                         '--memory=768m','--cpus=2','--network','container:'+db['container'],'--tmpfs','/tmp:rw,noexec,nosuid,size=64m,uid=65532,gid=65532',
                         '--env-file',str(env_path),'-v',f'{public.as_posix()}:/etc/toolgate/public.pem:ro',image],capture_output=True,text=True).stdout.strip()
        try:
            info = json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
            assert info['Config']['User']=='65532:65532'
            assert info['HostConfig']['ReadonlyRootfs']
            management = 'http://127.0.0.1:'+db['managementPort']; runtime='http://127.0.0.1:'+db['runtimePort']
            ready(management); credential = http_tests(runtime,management,key)
            assert request(runtime+'/api/control/v1/users',credential)[0] == 200
            ready(management)
            run(['docker','stop','--time','30',container],capture_output=True)
            info = json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
            assert info['State']['ExitCode'] in (0,143), info['State']
            logs = run(['docker','logs',container],capture_output=True,text=True)
            combined = logs.stdout+logs.stderr
            assert credential not in combined and db['CONTROL_TEST_PASSWORD'] not in combined and 'secret-redaction-sentinel' not in combined
            assert 'stopped in' in combined
            run(['docker','cp',container+':/usr/share/licenses/olo-toolgate/third-party/gradle.lockfile',str(work/'image.lock')],capture_output=True)
            assert (work/'image.lock').read_bytes()==(ROOT/'apps/control-plane/gradle.lockfile').read_bytes()
            image_info = json.loads(run(['docker','image','inspect',image],capture_output=True,text=True).stdout)[0]
            output = ROOT/'build/control'; output.mkdir(parents=True,exist_ok=True)
            report = {'image':image,'imageId':image_info['Id'],'imageBytes':image_info['Size'],'nonRoot':True,'readOnlyRoot':True,
                      'realPostgres':True,'jwtRolesIsolationReplay':True,'jsonYamlAudit':True,'metrics':True,'logsRedacted':True,
                      'sigtermExitCode':info['State']['ExitCode'],'shutdownCompleted':True,'thirdPartyNotices':True}
            (output/'container-smoke.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
            print('Control production container smoke and graceful shutdown passed')
        finally: run(['docker','rm','-f',container],capture_output=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--image',default='olo-toolgate-control:module04');parser.add_argument('--no-build',action='store_true');args=parser.parse_args()
    if not args.no_build:
        version=(ROOT/'VERSION').read_text().strip();revision=run(['git','rev-parse','HEAD'],capture_output=True,text=True).stdout.strip()
        run(['docker','build','-f','apps/control-plane/Dockerfile','--build-arg','VERSION='+version,'--build-arg','REVISION='+revision,'-t',args.image,'.'])
    smoke(args.image)


if __name__=='__main__': main()
