# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Sign an exact sealed descriptor outside Control using an independent release authority."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding

def sign_release(raw, private_key, kid):
    if not 1 <= len(raw) <= 32768 or not isinstance(private_key,rsa.RSAPrivateKey) or private_key.key_size < 2048:
        raise ValueError('Bounded descriptor and RSA release authority required')
    if not kid or len(kid)>128 or any(c not in 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._:-/' for c in kid):
        raise ValueError('Invalid release key ID')
    def unique(pairs):
        result={}
        for name,value in pairs:
            if name in result:raise ValueError('Duplicate descriptor property')
            result[name]=value
        return result
    def finite(_value):raise ValueError('Non-finite descriptor value')
    document=json.loads(raw,object_pairs_hook=unique,parse_constant=finite)
    from referencing import Registry, Resource
    from jsonschema import Draft202012Validator
    root=Path(__file__).resolve().parents[2]/'packages/contracts/schemas/v1'
    schemas=[json.loads(path.read_text(encoding='utf-8')) for path in root.glob('*.json')]
    registry=Registry().with_resources((schema['$id'],Resource.from_contents(schema)) for schema in schemas)
    schema=next(s for s in schemas if s['$id'].endswith('/fleet.schema.json'))
    Draft202012Validator({'$ref':schema['$id']+'#/$defs/FleetPackageDocument'},registry=registry).validate(document)
    def b64(data):return base64.urlsafe_b64encode(data).rstrip(b'=').decode()
    header=b64(json.dumps({'alg':'RS256','typ':'toolgate-package-release+jws','kid':kid},separators=(',',':')).encode())
    message=header+'.'+b64(raw)
    return {'packageId':document['packageId'],'version':document['version'],'manifestDigest':hashlib.sha256(raw).hexdigest(),'sizeBytes':len(raw),'release':{'jws':message+'.'+b64(private_key.sign(message.encode(),padding.PKCS1v15(),hashes.SHA256()))}}

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--descriptor',type=Path,required=True);parser.add_argument('--key',type=Path,required=True)
    parser.add_argument('--kid',required=True);parser.add_argument('--output',type=Path,required=True)
    args=parser.parse_args()
    if args.descriptor.is_symlink() or args.descriptor.stat().st_size>32768 or args.key.is_symlink() or args.key.stat().st_size>16384:
        raise ValueError('Regular bounded input files required')
    if args.output.resolve() in (args.descriptor.resolve(),args.key.resolve()):raise ValueError('Output must differ from inputs')
    key=serialization.load_pem_private_key(args.key.read_bytes(),password=None)
    envelope=sign_release(args.descriptor.read_bytes(),key,args.kid)
    # No author program is loaded; only the descriptor bytes are signed. Never print private material.
    with args.output.open('x',encoding='utf-8') as stream:stream.write(json.dumps(envelope,indent=2)+'\n')
    print('Release envelope created; mirror the exact descriptor bytes under its manifestDigest.json name.')
if __name__=='__main__':main()
