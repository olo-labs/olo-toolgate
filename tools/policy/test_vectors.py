# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Explicitly regenerate public-only signed test vectors; private keys never persist.

Not part of normal generation: review changes to this frozen cryptographic corpus.
Every test runs against the same committed signatures and injected wall clock.
"""
import base64
import copy
import hashlib
import json
from pathlib import Path
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import rsa, padding

ROOT = Path(__file__).resolve().parents[2]


def b64(value): return base64.urlsafe_b64encode(value).decode().rstrip('=')
def wire(value): return json.dumps(value, sort_keys=True, separators=(',', ':')).encode()


def main():
    keys = [rsa.generate_private_key(public_exponent=65537, key_size=2048) for _ in range(2)]
    keyring = {'keys': []}
    for index, key in enumerate(keys):
        numbers = key.public_key().public_numbers()
        keyring['keys'].append({'keyId': f'bundle-key-{index+1}', 'modulus': b64(numbers.n.to_bytes(256, 'big')), 'exponent': 'AQAB'})
    fixtures = json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
    payload = copy.deepcopy(fixtures['BundlePayload']); payload.update(expiresAtUnixMs=payload['issuedAtUnixMs']+1000, graceMs=100)
    compiled = copy.deepcopy(fixtures['CompiledPolicy'])
    header = fixtures['BundleHeader']
    vectors = {}
    def signed(name, claims=None, policy=None, protected=None, key=0):
        p = {**payload, **(claims or {})}
        if policy is not None:
            raw = wire(policy); p['policy'] = b64(raw); p['policySha256'] = hashlib.sha256(raw).hexdigest()
        h = {**header, **(protected or {})}
        message = b64(wire(h))+'.'+b64(wire(p))
        vectors[name] = {'jws': message+'.'+b64(keys[key].sign(message.encode(), padding.PKCS1v15(), hashes.SHA256()))}
    signed('allow', policy=compiled)
    strict = copy.deepcopy(compiled); strict['rules'][0]['graceAllowed'] = False; signed('strict', policy=strict)
    blocked = copy.deepcopy(compiled); blocked['rules'][0]['effect'] = 'BLOCK'
    signed('block', {'sequence':2,'version':'1.0.2'}, blocked)
    signed('rollback', {'sequence':3,'version':'1.0.3','rollbackOf':1}, compiled)
    signed('equivocation', policy=blocked)
    signed('rotated', {'sequence':4,'version':'1.0.4'}, compiled, {'kid':'bundle-key-2'}, key=1)
    for field,value in [('issuer','wrong'),('audience','wrong'),('tenantId','wrong'),('version','2.0.0'),('formatVersion',2),('policySha256','0'*64),('extra','unsupported')]:
        signed('bad-'+field, {field:value})
    signed('future', {'issuedAtUnixMs':payload['issuedAtUnixMs']+1})
    signed('expired', {'expiresAtUnixMs':payload['issuedAtUnixMs']})
    signed('bad-lifetime', {'expiresAtUnixMs':payload['issuedAtUnixMs']+86400001})
    signed('bad-grace', {'graceMs':300001})
    signed('bad-rollback', {'rollbackOf':1})
    signed('bad-alg', protected={'alg':'none'})
    signed('bad-key-url', protected={'jku':'https://attacker.invalid/keys'})
    signed('bad-typ', protected={'typ':'JWT'})
    bad = copy.deepcopy(compiled); bad['rules'][0]['effect']='ASK'; signed('ask', policy=bad)
    bad = copy.deepcopy(compiled); bad['rules'].append(bad['rules'][0]); signed('duplicate-rule', policy=bad)
    bad = copy.deepcopy(compiled); bad['formatVersion']=2; signed('bad-policy-version', policy=bad)
    bad = copy.deepcopy(compiled); bad['extra']=True; signed('bad-policy-field', policy=bad)
    # Both effects match: BLOCK precedence must survive declaration order.
    both = copy.deepcopy(compiled); block_rule=copy.deepcopy(blocked['rules'][0]); block_rule['policyId']='explicit-block'; both['rules'].append(block_rule)
    signed('precedence', policy=both)
    many = {'formatVersion':1, 'rules':[]}
    for index in range(512):
        rule=copy.deepcopy(compiled['rules'][0]);rule['policyId']=f'policy-{index:03}'
        if index==511:rule['effect']='BLOCK'
        many['rules'].append(rule)
    signed('many', policy=many)
    request = copy.deepcopy(fixtures['PolicyInput']); request.update(toolId='files', action='read',resource=compiled['rules'][0]['resource'])
    request['context'].update(tenantId='example',userId='alice')
    output = {'now':payload['issuedAtUnixMs'], 'keyring':keyring, 'input':request, 'bundles':vectors}
    directory=ROOT/'tests/fixtures/policy'; directory.mkdir(parents=True,exist_ok=True)
    (directory/'signed-v1.json').write_text(json.dumps(output,indent=2)+'\n',encoding='utf-8',newline='\n')


if __name__=='__main__': main()
