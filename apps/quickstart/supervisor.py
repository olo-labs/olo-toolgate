# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Single-node composition root. Child failure removes readiness and stops every process.

Only fixed local adapters run here: identity bootstrap, encrypted vault and HTTP
composition. Tool execution stays in the Rust fixed built-in executor; custom
author code stays on designated clients. No credential is emitted in logs.
"""
import argparse
import base64
from contextlib import closing
import datetime
import fcntl
import hashlib
import hmac
import http.client
import http.server
import ipaddress
import json
import os
from pathlib import Path
import re
import secrets
import signal
import sqlite3
import ssl
import subprocess
import threading
import time
import urllib.parse
import urllib.request

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

DATA = Path('/data')
TENANT = 'quickstart'
ISSUER = 'https://quickstart.local'
GROUPS = ('toolgate-admin', 'toolgate-super-admin', 'toolgate-reader', 'toolgate-approver', 'toolgate-enroller')


def password_disabled():
    value = os.environ.get('TOOLGATE_DISABLE_ADMIN_PASSWORD', 'false')
    if value not in ('true', 'false'): raise ValueError('Invalid password mode')
    return value == 'true'


def database_mode():
    mode = os.environ.get('TOOLGATE_QUICKSTART_DATABASE_MODE', 'sqlite')
    if mode not in ('sqlite', 'postgresql'): raise ValueError('Invalid database mode')
    return mode


class Database:
    """Same parameterized vault/audit operations with explicit external PG custody."""
    def __init__(self):
        self.pg = database_mode() == 'postgresql'
        if self.pg:
            import psycopg
            url = os.environ['QUARKUS_DATASOURCE_JDBC_URL']
            if not url.startswith('jdbc:postgresql://'): raise ValueError('PostgreSQL JDBC URL required')
            parsed = urllib.parse.urlsplit(url[5:])
            if parsed.username or parsed.password or parsed.fragment: raise ValueError('Separate DB credentials required')
            options = dict(urllib.parse.parse_qsl(parsed.query, strict_parsing=True))
            if set(options)-{'sslmode','sslrootcert'}: raise ValueError('Unsupported database option')
            if options.get('sslmode') not in ('verify-full','require','disable'): raise ValueError('Explicit database TLS mode required')
            self.connection = psycopg.connect(host=parsed.hostname, port=parsed.port or 5432,
                dbname=parsed.path.lstrip('/'), user=os.environ['QUARKUS_DATASOURCE_USERNAME'],
                password=os.environ['QUARKUS_DATASOURCE_PASSWORD'], connect_timeout=3,
                options='-c statement_timeout=3000', **options)
        else: self.connection = sqlite3.connect(DATA/'state/control.sqlite', timeout=3)
    def __enter__(self): return self
    def __exit__(self, typ, value, trace):
        try:
            if typ: self.connection.rollback()
            else: self.connection.commit()
        finally: self.connection.close()
    def execute(self, sql, values=()):
        if self.pg:
            sql = 'LOCK TABLE quickstart_vault IN SHARE ROW EXCLUSIVE MODE' if sql == 'BEGIN IMMEDIATE' else sql.replace('?', '%s')
        return self.connection.execute(sql, values)
    def commit(self): self.connection.commit()


class CatalogCache:
    """Cache only immutable non-secret tool metadata; never cache security decisions."""
    def __init__(self):
        self.lock = threading.Lock(); self.value = None; self.expiry = 0
        mode = os.environ.get('TOOLGATE_CACHE_MODE', 'embedded')
        if mode not in ('embedded', 'redis'): raise ValueError('Invalid cache mode')
        self.redis = None
        if mode == 'redis':
            import redis
            url = os.environ['TOOLGATE_REDIS_URL']
            if urllib.parse.urlsplit(url).scheme not in ('redis','rediss'): raise ValueError('Redis URL required')
            self.redis = redis.Redis.from_url(url, socket_timeout=2, socket_connect_timeout=2,
                max_connections=8, password=os.environ.get('TOOLGATE_REDIS_PASSWORD') or None)
            self.redis.ping()
        catalog = Path('/opt/quickstart/builtins.json').read_bytes()
        self.document = strict(catalog)
        self.cache_key = 'toolgate:catalog:'+hashlib.sha256(catalog).hexdigest()
    def get(self):
        with self.lock:
            now = time.monotonic()
            if self.value is not None and now < self.expiry: return self.value
            # External cache cannot substitute untrusted tool metadata.
            value = self.document
            if self.redis is not None:
                try:
                    cached = self.redis.get(self.cache_key)
                    if cached and len(cached) <= 65536 and strict(cached) == self.document: value = strict(cached)
                    else: self.redis.set(self.cache_key, json.dumps(value), ex=60)
                except Exception: pass  # Immutable source fallback, never authorization fallback.
            self.value = value; self.expiry = now+60
            return value


def strict(raw):
    def unique(pairs):
        value = {}
        for key, item in pairs:
            if key in value: raise ValueError('Duplicate field')
            value[key] = item
        return value
    return json.loads(raw, object_pairs_hook=unique, parse_constant=lambda _: (_ for _ in ()).throw(ValueError()))


def atomic(path, value):
    """Private files use exclusive temporary creation, fsync and an atomic rename."""
    path = Path(path)
    if path.is_symlink(): raise ValueError('Unsafe state path')
    temp = path.with_name(path.name + '.' + secrets.token_hex(8))
    try:
        with temp.open('xb') as output:
            output.write(value if isinstance(value, bytes) else value.encode())
            output.flush(); os.fsync(output.fileno())
        temp.replace(path)
        descriptor = os.open(path.parent, os.O_RDONLY)
        try: os.fsync(descriptor)
        finally: os.close(descriptor)
    finally: temp.unlink(missing_ok=True)


def password_valid(value):
    return isinstance(value, str) and 16 <= len(value) <= 128 and value.isascii() and all(32 < ord(c) < 127 for c in value) and len(set(value)) >= 8


def password_record(value, changed=False, generation=1):
    if not password_valid(value): raise ValueError('Password must be 16–128 printable ASCII characters with eight distinct characters')
    salt = secrets.token_bytes(32)
    return {'salt': salt.hex(), 'hash': hashlib.pbkdf2_hmac('sha256', value.encode(), salt, 600000).hex(), 'changed': changed, 'generation': generation}


def identity_audit(operation, generation, request_id):
    """Persist credential-change intent before changing custody; never audit verifier bytes."""
    path=DATA/'state/control.sqlite'
    if database_mode() == 'sqlite' and (not path.is_file() or path.is_symlink()): raise ValueError('Identity audit unavailable')
    with Database() as db:
        db.execute('INSERT INTO control_audit(tenant_id,actor_id,operation,target,revision,request_id,request_digest) VALUES(?,?,?,?,?,?,?)',
                   (TENANT,hashlib.sha256((ISSUER+'\nadmin').encode()).hexdigest(),operation,'local-identity',generation,request_id,
                    hashlib.sha256((operation+'\n'+str(generation)).encode()).hexdigest()))
        db.commit()


class LocalIdentity:
    """One composition-owned password writer; durable verifier/generation stay in custody."""
    def __init__(self): self.lock=threading.Lock()
    def authenticate(self, value, replacement=None):
        with self.lock:
            record = strict((DATA/'identity.json').read_bytes())
            candidate = value if isinstance(value, str) and len(value) <= 128 else ''
            digest = hashlib.pbkdf2_hmac('sha256', candidate.encode(), bytes.fromhex(record['salt']), 600000).hex()
            if not hmac.compare_digest(digest, record['hash']): raise PermissionError()
            if not record['changed'] or replacement is not None:
                if not password_valid(replacement) or replacement == value: raise ValueError('A different strong password is required')
                record = password_record(replacement, True, record['generation']+1)
                request_id=secrets.token_hex(16)
                identity_audit('PASSWORD_CHANGE_REQUESTED',record['generation'],request_id)
                atomic(DATA/'identity.json', json.dumps(record))
                (DATA/'bootstrap-password').unlink(missing_ok=True)
                identity_audit('PASSWORD_CHANGED',record['generation'],request_id)
            return jwt(GROUPS, 'admin', generation=record['generation'])


def key(name):
    path = DATA/'keys'/f'{name}.pem'
    if not path.exists():
        private = rsa.generate_private_key(public_exponent=65537, key_size=3072)
        atomic(path, private.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        atomic(path.with_name(name+'-public.pem'), private.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))
    return serialization.load_pem_private_key(path.read_bytes(), password=None)


def jwt(groups, subject='admin', generation=None, directory_bound=True):
    now = int(time.time())
    claims = {'iss': ISSUER, 'aud': 'toolgate-control', 'sub': subject, 'user_id': subject, 'tenant_id': TENANT, 'groups': groups, 'iat': now, 'exp': now+900}
    if not directory_bound: claims.pop('user_id')
    if generation is not None: claims['password_generation'] = generation
    encode = lambda value: base64.urlsafe_b64encode(json.dumps(value, separators=(',', ':')).encode()).rstrip(b'=')
    message = encode({'alg': 'RS256', 'typ': 'JWT'})+b'.'+encode(claims)
    signature = key('identity').sign(message, padding.PKCS1v15(), hashes.SHA256())
    return (message+b'.'+base64.urlsafe_b64encode(signature).rstrip(b'=')).decode()


def session(header):
    """Never accept machine tokens or stale password generations at the public proxy."""
    try:
        if not header.startswith('Bearer ') or len(header)>16384: raise ValueError()
        a, b, c = header[7:].split('.')
        decode = lambda text: base64.urlsafe_b64decode(text+'='*(-len(text)%4))
        if strict(decode(a)) != {'alg': 'RS256', 'typ': 'JWT'}: raise ValueError()
        key('identity').public_key().verify(decode(c), (a+'.'+b).encode(), padding.PKCS1v15(), hashes.SHA256())
        claims = strict(decode(b)); now = int(time.time())
        record = strict((DATA/'identity.json').read_bytes())
        if claims['iss'] != ISSUER or claims['aud'] != 'toolgate-control' or claims['tenant_id'] != TENANT or claims['sub'] != 'admin' or not claims['iat'] <= now < claims['exp'] <= claims['iat']+900 or claims.get('password_generation') != record['generation']: raise ValueError()
        return claims
    except Exception: raise PermissionError() from None


def certificates():
    if (DATA/'keys/server.crt').exists(): return
    now = datetime.datetime.now(datetime.timezone.utc)
    ca_key = key('device-ca'); server_key = key('server-tls')
    name = x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, 'ToolGate Quickstart Device CA')])
    ca = x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(ca_key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=3650)).add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True).add_extension(x509.KeyUsage(False, False, False, False, False, True, True, False, False), critical=True).sign(ca_key, hashes.SHA256())
    server = x509.CertificateBuilder().subject_name(x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME, 'localhost')])).issuer_name(name).public_key(server_key.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-datetime.timedelta(minutes=1)).not_valid_after(now+datetime.timedelta(days=365)).add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True).add_extension(x509.SubjectAlternativeName([x509.DNSName('localhost'), x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]), critical=False).add_extension(x509.ExtendedKeyUsage([x509.oid.ExtendedKeyUsageOID.SERVER_AUTH]), critical=False).sign(ca_key, hashes.SHA256())
    atomic(DATA/'keys/device-ca.crt', ca.public_bytes(serialization.Encoding.PEM))
    atomic(DATA/'keys/server.crt', server.public_bytes(serialization.Encoding.PEM))
    subprocess.run(['keytool', '-importcert', '-noprompt', '-alias', 'device-ca', '-file', str(DATA/'keys/device-ca.crt'), '-keystore', str(DATA/'keys/device-trust.p12'), '-storetype', 'PKCS12', '-storepass', 'changeit'], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def initialize():
    password_disabled(); mode = database_mode()
    os.umask(0o077)
    DATA.mkdir(exist_ok=True)
    if DATA.is_symlink() or DATA.stat().st_uid != os.getuid(): raise ValueError('Private service-owned /data required')
    DATA.chmod(0o700)
    for name in ('keys', 'run', 'state', 'hotfolder', 'artifacts'):
        path = DATA/name
        if path.is_symlink(): raise ValueError('Unsafe local layout')
        path.mkdir(exist_ok=True); path.chmod(0o700)
    layout = DATA/'layout.json'
    if layout.exists() and strict(layout.read_bytes()) != {'layoutVersion': 1}: raise ValueError('Unsupported local layout; restore or use a compatible image')
    if not layout.exists(): atomic(layout, '{"layoutVersion":1}')
    storage = DATA/'storage.json'
    previous = strict(storage.read_bytes())['mode'] if storage.exists() else ('sqlite' if (DATA/'state/control.sqlite').exists() else mode)
    if previous != mode: raise ValueError('Use separate /data when changing database backend; migration is explicit')
    if not storage.exists(): atomic(storage, json.dumps({'mode': mode}))
    if not (DATA/'identity.json').exists():
        supplied = os.environ.pop('TOOLGATE_BOOTSTRAP_PASSWORD', None)
        password = supplied if supplied is not None else secrets.token_urlsafe(32)
        atomic(DATA/'identity.json', json.dumps(password_record(password)))
        if supplied is None: atomic(DATA/'bootstrap-password', password+'\n')
    else: os.environ.pop('TOOLGATE_BOOTSTRAP_PASSWORD', None)  # Existing identity is never reset by startup settings.
    for name in ('identity', 'policy', 'permit', 'device-ca', 'server-tls', 'fleet', 'package-release'): key(name)
    if not (DATA/'keys/vault.key').exists(): atomic(DATA/'keys/vault.key', secrets.token_bytes(32))
    certificates()
    fleet_keys()


def call(path, token=None, body=None, port=8082, secure=False, method=None, extra_headers=None):
    client = http.client.HTTPSConnection('127.0.0.1', port, timeout=10, context=ssl.create_default_context(cafile=str(DATA/'keys/device-ca.crt'))) if secure else http.client.HTTPConnection('127.0.0.1', port, timeout=10)
    headers = {'Content-Type': 'application/json', 'X-Request-ID': secrets.token_hex(16)}
    if token: headers['Authorization'] = 'Bearer '+token
    if body is not None: headers['Idempotency-Key'] = secrets.token_hex(16)
    if extra_headers: headers.update(extra_headers)
    try:
        client.request(method or ('POST' if body is not None else 'GET'), path, json.dumps(body).encode() if body is not None else None, headers)
        response = client.getresponse(); raw = response.read(2*1024*1024+1)
        if len(raw)>2*1024*1024: raise ValueError()
        return response.status, raw, dict(response.getheaders())
    finally: client.close()


def client_credentials():
    """Optional administrator-installed hashes; never issue an agent token through the portal."""
    if os.environ.get('TOOLGATE_QUICKSTART_CLIENT_CREDENTIALS', 'false') != 'true': return []
    path = DATA/'client-runtime-credentials.json'
    if not path.exists(): return []
    metadata = path.stat()
    if path.is_symlink() or not path.is_file() or metadata.st_size > 65536 or metadata.st_mode & 0o077 or metadata.st_uid not in (0, os.geteuid()): raise ValueError('Unsafe client credential file')
    values = strict(path.read_bytes())
    fields = {'tokenSha256','tenantId','userId','agentId','deviceId','expiresAtUnixMs'}
    if not isinstance(values, list) or len(values) > 32: raise ValueError('Invalid client credentials')
    now = int(time.time()*1000)
    for value in values:
        if not isinstance(value, dict) or set(value) != fields or value['tenantId'] != TENANT or not isinstance(value['expiresAtUnixMs'], int) or isinstance(value['expiresAtUnixMs'], bool) or not isinstance(value['tokenSha256'], str) or not re.fullmatch('[a-f0-9]{64}', value['tokenSha256']): raise ValueError('Invalid client credentials')
        for field in ('userId','agentId','deviceId'):
            if not isinstance(value[field], str) or not re.fullmatch('[A-Za-z0-9][A-Za-z0-9._:/-]{0,127}', value[field]): raise ValueError('Invalid client identity')
    return [value for value in values if value['expiresAtUnixMs'] > now]

def configure():
    runtime = secrets.token_urlsafe(48); atomic(DATA/'run/runtime-token', runtime)
    credentials = [{'tokenSha256': hashlib.sha256(runtime.encode()).hexdigest(), 'tenantId': TENANT, 'userId': 'local-tools', 'agentId': 'agent-default', 'deviceId': 'local-builtins', 'expiresAtUnixMs': int(time.time()*1000)+86400000}]
    credentials.extend(client_credentials())
    atomic(DATA/'run/credentials.json', json.dumps(credentials))
    numbers = key('policy').public_key().public_numbers()
    encode_number = lambda n: base64.urlsafe_b64encode(n.to_bytes((n.bit_length()+7)//8,'big')).rstrip(b'=').decode()
    atomic(DATA/'run/keyring.json', json.dumps({'keys': [{'keyId': 'policy-local', 'modulus': encode_number(numbers.n), 'exponent': encode_number(numbers.e)}]}))
    catalog = strict(Path('/opt/quickstart/builtins.json').read_bytes())['tools']
    config = {'listen': '127.0.0.1:8081', 'managementListen': '127.0.0.1:9091', 'trustedTlsProxy': False, 'allowedOrigins': [],
              'limits': {'maxBodyBytes':65536,'maxHeaderBytes':8192,'maxConcurrentRequests':128,'maxConnections':256,'requestsPerSecond':1000,'requestTimeoutMs':30000,'connectionTimeoutMs':35000,'shutdownTimeoutMs':35000,'auditQueueCapacity':256},
              'localMcp': {'url':'http://127.0.0.1:8082','tokenPath':'/data/run/machine-token','developmentLoopbackHttp':True},
              'bundleSource': {'url': 'http://127.0.0.1:8082/api/control/v1/bundles/current', 'tenantId': TENANT, 'issuer': 'control', 'audience': 'gateway', 'keyringPath': '/data/run/keyring.json', 'tokenPath': '/data/run/machine-token', 'minimumSequence': 0, 'maxGraceMs': 0, 'pollIntervalMs': 1000, 'fetchTimeoutMs': 1000, 'developmentLoopbackHttp': True},
              'approval': {'url': 'http://127.0.0.1:8082', 'tokenPath': '/data/run/machine-token', 'privateKeyPath': '/data/keys/permit.pem', 'keyId': 'permit-local', 'issuer': 'gateway', 'audience': 'endpoint', 'requestTimeoutMs': 1000, 'permitLifetimeMs': 10000, 'developmentLoopbackHttp': True},
              'extractors': [{'toolId': item['toolId'], 'action': item['action'], 'pointer': '/path', 'kind': 'FILE' if item['toolId'].startswith('hotfolder.') else 'CUSTOM'} for item in catalog]}
    atomic(DATA/'run/gateway.json', json.dumps(config))
    atomic(DATA/'run/builtins.json', json.dumps({'hotfolder': {'root': '/data/hotfolder', 'maxFileBytes': 65536, 'maxEntries': 256, 'extensions': ['txt', 'md', 'json', 'csv']}, 'gatewayUrl': 'https://localhost:8443', 'gatewayTokenPath': '/data/run/runtime-token', 'gatewayCaPath': None, 'deviceId': 'local-builtins', 'webSearchTokenPath': None}))
    machine()
    env = dict(os.environ)
    settings = {'TOOLGATE_QUICKSTART_ENABLED': 'true', 'TOOLGATE_QUICKSTART_DATABASE': '/data/state/control.sqlite',
                'QUARKUS_PROFILE':'quickstart', 'QUARKUS_FLYWAY_ACTIVE':'false', 'QUARKUS_DATASOURCE_ACTIVE': 'false', 'QUARKUS_DATASOURCE_HEALTH_ENABLED': 'false', 'QUARKUS_FLYWAY_MIGRATE_AT_START': 'false',
                'QUARKUS_HTTP_HOST': '0.0.0.0', 'QUARKUS_HTTP_PORT': '8082', 'QUARKUS_HTTP_SSL_PORT': '8443',
                'QUARKUS_HTTP_INSECURE_REQUESTS': 'enabled', 'QUARKUS_MANAGEMENT_HOST': '127.0.0.1',
                'QUARKUS_HTTP_SSL_CERTIFICATE_FILES': '/data/keys/server.crt', 'QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES': '/data/keys/server-tls.pem',
                'QUARKUS_HTTP_SSL_CERTIFICATE_TRUST_STORE_FILE': '/data/keys/device-trust.p12', 'QUARKUS_HTTP_SSL_CERTIFICATE_TRUST_STORE_PASSWORD': 'changeit',
                'MP_JWT_VERIFY_ISSUER': ISSUER, 'MP_JWT_VERIFY_AUDIENCES': 'toolgate-control', 'MP_JWT_VERIFY_PUBLICKEY_LOCATION': '/data/keys/identity-public.pem',
                'TOOLGATE_CONTROL_BUNDLE_ENABLED': 'true', 'TOOLGATE_CONTROL_BUNDLE_KEY_ID': 'policy-local', 'TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH': '/data/keys/policy.pem',
                'TOOLGATE_QUICKSTART_ARTIFACT_DIRECTORY': '/data/artifacts',
                'TOOLGATE_CONTROL_FLEET_ENABLED': 'true', 'TOOLGATE_CONTROL_FLEET_KEY_ID': 'fleet-local',
                'TOOLGATE_CONTROL_FLEET_PRIVATE_KEY_PATH': '/data/keys/fleet.pem',
                'TOOLGATE_CONTROL_FLEET_RELEASE_KEYS_PATH': '/data/keys/release-keys.json',
                'TOOLGATE_CONTROL_FLEET_ORGANIZATION_KEYS_PATH': '/data/keys/organization-keys.json',
                'TOOLGATE_CONTROL_FLEET_ARTIFACT_ORIGIN': env.get('TOOLGATE_CONTROL_FLEET_ARTIFACT_ORIGIN', 'https://localhost:8443/artifacts'),
                'TOOLGATE_CONTROL_FLEET_ARTIFACT_CA_PATH': env.get('TOOLGATE_CONTROL_FLEET_ARTIFACT_CA_PATH', '/data/keys/device-ca.crt'),
                'TOOLGATE_CONTROL_APPROVAL_ENABLED': 'true', 'TOOLGATE_CONTROL_ENDPOINT_ENABLED': 'true',
                'TOOLGATE_CONTROL_ENDPOINT_PRIVATE_KEY_PATH': '/data/keys/device-ca.pem', 'TOOLGATE_CONTROL_ENDPOINT_CA_CERTIFICATE_PATH': '/data/keys/device-ca.crt',
                'TOOLGATE_CONTROL_ENDPOINT_TENANT_ID': TENANT, 'TOOLGATE_CONTROL_ENDPOINT_SERVER_ID': 'quickstart-server',
                'TOOLGATE_CONTROL_ENDPOINT_ORGANIZATION': 'Quickstart',
                'TOOLGATE_CONTROL_ENDPOINT_CONTROL_URL': env.get('TOOLGATE_CONTROL_ENDPOINT_CONTROL_URL', 'https://localhost:8443'),
                'TOOLGATE_CONTROL_ENDPOINT_GATEWAY_URL': env.get('TOOLGATE_CONTROL_ENDPOINT_GATEWAY_URL', 'https://localhost:8443'),
                'TOOLGATE_CLIENT_DOWNLOADS_DIRECTORY': '/opt/toolgate/client-downloads'}
    if database_mode() == 'postgresql':
        for name in ('QUARKUS_DATASOURCE_JDBC_URL','QUARKUS_DATASOURCE_USERNAME','QUARKUS_DATASOURCE_PASSWORD','QUARKUS_FLYWAY_USERNAME','QUARKUS_FLYWAY_PASSWORD'):
            if not env.get(name): raise ValueError('External database settings required')
        settings.update(TOOLGATE_QUICKSTART_STORAGE='postgresql', QUARKUS_FLYWAY_ACTIVE='true',
            QUARKUS_DATASOURCE_ACTIVE='true', QUARKUS_DATASOURCE_HEALTH_ENABLED='true', QUARKUS_FLYWAY_MIGRATE_AT_START='true')
    env.update(settings)
    env.setdefault('TOOLGATE_DEFAULT_POLICY_IDS', ','.join('default-'+item['toolId'] for item in catalog if item['toolId']!='web.search'))
    return env, catalog


def fleet_keys():
    """Persistent disjoint package release and desired-state trust domains."""
    for name, kid, filename in (('fleet', 'fleet-local', 'organization-keys.json'),
                                ('package-release', 'package-local', 'release-keys.json')):
        numbers = key(name).public_key().public_numbers()
        encode = lambda value: base64.urlsafe_b64encode(value.to_bytes((value.bit_length()+7)//8, 'big')).rstrip(b'=').decode()
        atomic(DATA/'keys'/filename, json.dumps([{'kid': kid, 'n': encode(numbers.n), 'e': encode(numbers.e)}]))


def machine():
    atomic(DATA/'run/machine-token', jwt(['toolgate-bundle-reader', 'toolgate-approval-gateway', 'toolgate-relay-gateway'], 'gateway'))


def seed(catalog):
    admin = jwt(GROUPS, 'bootstrap', directory_bound=False)
    fixture = strict(Path('/opt/quickstart/seed.json').read_bytes())
    records = [('users', {'id': 'admin', 'name': 'Quickstart administrator', 'enabled': True, 'revision': 1, 'access': {'role':'SUPER_ADMIN','templateIds':[],'deviceGroupIds':[]}}),
               ('users', {'id': 'local-tools', 'name': 'Local tool requester', 'enabled': True, 'revision': 1}),
               ('teams', {'id': 'team-default', 'name': 'Default workspace', 'enabled': True, 'revision': 1, 'userIds': ['admin', 'local-tools']}),
               ('agents', {'id': 'agent-default', 'name': 'Local agent', 'enabled': True, 'revision': 1, 'ownerUserId': 'local-tools'}),
               ('devices', {'id':'local-builtins','name':'Quickstart fixed executor','enabled':True,'revision':1,'ownerUserId':'local-tools'})]
    for item in catalog:
        if item['toolId']=='web.search': continue
        resource = {'kind': 'FILE' if item['toolId'].startswith('hotfolder.') else 'CUSTOM', 'locator': 'hotfolder'}
        definition = {'id': item['toolId'], 'name': item['toolId'], 'description': item['description'], 'actions': [{'name': item['action'], 'resourceKinds': [resource['kind']]}], 'inputSchema': item['inputSchema'], 'outputSchema': {'type': 'object'}}
        records.append(('tools', {'id': item['toolId'], 'name': item['toolId'], 'enabled': True, 'revision': 1, 'definition': definition}))
        # File effects are demonstrated only on an exact local note; all other paths default BLOCK.
        if item['toolId'].startswith('hotfolder.') and item['toolId'] not in ('hotfolder.list','hotfolder.watch_events'): resource['locator'] = 'welcome.txt'
        records.append(('policies', {**fixture, 'id': 'default-'+item['toolId'], 'name': 'Default '+item['toolId'], 'toolId': item['toolId'], 'action': item['action'], 'resource': resource, 'decision': 'ASK' if item['action'] in ('write','append','move','copy','mkdir') else 'ALLOW', 'userIds': ['local-tools'], 'teamIds': ['team-default'], 'agentIds': ['agent-default'], 'deviceIds': []}))
    for kind, record in records:
        status, raw, _ = call('/api/control/v1/'+kind+'/'+record['id'], admin)
        if status==404:
            status, raw, _ = call('/api/control/v1/'+kind, admin, record)
            if status!=201: raise ValueError('Default record rejected: '+kind+' '+str(status))
        elif status!=200: raise ValueError('Default state unavailable')
        elif kind=='users' and record['id']=='admin':
            existing=strict(raw)
            if 'access' not in existing:
                status, _, _ = call('/api/control/v1/users/admin',admin,{**existing,'access':record['access']},method='PUT',extra_headers={'If-Match':'"'+str(existing['revision'])+'"','Idempotency-Key':'seed-admin-role'})
                if status!=200: raise ValueError('Administrator role upgrade rejected')
    note = DATA/'hotfolder/welcome.txt'
    if not note.exists(): atomic(note, 'Welcome to ToolGate. Writes require human approval.\n')
    publish()


def publish():
    admin = jwt(GROUPS)
    status, raw, _ = call('/api/control/v1/config/export', admin)
    if status!=200: raise ValueError('Directory unavailable')
    revision = strict(raw)['revision']
    status, raw, _ = call('/api/control/v1/bundles/current', admin)
    sequence = 0 if status==404 else strict(base64.urlsafe_b64decode(strict(raw)['jws'].split('.')[1]+'=='))['sequence']
    status, _, _ = call('/api/control/v1/bundles/publish', admin, {'directoryRevision': revision, 'expectedSequence': sequence, 'lifetimeMs': 86400000, 'graceMs': 0})
    if status!=201: raise ValueError('Signed bundle publication failed')


class BoundedServer(http.server.ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, address, handler, ready):
        super().__init__(address,handler)
        self.ready=ready
        self.capacity=threading.BoundedSemaphore(16)
        self.identity=LocalIdentity()
        self.cache=CatalogCache()
        self.login_lock=threading.Lock()
        self.login_times=[]
    def process_request(self, request, address):
        if not self.capacity.acquire(False): request.close(); return
        try: super().process_request(request, address)
        except Exception: self.capacity.release(); raise
    def process_request_thread(self, request, address):
        try: super().process_request_thread(request, address)
        finally: self.capacity.release()


class Handler(http.server.BaseHTTPRequestHandler):
    server_version = 'ToolGate'
    def log_message(self, *_): pass
    def setup(self):
        super().setup(); self.connection.settimeout(10)
    def reply(self, status, body, kind='application/json', headers=None):
        request_id=(headers or {}).get('X-Request-ID') or (headers or {}).get('x-request-id') or self.headers.get('X-Request-ID','')
        if not re.fullmatch(r'[A-Za-z0-9._-]{1,128}',request_id): request_id=secrets.token_hex(16)
        if status>=400:
            value=strict(body) if isinstance(body,bytes) else body
            if isinstance(value,dict) and 'code' in value:
                code={'INVALID_REQUEST':'VALIDATION','RATE_LIMITED':'DEPENDENCY_UNAVAILABLE'}.get(value['code'],value['code'])
                body={'code':code,'requestId':request_id,'retryable':status in (429,503)}
        raw = body if isinstance(body, bytes) else json.dumps(body).encode()
        self.send_response(status); self.send_header('Content-Type', kind)
        self.send_header('Content-Length', str(len(raw))); self.send_header('Cache-Control', 'no-store')
        self.send_header('X-Content-Type-Options', 'nosniff'); self.send_header('X-Request-ID', request_id)
        for key_, value in (headers or {}).items():
            if key_.lower() in ('content-security-policy','referrer-policy','permissions-policy','content-disposition','etag','location'): self.send_header(key_, value)
        self.end_headers(); self.wfile.write(raw)
    def dispatch(self):
        try:
            host = self.headers.get('Host', '')
            if not re.fullmatch(r'(localhost|127\.0\.0\.1)(:[0-9]{1,5})?', host): raise PermissionError()
            origin = self.headers.get('Origin')
            if origin is not None and origin != 'http://'+host: raise PermissionError()
            if self.headers.get('Transfer-Encoding') or len(self.path)>2048 or '\\' in self.path or '..' in urllib.parse.unquote(self.path): raise ValueError()
            length = int(self.headers.get('Content-Length','0'))
            if not 0<=length<=65536: raise ValueError()
            raw = self.rfile.read(length)
            if len(raw)!=length: raise ValueError()
            path = urllib.parse.urlsplit(self.path).path
            if path=='/api/quickstart/v1/status' and self.command=='GET':
                return self.reply(200, {'mode': 'Quickstart', 'nonHa': True, 'ready': self.server.ready.is_set(), 'version': Path('/opt/quickstart/VERSION').read_text().strip(), 'passwordRequired': not password_disabled(), 'database': database_mode(), 'cache': os.environ.get('TOOLGATE_CACHE_MODE','embedded')})
            if path=='/health/ready' and self.command=='GET': return self.reply(200 if self.server.ready.is_set() else 503, {'ready': self.server.ready.is_set()})
            if path=='/api/quickstart/v1/login' and self.command=='POST':
                with self.server.login_lock:
                    now = time.monotonic(); self.server.login_times[:] = [t for t in self.server.login_times if now-t<60]
                    if len(self.server.login_times)>=10: return self.reply(429, {'code':'RATE_LIMITED'})
                    self.server.login_times.append(now)
                data = strict(raw)
                if not isinstance(data, dict) or set(data)-{'password','newPassword'}: raise ValueError()
                if password_disabled():
                    record = strict((DATA/'identity.json').read_bytes())
                    return self.reply(200, {'accessToken': jwt(GROUPS, 'admin', generation=record['generation'])})
                return self.reply(200, {'accessToken': self.server.identity.authenticate(data.get('password'), data.get('newPassword'))})
            protected = path.startswith(('/api/control/', '/api/quickstart/'))
            if protected: session(self.headers.get('Authorization',''))
            if path=='/api/quickstart/v1/tools' and self.command=='GET':
                catalog = self.server.cache.get()
                return self.reply(200, {**catalog, 'tools': [{**item, 'enabled': item.get('enabled', False) and item['toolId']!='web.search'} for item in catalog['tools']]})
            if path=='/api/quickstart/v1/invoke' and self.command=='POST':
                data = strict(raw)
                token_ = (DATA/'run/runtime-token').read_text()
                status, response, upstream = call('/invoke', token_, data, port=8083)
                if status==403 and strict(response).get('decision')=='ASK': status=202
                return self.reply(status, response, headers=upstream)
            if path=='/api/quickstart/v1/vault' and self.command in ('GET','POST'):
                with Database() as db:
                    if self.command=='GET': return self.reply(200, {'names': [r[0] for r in db.execute('SELECT name FROM quickstart_vault ORDER BY name LIMIT 256')]})
                    data = strict(raw)
                    if set(data)!= {'name','value'} or not isinstance(data['name'],str) or not re.fullmatch('[a-z0-9][a-z0-9/-]{0,127}',data['name']) or not isinstance(data['value'],str) or not 1<=len(data['value'].encode())<=32768: raise ValueError()
                    db.execute('BEGIN IMMEDIATE')
                    if db.execute('SELECT count(*) FROM quickstart_vault').fetchone()[0]>=256 and not db.execute('SELECT 1 FROM quickstart_vault WHERE name=?',(data['name'],)).fetchone(): raise ValueError()
                    nonce = secrets.token_bytes(12)
                    cipher = nonce+AESGCM((DATA/'keys/vault.key').read_bytes()).encrypt(nonce, data['value'].encode(), data['name'].encode())
                    db.execute('INSERT INTO quickstart_vault VALUES(?,?) ON CONFLICT(name) DO UPDATE SET cipher=excluded.cipher',(data['name'],cipher))
                    db.execute('INSERT INTO control_audit(tenant_id,actor_id,operation,target,revision,request_id,request_digest) VALUES(?,?,?,?,?,?,?)',(TENANT,hashlib.sha256((ISSUER+'\nadmin').encode()).hexdigest(),'VAULT_PUT','vault:'+data['name'],1,secrets.token_hex(16),hashlib.sha256(data['name'].encode()).hexdigest()))
                    db.commit()
                return self.reply(201, {'stored': True})
            if path.startswith(('/console/', '/api/control/v1/', '/api/public/v1/clients', '/api/public/v1/installers')) or path in ('/','/console'):
                # Direct TLS adapter is used for enrollment administration; device peers use :8443 itself.
                secure = path.startswith(('/api/control/v1/endpoint/', '/api/control/v1/builder/', '/api/control/v1/fleet/'))
                client = http.client.HTTPSConnection('127.0.0.1',8443,context=ssl.create_default_context(cafile=str(DATA/'keys/device-ca.crt')),timeout=10) if secure else http.client.HTTPConnection('127.0.0.1',8082,timeout=10)
                headers = {k:v for k,v in self.headers.items() if k.lower() in ('authorization','content-type','idempotency-key','if-match','x-request-id','traceparent')}
                try:
                    client.request(self.command, self.path, raw or None, headers)
                    response = client.getresponse(); body = response.read(64*1024*1024+1)
                    if len(body)>64*1024*1024: raise ValueError()
                    if path in ('/console/','/console/index.html') and response.status==200 and b'toolgate-mode' not in body:
                        body = body.replace(b'<head>', b'<head><meta name="toolgate-mode" content="quickstart">', 1)
                    return self.reply(response.status, body, response.getheader('Content-Type','application/octet-stream'),dict(response.getheaders()))
                finally: client.close()
            return self.reply(404, {'code':'NOT_FOUND'})
        except PermissionError: self.reply(401, {'code':'UNAUTHORIZED'})
        except (ValueError, KeyError, TypeError): self.reply(400, {'code':'INVALID_REQUEST'})
        except Exception: self.reply(503, {'code':'DEPENDENCY_UNAVAILABLE'})
    do_GET = dispatch
    do_POST = dispatch
    do_PUT = dispatch
    do_DELETE = dispatch


def backup(destination):
    """Offline backup under the exclusive data lock; includes all trust/vault custody."""
    import shutil
    if database_mode() != 'sqlite': raise ValueError('External PostgreSQL requires pg_dump plus matching custody backup')
    destination = Path(destination)
    if destination.exists() or not destination.is_absolute() or destination.is_relative_to(DATA): raise ValueError('Backup must be a new absolute path outside /data')
    destination.mkdir(mode=0o700)
    if any(p.is_symlink() for p in DATA.rglob('*')): raise ValueError('Unsafe backup source')
    shutil.copytree(DATA, destination/'data', ignore=shutil.ignore_patterns('run','service.lock','control.sqlite-wal','control.sqlite-shm'))
    if (DATA/'state/control.sqlite').exists():
        # sqlite3's transaction context does not close a connection. Close both
        # before hashing: a WAL checkpoint can remove sidecars at close time.
        with closing(sqlite3.connect(DATA/'state/control.sqlite')) as source, closing(sqlite3.connect(destination/'data/state/control.sqlite')) as target: source.backup(target)
    atomic(destination/'manifest.json', json.dumps({'layoutVersion':1, 'files': {p.relative_to(destination/'data').as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in (destination/'data').rglob('*') if p.is_file()}}))


def restore(source):
    import shutil
    if any(p.name!='service.lock' for p in DATA.iterdir()): raise ValueError('Restore requires empty /data')
    source = Path(source); manifest = strict((source/'manifest.json').read_bytes())
    if manifest['layoutVersion']!=1: raise ValueError()
    for name, digest in manifest['files'].items():
        if not name or '..' in Path(name).parts or Path(name).is_absolute(): raise ValueError()
        path = source/'data'/name
        if path.is_symlink() or hashlib.sha256(path.read_bytes()).hexdigest()!=digest: raise ValueError('Backup integrity failure')
    if any(p.is_symlink() for p in (source/'data').rglob('*')): raise ValueError('Unsafe backup entry')
    actual = {p.relative_to(source/'data').as_posix() for p in (source/'data').rglob('*') if p.is_file()}
    if actual != set(manifest['files']): raise ValueError('Unexpected backup file')
    shutil.copytree(source/'data',DATA,dirs_exist_ok=True)
    for path in DATA.rglob('*'): path.chmod(0o700 if path.is_dir() else 0o600)


def main():
    parser = argparse.ArgumentParser(); parser.add_argument('--backup'); parser.add_argument('--restore'); parser.add_argument('--health',action='store_true'); args = parser.parse_args()
    if args.health:
        with urllib.request.urlopen('http://127.0.0.1:8080/health/ready',timeout=3) as reply: return 0 if reply.status==200 else 1
    os.umask(0o077); DATA.mkdir(exist_ok=True)
    with (DATA/'service.lock').open('a') as lock:
        fcntl.flock(lock,fcntl.LOCK_EX|fcntl.LOCK_NB)
        if args.backup: backup(args.backup); return
        if args.restore: restore(args.restore); return
        initialize(); env, catalog = configure(); children = []; stop=threading.Event(); ready_state=threading.Event()
        signal.signal(signal.SIGTERM, lambda *_: stop.set()); signal.signal(signal.SIGINT, lambda *_: stop.set())
        server = None
        try:
            children.append(subprocess.Popen(['java','-jar','/app/quarkus-run.jar'],env=env))
            deadline = time.monotonic()+90
            while not stop.is_set():
                if children[0].poll() is not None: raise ValueError('Control failed')
                try:
                    if call('/q/health/ready',port=9092)[0]==200: break
                except Exception: pass
                if time.monotonic()>deadline: raise ValueError('Control readiness timeout')
                stop.wait(.2)
            seed(catalog)
            child_env = {name:value for name,value in os.environ.items() if not name.startswith(('QUARKUS_DATASOURCE_', 'QUARKUS_FLYWAY_', 'TOOLGATE_REDIS_'))}
            gateway_env = dict(child_env, TOOLGATE_GATEWAY_CONFIG='/data/run/gateway.json',TOOLGATE_GATEWAY_CREDENTIALS='/data/run/credentials.json')
            children.append(subprocess.Popen(['/usr/local/bin/olo-toolgate-gateway'],env=gateway_env))
            children.append(subprocess.Popen(['/usr/local/bin/quickstart-tools'],env=child_env))
            server = BoundedServer(('0.0.0.0',8080),Handler,ready_state)
            threading.Thread(target=server.serve_forever,daemon=True).start()
            renewed = time.monotonic(); published = renewed; boot = renewed
            while not stop.wait(1):
                if any(child.poll() is not None for child in children): raise ValueError('Composed process failed')
                ready = call('/q/health/ready',port=9092)[0]==200 and call('/v1/health/ready',port=9091)[0]==200
                if ready: ready_state.set()
                else: ready_state.clear()
                now = time.monotonic()
                if now-renewed>=600: machine(); renewed=now
                if now-published>=3600: publish(); published=now
                if now-boot>=82800:
                    ready_state.clear()
                    for child in children[1:]:
                        child.terminate(); child.wait(timeout=20)
                    configure()
                    children[1] = subprocess.Popen(['/usr/local/bin/olo-toolgate-gateway'],env=gateway_env)
                    children[2] = subprocess.Popen(['/usr/local/bin/quickstart-tools'],env=child_env)
                    boot=now
        finally:
            ready_state.clear()
            if server: server.shutdown(); server.server_close()
            for child in reversed(children):
                if child.poll() is None: child.terminate()
            for child in children:
                try: child.wait(timeout=25)
                except subprocess.TimeoutExpired: child.kill(); child.wait()


if __name__=='__main__':
    try: main()
    except Exception:
        print(json.dumps({'service':'quickstart','event':'startup_or_runtime_failure','code':'DEPENDENCY_UNAVAILABLE'}),flush=True)
        raise SystemExit(1) from None
