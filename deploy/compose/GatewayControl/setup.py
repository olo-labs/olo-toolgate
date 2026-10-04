# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Contained local bootstrap; custody stays in private Docker volumes."""
import base64
import datetime
import hashlib
import ipaddress
import json
import os
from pathlib import Path
import secrets
import sys
import time
import urllib.request
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

ISSUER = 'https://localhost/compose-identity'

def write(path, value, private=False):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(value.encode() if isinstance(value, str) else value)
    path.chmod(0o600 if private else 0o644)
    if str(path).startswith(('/control-config/', '/gateway-config/')):
        os.chown(path, 65532, 65532)

def key():
    path = Path('/custody/identity.pem')
    if not path.exists():
        k = rsa.generate_private_key(public_exponent=65537, key_size=3072)
        write(path, k.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()), True)
    return serialization.load_pem_private_key(path.read_bytes(), None)

def init():
    os.umask(0o077)
    Path('/custody').chmod(0o700)
    for name in ('postgres', 'app', 'migrator', 'runtime', 'redis'):
        p = Path('/custody/'+name)
        if not p.exists(): write(p, secrets.token_hex(32), True)
    secret = lambda name: Path('/custody/'+name).read_text()
    write('/redis-config/redis.conf', 'bind 0.0.0.0\nprotected-mode yes\nrequirepass '+secret('redis')+'\nmaxmemory 128mb\nmaxmemory-policy allkeys-lru\nsave ""\nappendonly no\n')
    write('/redis-config/password', secret('redis'))
    write('/control-config/bundled-cache.env', 'TOOLGATE_CACHE_MODE=redis\nTOOLGATE_REDIS_URL=redis://redis:6379/0\nTOOLGATE_REDIS_PASSWORD='+secret('redis')+'\n', True)
    k = key()
    write('/control-config/identity-public.pem', k.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))
    env = {
        'QUARKUS_DATASOURCE_JDBC_URL': 'jdbc:postgresql://postgres:5432/control?sslmode=disable',
        'QUARKUS_DATASOURCE_USERNAME': 'control_app', 'QUARKUS_DATASOURCE_PASSWORD': secret('app'),
        'QUARKUS_FLYWAY_USERNAME': 'control_migrator', 'QUARKUS_FLYWAY_PASSWORD': secret('migrator'),
        'MP_JWT_VERIFY_PUBLICKEY_LOCATION': '/config/identity-public.pem',
        'MP_JWT_VERIFY_ISSUER': ISSUER, 'MP_JWT_VERIFY_AUDIENCES': 'toolgate-control',
        'TOOLGATE_CONTROL_DEVELOPMENT_MODE': 'true',
    }
    write('/control-config/control.env', ''.join("export "+n+"='"+v+"'\n" for n,v in env.items()), True)
    write('/control-config/bundled.env', ''.join(n+'='+v+'\n' for n,v in env.items() if not n.startswith('MP_JWT_')), True)
    write('/control-config/identity.env', ''.join("export "+n+"='"+v+"'\n" for n,v in env.items() if n.startswith('MP_JWT_')), True)
    write('/db-init/postgres-password', secret('postgres'))
    write('/db-init/01-roles.sql', "CREATE ROLE toolgate_control_runtime NOLOGIN;\nCREATE ROLE control_migrator LOGIN PASSWORD '"+secret('migrator')+"';\nCREATE ROLE control_app LOGIN PASSWORD '"+secret('app')+"' IN ROLE toolgate_control_runtime;\nCREATE DATABASE control OWNER control_migrator;\n")
    expiry = int(time.time()*1000)+30*86400000
    write('/gateway-config/credentials.json', json.dumps([dict(tokenSha256=hashlib.sha256(secret('runtime').encode()).hexdigest(), tenantId='tenant-local', userId='admin-local', agentId='agent-local', deviceId=None, expiresAtUnixMs=expiry)]))
    # Existing administrator policy is retained; defaults never authorize execution.
    config = Path('/gateway-config/gateway.json')
    if not config.exists():
        write(config, json.dumps(dict(listen='0.0.0.0:8081', managementListen='0.0.0.0:9091', trustedTlsProxy=True, allowedOrigins=[], policy=dict(version='0.1.0',expiresAtUnixMs=expiry,emergencyBlock=False,rules=[]), extractors=[dict(toolId='files.read',action='read',pointer='/path',kind='FILE')]),indent=2))
    if not Path('/proxy-tls/server.key').exists():
        tls = rsa.generate_private_key(public_exponent=65537,key_size=3072)
        now = datetime.datetime.now(datetime.timezone.utc)
        name = x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME,'localhost')])
        cert = x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(tls.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=365)).add_extension(x509.SubjectAlternativeName([x509.DNSName('localhost'),x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]),critical=False).add_extension(x509.BasicConstraints(ca=False,path_length=None),critical=True).sign(tls,hashes.SHA256())
        write('/proxy-tls/server.key', tls.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()),True)
        write('/proxy-tls/server.crt',cert.public_bytes(serialization.Encoding.PEM))
    print('Local custody provisioned; existing database and identity retained.')

def token():
    now = int(time.time())
    def enc(data): return base64.urlsafe_b64encode(json.dumps(data,separators=(',',':')).encode()).rstrip(b'=')
    message=enc(dict(alg='RS256',typ='JWT'))+b'.'+enc(dict(iss=ISSUER,aud='toolgate-control',sub='admin-local',tenant_id='tenant-local',groups=['toolgate-admin'],iat=now,exp=now+600))
    print((message+b'.'+base64.urlsafe_b64encode(key().sign(message,padding.PKCS1v15(),hashes.SHA256())).rstrip(b'=')).decode())

def probe():
    deadline=time.monotonic()+150
    while True:
        try:
            for url in ('http://control:9092/q/health/ready','http://gateway:9091/v1/health/ready'):
                with urllib.request.urlopen(url,timeout=3) as r:
                    if r.status!=200: raise RuntimeError('Not ready')
            if os.environ.get('TOOLGATE_REDIS_URL'):
                import redis
                cache = redis.Redis.from_url(os.environ['TOOLGATE_REDIS_URL'], password=os.environ.get('TOOLGATE_REDIS_PASSWORD') or None,
                    socket_timeout=2, socket_connect_timeout=2)
                try: cache.ping()
                finally: cache.close()
            print('Control and Gateway ready.')
            return
        except Exception:
            if time.monotonic()>deadline: raise RuntimeError('Readiness timed out; inspect compose logs')
            time.sleep(2)

if __name__=='__main__':
    if sys.argv[1]=='export-db':
        write('/host-env/.env.db', Path('/control-config/bundled.env').read_bytes(), True)
        write('/host-env/.env.cache', Path('/control-config/bundled-cache.env').read_bytes(), True)
        if 'HOST_UID' in os.environ:
            os.chown('/host-env/.env.db', int(os.environ['HOST_UID']), int(os.environ['HOST_GID']))
            os.chown('/host-env/.env.cache', int(os.environ['HOST_UID']), int(os.environ['HOST_GID']))
    elif sys.argv[1]=='apply-policy':
        source = Path('/host-env/gateway.json')
        if source.is_symlink() or source.stat().st_size > 1048576:
            raise ValueError('Bounded regular Gateway config required')
        value = json.loads(source.read_bytes())
        if not isinstance(value, dict):
            raise ValueError('Gateway config must be an object')
        write('/gateway-config/gateway.json', json.dumps(value, indent=2))
        print('Administrator configuration saved; restart Gateway to validate.')
    else:
        {'init':init,'token':token,'probe':probe}[sys.argv[1]]()
