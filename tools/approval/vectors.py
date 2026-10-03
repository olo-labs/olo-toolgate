# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Explicitly regenerate public format-2 compatibility vectors; never persist private keys."""
import base64
import hashlib
import json
from pathlib import Path
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding, rsa

ROOT=Path(__file__).resolve().parents[2]


def main():
    def b64(value):return base64.urlsafe_b64encode(value).decode().rstrip('=')
    def encode(value):return json.dumps(value,sort_keys=True,separators=(',',':')).encode()
    private=rsa.generate_private_key(public_exponent=65537,key_size=2048)
    public=private.public_key().public_numbers()
    keyring={'keys':[{'keyId':'approval-vector-key','modulus':b64(public.n.to_bytes(256,'big')),'exponent':'AQAB'}]}
    now=1700000000000
    rule={'policyId':'ask','userIds':['alice'],'agentIds':['agent-demo'],'deviceIds':[],
          'toolId':'files.read','action':'read','resource':{'kind':'FILE','locator':'workspace/readme.txt'},
          'graceAllowed':False,'effect':'ASK'}
    def bundle(rules):
        policy=encode({'formatVersion':2,'rules':rules})
        payload={'formatVersion':2,'issuer':'control','audience':'gateway','tenantId':'http-tenant','sequence':1,
                 'version':'2.0.1','directoryRevision':1,'issuedAtUnixMs':now,'expiresAtUnixMs':now+60000,'graceMs':0,
                 'policySha256':hashlib.sha256(policy).hexdigest(),'policy':b64(policy)}
        message=b64(encode({'alg':'RS256','typ':'toolgate-policy-bundle+jws','kid':'approval-vector-key'}))+'.'+b64(encode(payload))
        return {'jws':message+'.'+b64(private.sign(message.encode(),padding.PKCS1v15(),hashes.SHA256()))}
    many=[{**rule,'policyId':f'earlier-{i}','toolId':f'other-{i}','effect':'ALLOW'} for i in range(511)]+[rule]
    corpus={'clock':{'nowUnixMs':now},'keyring':keyring,'bundles':{'ask':bundle([rule]),'many':bundle(many),
        'precedence':bundle([{**rule,'policyId':'allow','effect':'ALLOW'},rule,{**rule,'policyId':'block','effect':'BLOCK'}])}}
    path=ROOT/'tests/fixtures/approval/signed-v2.json';path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(corpus,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Frozen public format-2 ASK/signature/precedence/full-scan vectors written; private key discarded')


if __name__=='__main__':main()
