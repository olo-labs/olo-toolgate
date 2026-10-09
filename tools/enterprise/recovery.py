# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Prepare, independently sign and assemble a protected group-only recovery review.

Each reviewer runs sign separately with their own protected key. Assemble reads
only public proofs. Control validates the pinned external trust and live revision.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import time
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa


def canonical(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':')).encode('utf-8')


def read(path, limit=2097152):
    path = Path(path).absolute()
    if path.is_symlink() or not path.is_file() or path.stat().st_size > limit:
        raise ValueError('Regular bounded file required')
    def pairs(values):
        result = {}
        for name, value in values:
            if name in result:
                raise ValueError('Duplicate JSON field')
            result[name] = value
        return result
    return json.loads(path.read_text(encoding='utf-8'), object_pairs_hook=pairs)


def write(path, value):
    path = Path(path).absolute()
    # Never replace an operator's existing review or key through this utility.
    with os.fdopen(os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), 'wb') as out:
        out.write(canonical(value) + b'\n')
        out.flush()
        os.fsync(out.fileno())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    prepare = commands.add_parser('prepare')
    prepare.add_argument('--snapshot', required=True)
    prepare.add_argument('--reason-file', required=True)
    prepare.add_argument('--expected-revision', type=int, required=True)
    prepare.add_argument('--output', required=True)
    prepare.add_argument('--duration-seconds', type=int, default=900)
    sign = commands.add_parser('sign')
    sign.add_argument('--authorization', required=True)
    sign.add_argument('--private-key', required=True)
    sign.add_argument('--key-id', required=True)
    sign.add_argument('--output', required=True)
    assemble = commands.add_parser('assemble')
    assemble.add_argument('--authorization', required=True)
    assemble.add_argument('--proofs', nargs='+', required=True)
    assemble.add_argument('--output', required=True)
    args = parser.parse_args()
    if args.command == 'prepare':
        snapshot = read(args.snapshot)
        if not 1 <= args.duration_seconds <= 3600 or snapshot['revision'] != args.expected_revision:
            raise ValueError('Exact snapshot revision and bounded lifetime required')
        reason = Path(args.reason_file)
        if reason.is_symlink() or not reason.is_file() or not 1 <= reason.stat().st_size <= 32768:
            raise ValueError('Bounded review reason required')
        now = int(time.time() * 1000)
        write(args.output, dict(formatVersion=1, tenantId=snapshot['tenantId'], expectedRevision=args.expected_revision,
                               snapshot=snapshot, reasonDigest=hashlib.sha256(reason.read_bytes()).hexdigest(),
                               issuedAtUnixMs=now, expiresAtUnixMs=now + args.duration_seconds * 1000))
    elif args.command == 'sign':
        import re
        if not re.fullmatch(r'[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}', args.key_id):
            raise ValueError('Canonical reviewer key ID required')
        path = Path(args.private_key).absolute()
        if path.is_symlink() or not path.is_file() or path.stat().st_size > 16384:
            raise ValueError('Protected RSA private key required')
        if os.name != 'nt' and (path.stat().st_mode & 0o077 or path.stat().st_uid not in (0, os.geteuid())):
            raise ValueError('Private key ownership or permissions rejected')
        key = serialization.load_pem_private_key(path.read_bytes(), password=None)
        if not isinstance(key, rsa.RSAPrivateKey) or not 2048 <= key.key_size <= 8192:
            raise ValueError('RSA key of at least 2048 bits required')
        message = b'OLO ToolGate recovery v1\n' + canonical(read(args.authorization))
        proof = key.sign(message, padding.PKCS1v15(), hashes.SHA256())
        write(args.output, dict(keyId=args.key_id, signature=base64.urlsafe_b64encode(proof).rstrip(b'=').decode()))
    else:
        proofs = [read(path, 4096) for path in args.proofs]
        authorization = read(args.authorization)
        minimum = 1 if authorization['expectedRevision'] == 0 and authorization['snapshot']['revision'] == 0 else 2
        if not minimum <= len(proofs) <= 4 or len({proof['keyId'] for proof in proofs}) != len(proofs):
            raise ValueError('Two to four distinct independent review proofs required')
        write(args.output, dict(authorization=authorization, proofs=proofs))
    print('Protected review artifact written; Control still verifies signatures and current authority.')


if __name__ == '__main__':
    main()
