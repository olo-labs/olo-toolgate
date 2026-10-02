# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Maintain the additive Module 04 schema and structural round-trip corpus."""
import base64
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def b64(value):
    return base64.urlsafe_b64encode(value).decode().rstrip('=')


def main():
    identifier = {'$ref': 'identifiers.schema.json#/$defs/Identifier'}
    integer = {'type': 'integer', 'minimum': 0, 'maximum': 9007199254740991}
    def obj(description, properties, optional=()):
        return {'type': 'object', 'description': description, 'additionalProperties': False,
                'properties': properties, 'required': [k for k in properties if k not in optional]}
    def ids():
        return {'type': 'array', 'items': identifier, 'maxItems': 512, 'uniqueItems': True}
    defs = {
        'BundleRule': obj('Exact tenant-scoped compiled policy; empty identity dimensions are unrestricted. BLOCK has precedence.', {
            'policyId': identifier, 'userIds': ids(), 'agentIds': ids(), 'deviceIds': ids(),
            'toolId': identifier, 'action': identifier,
            'resource': {'$ref': 'resource.schema.json#/$defs/ResourceDescriptor'},
            'graceAllowed': {'type': 'boolean', 'description': 'Explicit administrator classification as a low-risk read; never inferred from operation names.'},
            'effect': {'$ref': 'bundle.schema.json#/$defs/BundleEffect'}}),
        'BundleEffect': {'type': 'string', 'enum': ['ALLOW', 'BLOCK']},
        'CompiledPolicy': obj('Version 1 deterministic exact-match rules, with unconditional default deny.', {
            'formatVersion': {'type': 'integer', 'const': 1},
            'rules': {'type': 'array', 'items': {'$ref': 'bundle.schema.json#/$defs/BundleRule'}, 'maxItems': 4096}}),
        'BundleHeader': obj('Strict JWS protected header; no remote or embedded keys and no algorithm negotiation.', {
            'alg': {'type': 'string', 'const': 'RS256'},
            'typ': {'type': 'string', 'const': 'toolgate-policy-bundle+jws'}, 'kid': identifier}),
        'BundlePayload': obj('Signed version, trust domain, immutable policy bytes and bounded freshness claims.', {
            'formatVersion': {'type': 'integer', 'const': 1},
            'issuer': identifier, 'audience': identifier, 'tenantId': identifier,
            'sequence': dict(integer, minimum=1),
            'version': {'$ref': 'identifiers.schema.json#/$defs/SemanticVersion'},
            'directoryRevision': integer,
            'issuedAtUnixMs': integer, 'expiresAtUnixMs': integer,
            'graceMs': {'type': 'integer', 'minimum': 0, 'maximum': 300000},
            'policySha256': {'$ref': 'identifiers.schema.json#/$defs/Sha256'},
            'policy': {'type': 'string', 'pattern': '^[A-Za-z0-9_-]+$', 'minLength': 1, 'maxLength': 1048576},
            'rollbackOf': dict(integer, minimum=1)}, ('rollbackOf',)),
        'SignedPolicyBundle': obj('RFC 7515 compact JWS; payload and hash must both verify before adoption.', {
            'jws': {'type': 'string', 'pattern': '^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$',
                    'minLength': 1, 'maxLength': 1500000}}),
        'BundlePublishRequest': obj('Publish a consistent directory snapshot or roll back into a new sequence. Requires Idempotency-Key.', {
            'directoryRevision': integer, 'expectedSequence': integer,
            'lifetimeMs': {'type': 'integer', 'minimum': 1000, 'maximum': 86400000},
            'graceMs': {'type': 'integer', 'minimum': 0, 'maximum': 300000},
            'gracePolicyIds': {'type': 'array', 'items': identifier, 'maxItems': 512, 'uniqueItems': True},
            'rollbackOf': dict(integer, minimum=1)}, ('rollbackOf', 'gracePolicyIds')),
    }
    schema = {'$schema': 'https://json-schema.org/draft/2020-12/schema',
              '$id': 'https://schemas.ololabs.io/toolgate/v1/bundle.schema.json',
              '$comment': 'Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0',
              'title': 'SignedPolicyBundle', '$ref': '#/$defs/SignedPolicyBundle', '$defs': defs}
    (ROOT/'packages/contracts/schemas/v1/bundle.schema.json').write_text(json.dumps(schema, indent=2)+'\n', encoding='utf-8', newline='\n')
    path = ROOT/'tests/fixtures/contracts/v1/valid.json'
    fixtures = json.loads(path.read_text())
    rule = {'policyId': 'allow-read', 'userIds': ['alice'], 'agentIds': [], 'deviceIds': [],
            'toolId': 'files', 'action': 'read', 'resource': fixtures['ResourceDescriptor'], 'graceAllowed': True, 'effect': 'ALLOW'}
    policy = {'formatVersion': 1, 'rules': [rule]}
    policy_bytes = json.dumps(policy, sort_keys=True, separators=(',', ':')).encode()
    payload = {'formatVersion': 1, 'issuer': 'control', 'audience': 'gateway', 'tenantId': 'example',
               'sequence': 1, 'version': '1.0.1', 'directoryRevision': 0, 'issuedAtUnixMs': 1700000000000,
               'expiresAtUnixMs': 1700000060000, 'graceMs': 0, 'policySha256': hashlib.sha256(policy_bytes).hexdigest(),
               'policy': b64(policy_bytes)}
    header = {'alg': 'RS256', 'typ': 'toolgate-policy-bundle+jws', 'kid': 'bundle-key-1'}
    fixtures.update({'BundleRule': rule, 'BundleEffect': 'ALLOW', 'CompiledPolicy': policy,
                     'BundleHeader': header, 'BundlePayload': payload,
                     # Structural fixture only; security tests use genuine signatures.
                     'SignedPolicyBundle': {'jws': b64(json.dumps(header).encode())+'.'+b64(json.dumps(payload).encode())+'.c3RydWN0dXJhbC1vbmx5'},
                     'BundlePublishRequest': {'directoryRevision': 0, 'expectedSequence': 0, 'lifetimeMs': 60000, 'graceMs': 0}})
    path.write_text(json.dumps(fixtures, indent=2)+'\n', encoding='utf-8', newline='\n')


if __name__ == '__main__':
    main()
