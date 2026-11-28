# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Execute sandbox target storage checks against a fresh owned PostgreSQL container."""
import json
import os
from pathlib import Path
import secrets
import subprocess
import time

ROOT=Path(__file__).resolve().parents[2]


def main():
    name='olo-sandbox-test-'+secrets.token_hex(6)
    password=secrets.token_hex(24)
    environment=dict(os.environ,POSTGRES_PASSWORD=password)
    container=None
    try:
        container=subprocess.check_output(['docker','run','--rm','-d','--name',name,
            '--label','io.olo.qualification.owner=olo-business-sandbox','-p','127.0.0.1::5432',
            '-e','POSTGRES_PASSWORD','postgres:16'],env=environment,text=True).strip()
        end=time.monotonic()+60
        while time.monotonic()<end:
            if subprocess.run(['docker','exec',container,'pg_isready','-h','127.0.0.1','-U','postgres'],capture_output=True).returncode==0:
                break
            time.sleep(.5)
        else:
            raise TimeoutError('Sandbox PostgreSQL readiness deadline exceeded')
        info=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
        port=info['NetworkSettings']['Ports']['5432/tcp'][0]['HostPort']
        test_env=dict(os.environ,SANDBOX_TEST_JDBC='jdbc:postgresql://127.0.0.1:'+port+'/postgres',SANDBOX_TEST_PASSWORD=password)
        wrapper=ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')
        subprocess.run([str(wrapper),'--no-daemon','-p',str(ROOT/'bot/sandbox'),'test','installDist'],env=test_env,cwd=ROOT,check=True)
    finally:
        if container is not None:
            info=json.loads(subprocess.check_output(['docker','inspect',container],text=True))[0]
            if info['Id']!=container or info['Name']!='/'+name or (info['Config'].get('Labels') or {}).get('io.olo.qualification.owner')!='olo-business-sandbox':
                raise RuntimeError('Refusing to remove a sandbox fixture without exact owned identity')
            subprocess.run(['docker','rm','-f','-v',container],check=True,stdout=subprocess.DEVNULL)


if __name__=='__main__':
    main()
