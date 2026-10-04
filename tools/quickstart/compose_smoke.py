# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Own isolated Compose PG/Redis dependencies and test the source-built Quickstart."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import uuid

ROOT=Path(__file__).resolve().parents[2]

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image',default='olo-toolgate-quickstart:module11')
    parser.add_argument('--control-image',default='olo-toolgate-control:module11')
    parser.add_argument('--browser',action='store_true')
    args=parser.parse_args()
    project='toolgate-options-'+uuid.uuid4().hex[:10]
    (ROOT/'.dev').mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='compose-options-',dir=ROOT/'.dev') as temporary:
        folder=Path(temporary);(folder/'env').mkdir()
        for name in ('compose.yaml','setup.py','nginx.conf'):
            shutil.copyfile(ROOT/'deploy/compose/GatewayControl'/name,folder/name)
        # The test reuses Quickstart's exact Gateway binary. Its composition
        # healthcheck is inapplicable to this standalone binary; use its real readiness endpoint.
        (folder/'image-test.yaml').write_text('''services:
  gateway:
    healthcheck:
      test: [CMD, /opt/quickstart-python/bin/python, -c, "import urllib.request; urllib.request.urlopen('http://127.0.0.1:9091/v1/health/ready', timeout=2)"]
      interval: 2s
      timeout: 3s
      retries: 60
''',encoding='utf-8')
        environment=dict(os.environ,COMPOSE_PROJECT_NAME=project,CONTROL_IMAGE=args.control_image,
                         GATEWAY_IMAGE=args.image,HELPER_IMAGE=args.image,STACK_TLS_PORT='0')
        command=['docker','compose','-f',str(folder/'compose.yaml'),'-f',str(folder/'image-test.yaml'),'--profile','database','--profile','proxy']
        def run(tail):subprocess.run(command+tail,env=environment,check=True)
        try:
            run(['run','--rm','--no-deps','setup','init'])
            owner=[] if os.name=='nt' else ['-e','HOST_UID='+str(os.getuid()),'-e','HOST_GID='+str(os.getgid())]
            run(['run','--rm','--no-deps',*owner,'setup','export-db'])
            run(['up','-d','--wait','--wait-timeout','180'])
            run(['run','--rm','--no-deps','probe'])
            subprocess.run([sys.executable,str(ROOT/'tools/quickstart/options_smoke.py'),'--image',args.image,
                '--database-env-file',str(folder/'env/.env.db'),'--cache-env-file',str(folder/'env/.env.cache'),
                '--network',project+'_backend','--public-network',project+'_frontend',*(['--browser'] if args.browser else [])],check=True)
        finally:
            subprocess.run(command+['down','--volumes'],env=environment,check=False)

if __name__=='__main__':main()
