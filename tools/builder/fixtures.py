# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Regenerate public cross-language lease vectors; ephemeral private keys are never written."""
import base64
import copy
import json
from datetime import datetime,timezone,timedelta
from pathlib import Path
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa,padding
ROOT=Path(__file__).resolve().parents[2]
def main():
    def b64(raw):return base64.urlsafe_b64encode(raw).rstrip(b'=').decode()
    keys={name:rsa.generate_private_key(public_exponent=65537,key_size=2048) for name in ['organization','release','device-ca']}
    def public(name):
        number=keys[name].public_key().public_numbers().n
        return {'kid':name,'n':b64(number.to_bytes((number.bit_length()+7)//8,'big')),'e':'AQAB'}
    fixture=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
    task=copy.deepcopy(fixture['BuilderTestTask']);task['expiresAtUnixMs']=1700000060000;task['job']['expiresAtUnixMs']=task['expiresAtUnixMs']
    task['definition']['platforms']=['LINUX','WINDOWS','MACOS'];task['definition']['architectures']=['x86_64','aarch64']
    def sign(value,name='organization'):
        header=b64(json.dumps({'alg':'RS256','typ':'toolgate-builder-test+jws','kid':name},separators=(',',':')).encode());message=header+'.'+b64(json.dumps(value,separators=(',',':')).encode())
        return {'jws':message+'.'+b64(keys[name].sign(message.encode(),padding.PKCS1v15(),hashes.SHA256()))}
    name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'public-builder-fixture-ca')]);start=datetime(2023,1,1,tzinfo=timezone.utc)
    ca=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(keys['device-ca'].public_key()).serial_number(1).not_valid_before(start).not_valid_after(start+timedelta(days=3650)).add_extension(x509.BasicConstraints(ca=True,path_length=0),True).sign(keys['device-ca'],hashes.SHA256())
    output={'organizationKeys':[public('organization')],'releaseKeys':[public('release')],'issuerCertificatePem':ca.public_bytes(serialization.Encoding.PEM).decode(),'task':task,'valid':sign(task),'wrongAuthority':sign(task,'release')}
    for label in ['permission','credential','queued','resource']:
        value=copy.deepcopy(task)
        if label=='permission':value['definition']['permissions']=['FILE_WRITE']
        if label=='credential':value['definition']['credentialRequirements']=['secret://fixture/provider']
        if label=='queued':value['job']['state']='QUEUED'
        if label=='resource':value['definition']['resource']['locator']='runtime/other-tool'
        output[label]=sign(value)
    path=ROOT/'tests/fixtures/builder/v1/signed.json';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8')
if __name__=='__main__':main()
