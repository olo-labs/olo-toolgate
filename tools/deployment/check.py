# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real signed fleet E2E: owns only a nonce-named image and the integration harness's disposable infrastructure."""
import argparse
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from client.runtime_check import PYTHON

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--control-image',default='olo-toolgate-control:module09')
    parser.add_argument('--gateway-image',default='olo-toolgate-gateway:module07')
    parser.add_argument('--binary',type=Path,default=ROOT/'target/release/olo-toolgate-client')
    parser.add_argument('--docker-cli',type=Path)
    parser.add_argument('--builder',action='store_true',help='Include real designated-client authoring tests and deployment')
    args=parser.parse_args()
    cli=args.docker_cli or (ROOT/'.dev/bin/docker-linux' if os.name=='nt' else Path(shutil.which('docker') or 'missing'))
    if not cli.is_file() or not args.binary.is_file():raise ValueError('Real Linux client and Docker CLI required')
    tag='olo-toolgate-fleet-fixture:'+secrets.token_hex(12)
    try:
        (ROOT/'.dev').mkdir(parents=True, exist_ok=True)
        with tempfile.TemporaryDirectory(prefix='fleet-image-',dir=ROOT/'.dev') as temporary:
            work=Path(temporary);shutil.copyfile(ROOT/'tools/client/runtime-fixtures/python.py',work/'python.py')
            (work/'Dockerfile').write_text('# Copyright 2026 OLO Labs\n# SPDX-License-Identifier: Apache-2.0\nFROM '+PYTHON+'\nCOPY --chmod=0444 python.py /opt/tool/tool.py\nRUN chmod 0755 /opt /opt/tool\n',encoding='utf-8')
            subprocess.run(['docker','build','-t',tag,str(work)],check=True,cwd=ROOT)
            image=subprocess.check_output(['docker','image','inspect','--format','{{.Id}}',tag],text=True).strip()
            version=subprocess.check_output(['docker','run','--rm','--network','none','--entrypoint','/usr/local/bin/python3',image,'--version'],text=True).strip()
            match=re.fullmatch(r'Python (\d+\.\d+\.\d+)',version)
            if not match:raise ValueError('Exact runtime version required')
            subprocess.run([sys.executable,str(ROOT/'tools/client/integration.py'),'--fleet',*(['--builder'] if args.builder else []),'--control-image',args.control_image,'--gateway-image',args.gateway_image,'--binary',str(args.binary.resolve()),'--runtime-image',image,'--runtime-version',match[1],'--docker-cli',str(cli.resolve())],check=True,cwd=ROOT)
    finally:
        subprocess.run(['docker','image','rm',tag],capture_output=True,cwd=ROOT)
if __name__=='__main__':main()
