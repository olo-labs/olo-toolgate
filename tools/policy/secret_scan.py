# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Prove the public-JWS fixture allowance cannot hide credential/private-key fields."""
import json
import subprocess
import tempfile
from pathlib import Path
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa

ROOT=Path(__file__).resolve().parents[2]


def checks():
    public=json.loads((ROOT/'tests/fixtures/policy/signed-v1.json').read_text())['bundles']['allow']['jws']
    with tempfile.TemporaryDirectory(prefix='fixture-secret-proof-',dir=ROOT/'.dev') as temporary:
        work=Path(temporary);(work/'.gitleaks.toml').write_bytes((ROOT/'.gitleaks.toml').read_bytes())
        fixture=work/'tests/fixtures/policy/signed-v1.json';fixture.parent.mkdir(parents=True)
        fixture.write_text(json.dumps({'jws':public},indent=2)+'\n',encoding='utf-8')
        def scan(expected):
            result=subprocess.run(['docker','run','--rm','-v',f'{work.as_posix()}:/repo:ro','zricethezav/gitleaks:v8.24.2',
                'detect','--source=/repo','--no-git','--redact','--exit-code=1'],cwd=ROOT,capture_output=True,text=True)
            assert result.returncode==expected,'Secret-scan fixture allowance failed its positive/negative proof'
        scan(0)
        # Same token-shaped bytes outside the exact public-artifact field must flag.
        fixture.write_text(json.dumps({'accessToken':public},indent=2)+'\n',encoding='utf-8')
        scan(1)
        private=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        fixture.write_bytes(private.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
        scan(1)
    print('Secret scan: reviewed public JWS field allowed; credentials and private keys still rejected')


if __name__=='__main__':checks()
