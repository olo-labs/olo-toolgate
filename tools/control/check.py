# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real PostgreSQL and signed-token HTTP gates. Owns and removes only its isolated containers."""
import argparse
import base64
import contextlib
import http.server
import json
import os
import secrets
import socket
import subprocess
import sys
import tempfile
import time
import threading
import urllib.error
import urllib.request
from pathlib import Path

from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding, rsa

ROOT = Path(__file__).resolve().parents[2]
POSTGRES = 'postgres:17-bookworm@sha256:639ab7ceb90e13123085b741fb31ef493fba25463002f6da665352e7b534b652'


def run(args, **kwargs):
    return subprocess.run(args, cwd=ROOT, check=True, **kwargs)


@contextlib.contextmanager
def database():
    password = secrets.token_urlsafe(32)
    env = dict(os.environ, POSTGRES_PASSWORD=password)
    container = run(['docker', 'run', '-d', '--memory=512m', '--cpus=2', '-e', 'POSTGRES_PASSWORD',
                     '-p', '127.0.0.1::5432', '-p', '127.0.0.1::8082', '-p', '127.0.0.1::9092', POSTGRES], env=env, capture_output=True, text=True).stdout.strip()
    try:
        deadline = time.monotonic() + 60
        # The image's temporary init server accepts Unix sockets before initialization finishes.
        # TCP readiness observes only the final server, avoiding a startup race.
        while subprocess.run(['docker', 'exec', container, 'pg_isready', '-h', '127.0.0.1', '-U', 'postgres'], capture_output=True).returncode:
            if time.monotonic() > deadline: raise RuntimeError('Isolated PostgreSQL did not become ready')
            time.sleep(.2)
        info = json.loads(run(['docker', 'inspect', container], capture_output=True, text=True).stdout)[0]
        port = info['NetworkSettings']['Ports']['5432/tcp'][0]['HostPort']
        # SQL goes through stdin; secrets never appear in command arguments or logs.
        sql = f"""CREATE ROLE toolgate_control_runtime NOLOGIN;
CREATE ROLE control_migrator LOGIN CREATEDB PASSWORD '{password}';
CREATE ROLE control_app LOGIN PASSWORD '{password}' IN ROLE toolgate_control_runtime;
CREATE DATABASE control OWNER control_migrator;"""
        run(['docker', 'exec', '-i', container, 'psql', '-U', 'postgres', '-v', 'ON_ERROR_STOP=1'], input=sql, capture_output=True, text=True)
        yield {'CONTROL_TEST_URL':f'jdbc:postgresql://127.0.0.1:{port}/control?sslmode=disable', 'CONTROL_TEST_PASSWORD':password,
               'container':container, 'port':port,
               'runtimePort':info['NetworkSettings']['Ports']['8082/tcp'][0]['HostPort'],
               'managementPort':info['NetworkSettings']['Ports']['9092/tcp'][0]['HostPort']}
    finally:
        run(['docker', 'rm', '-f', container], capture_output=True)


def request(url, token=None, body=None, method='GET', headers=None):
    fields = {'Content-Type':'application/json', **(headers or {})}
    if token: fields['Authorization'] = 'Bearer ' + token
    data = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method, headers=fields)
    try:
        with urllib.request.urlopen(req, timeout=20) as response: return response.status, response.read(), dict(response.headers)
    except urllib.error.HTTPError as error: return error.code, error.read(), dict(error.headers)


def keypair(work):
    key = rsa.generate_private_key(public_exponent=65537, key_size=2048)
    path = work/'jwt-public.pem'
    path.write_bytes(key.public_key().public_bytes(serialization.Encoding.PEM, serialization.PublicFormat.SubjectPublicKeyInfo))
    path.chmod(0o644)
    return key, path


def token(key, **claims):
    now = int(time.time())
    data = {'iss':'https://identity.example.invalid', 'aud':'toolgate-control', 'sub':'test-admin',
            'iat':now, 'exp':now+300, 'tenant_id':'http-tenant', 'groups':['toolgate-admin'], **claims}
    def encode(value): return base64.urlsafe_b64encode(json.dumps(value, separators=(',', ':')).encode()).rstrip(b'=')
    message = encode({'alg':'RS256', 'typ':'JWT'}) + b'.' + encode(data)
    signature = key.sign(message, padding.PKCS1v15(), hashes.SHA256())
    return (message + b'.' + base64.urlsafe_b64encode(signature).rstrip(b'=')).decode()


def environment(db, public):
    return dict(os.environ, QUARKUS_DATASOURCE_JDBC_URL=db['CONTROL_TEST_URL'], QUARKUS_DATASOURCE_USERNAME='control_app',
                QUARKUS_DATASOURCE_PASSWORD=db['CONTROL_TEST_PASSWORD'], QUARKUS_FLYWAY_USERNAME='control_migrator',
                QUARKUS_FLYWAY_PASSWORD=db['CONTROL_TEST_PASSWORD'], MP_JWT_VERIFY_PUBLICKEY_LOCATION=public.as_posix(),
                TOOLGATE_CONTROL_DEVELOPMENT_MODE='true', MP_JWT_VERIFY_ISSUER='https://identity.example.invalid', MP_JWT_VERIFY_AUDIENCES='toolgate-control')


def free_port():
    with socket.socket() as server:
        server.bind(('127.0.0.1', 0)); return server.getsockname()[1]


def ready(management, process=None):
    deadline = time.monotonic() + 90
    while True:
        try:
            if request(management+'/q/health/ready')[0] == 200: return
        except (urllib.error.URLError, TimeoutError, ConnectionError): pass
        if process is not None and process.poll() is not None: raise RuntimeError('Control Plane failed startup; inspect private test log')
        if time.monotonic() > deadline: raise RuntimeError('Control Plane readiness deadline exceeded')
        time.sleep(.2)


def http_tests(runtime, management, key):
    static_tests(runtime)
    api = runtime+'/api/control/v1'
    admin = token(key); reader = token(key, groups=['toolgate-reader']); user = {'id':'user', 'name':'secret-redaction-sentinel', 'enabled':True, 'revision':1}
    for invalid in (None, token(rsa.generate_private_key(public_exponent=65537,key_size=2048)), token(key, exp=int(time.time())-1),
                    token(key, iss='https://wrong.example.invalid'), token(key, aud='gateway-runtime'), token(key, tenant_id=None), token(key, iat=int(time.time())+300)):
        status, body, _ = request(api+'/users', invalid)
        assert status == 401, (status, body)
        assert json.loads(body)['code'] == 'UNAUTHORIZED', body
    assert request(api+'/users', token(key, groups=[]))[0] == 403
    assert request(api+'/users', reader, user, 'POST', {'Idempotency-Key':'reader-key'})[0] == 403
    assert request(api+'/users', admin, b'{', 'POST', {'Idempotency-Key':'bad-json'})[0] == 400
    assert request(api+'/users', admin, {**user, 'password':'forbidden'}, 'POST', {'Idempotency-Key':'bad-field'})[0] == 400
    assert request(api+'/users', admin, user, 'POST')[0] == 400
    headers = {'Idempotency-Key':'create-user', 'traceparent':'00-'+'1'*32+'-'+'2'*16+'-01'}
    status, body, response_headers = request(api+'/users', admin, user, 'POST', headers)
    assert status == 201, (status, body)
    assert response_headers['ETag'] == '"1"' and response_headers.get('X-Request-ID')
    assert request(api+'/users', admin, user, 'POST', headers)[:2] == (status, body)
    assert request(api+'/users', admin, {**user, 'name':'different'}, 'POST', headers)[0] == 409
    assert request(api+'/users/user', token(key, tenant_id='other'))[0] == 404
    assert request(api+'/users?limit=0', reader)[0] == 400
    fixtures = json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
    records = {
        'teams':{'id':'team','name':'Team','enabled':True,'revision':1,'userIds':['user']},
        'agents':{'id':'agent','name':'Agent','enabled':True,'revision':1,'ownerUserId':'user'},
        'devices':{'id':'device','name':'Device','enabled':True,'revision':1,'ownerUserId':'user'},
        'tools':{'id':fixtures['ToolDefinition']['id'],'name':'Tool','enabled':True,'revision':1,'definition':fixtures['ToolDefinition']},
        'policies':{**fixtures['ControlPolicy'],'id':'policy','userIds':['user'],'teamIds':['team'],'agentIds':['agent'],'deviceIds':['device']}}
    for kind,record in records.items():
        status, body, _ = request(api+'/'+kind, admin, record, 'POST', {'Idempotency-Key':'create-'+kind})
        assert status == 201, (kind,status,body)
        assert request(api+'/'+kind+'/'+record['id'],reader)[0] == 200
        assert request(api+'/'+kind,reader)[0] == 200
    assert request(api+'/policies',admin,{**records['policies'],'id':'ask-policy','decision':'ASK'},'POST',{'Idempotency-Key':'ask-policy'})[0] == 400
    assert request(api+'/users/user',admin,method='DELETE',headers={'If-Match':'"1"','Idempotency-Key':'referenced-delete'})[0] == 409
    assert request(api+'/users/user',admin,{**user,'revision':1},'PUT',{'If-Match':'"0"','Idempotency-Key':'stale-update'})[0] == 409
    assert request(api+'/users',admin,{**user,'id':'page-user'},'POST',{'Idempotency-Key':'page-user'})[0] == 201
    complex_id='namespace:user/item'
    assert request(api+'/users',admin,{**user,'id':complex_id},'POST',{'Idempotency-Key':'complex-id'})[0] == 201
    assert request(api+'/users/'+urllib.parse.quote(complex_id,safe=''),reader)[0] == 200
    status,page,_=request(api+'/users?limit=1',reader);page=json.loads(page)
    assert status == 200 and len(page['items']) == 1 and page['nextCursor']
    assert len(json.loads(request(api+'/users?limit=1&cursor='+page['nextCursor'],reader)[1])['items']) == 1
    assert request(api+'/users?cursor='+page['nextCursor'],token(key,tenant_id='other'))[0] == 400
    status, snapshot, headers = request(api+'/config/export', reader)
    assert status == 200
    snapshot = json.loads(snapshot); rev = snapshot['revision']
    imp = {'snapshot':snapshot, 'mode':'REPLACE', 'dryRun':True}
    status, diff, _ = request(api+'/config/import', admin, imp, 'POST', {'If-Match':f'"{rev}"','Idempotency-Key':'dry-run'})
    assert status == 200 and json.loads(diff)['changes'] == [], (status, diff)
    status, yaml_body, _ = request(api+'/config/export', admin, headers={'Accept':'application/yaml'})
    assert status == 200
    import yaml
    imp['snapshot'] = yaml.safe_load(yaml_body)
    status, diff, _ = request(api+'/config/import', admin, yaml.safe_dump(imp).encode(), 'POST', {'Content-Type':'application/yaml','If-Match':f'"{rev}"','Idempotency-Key':'dry-yaml'})
    assert status == 200 and not json.loads(diff)['applied'], (status, diff)
    assert request(api+'/config/import', admin, b'!!java/object {}', 'POST', {'Content-Type':'application/yaml','If-Match':f'"{rev}"','Idempotency-Key':'unsafe-yaml'})[0] == 400
    status, audits, _ = request(api+'/audit', admin)
    assert status == 200 and len(json.loads(audits)['items']) == 8, audits
    imp['snapshot']['users'][0]['name']='Imported user'
    status,diff,_=request(api+'/config/import',admin,imp,'POST',{'If-Match':f'"{rev}"','Idempotency-Key':'changed-dry-run'})
    assert status == 200 and len(json.loads(diff)['changes']) == 1
    imp['dryRun']=False
    import_headers={'If-Match':f'"{rev}"','Idempotency-Key':'apply-import'}
    applied=request(api+'/config/import',admin,imp,'POST',import_headers)
    assert applied[0] == 200 and json.loads(applied[1])['revision'] == rev+1
    assert request(api+'/config/import',admin,imp,'POST',import_headers)[:2] == applied[:2]
    assert len(json.loads(request(api+'/audit',admin)[1])['items']) == 9
    record_path=api+'/users/'+urllib.parse.quote(complex_id,safe='')
    prior=json.loads(request(record_path,reader)[1]);revision=prior['revision']
    updated={**prior,'name':'Updated'}
    update_headers={'If-Match':f'"{revision}"','Idempotency-Key':'update-complex'}
    assert request(record_path,reader,updated,'PUT',update_headers)[0] == 403
    updated_reply=request(record_path,admin,updated,'PUT',update_headers)
    assert updated_reply[0] == 200 and json.loads(updated_reply[1])['revision'] == revision+1, updated_reply[:2]
    assert request(record_path,admin,updated,'PUT',update_headers)[:2] == updated_reply[:2]
    delete_headers={'If-Match':f'"{revision+1}"','Idempotency-Key':'delete-complex'}
    assert request(record_path,reader,method='DELETE',headers=delete_headers)[0] == 403
    assert request(record_path,admin,method='DELETE',headers=delete_headers)[0] == 204
    assert request(record_path,admin,method='DELETE',headers=delete_headers)[0] == 204
    assert request(record_path,reader)[0] == 404
    assert request(api+'/users',admin,{**user,'id':complex_id},'POST',{'Idempotency-Key':'retired-complex'})[0] == 409
    assert len(json.loads(request(api+'/audit',admin)[1])['items']) == 11
    assert request(api+'/audit', reader)[0] == 403
    assert request(management+'/q/health/live')[0] == 200
    status, metrics, _ = request(management+'/q/metrics')
    assert status == 200 and b'toolgate_control_operations_total' in metrics
    assert request(runtime+'/q/health/ready', admin)[0] == 404
    status, spec, _ = request(api+'/openapi', reader)
    assert status == 200 and json.loads(spec)['info']['version'] == (ROOT/'VERSION').read_text().strip()
    assert request(api+'/openapi', token(key, tenant_id=None))[0] == 401
    assert request(api+'/openapi', token(key, iat=int(time.time())+300))[0] == 401
    # Run connection-closing transport rejection last: kubectl's port-forward
    # terminates its tunnel on the backend's early close while copying this body.
    # Direct container/host smoke separately proves subsequent requests still work.
    assert request(api+'/users', admin, b'x'*(2*1024*1024+1), 'POST', {'Idempotency-Key':'oversize'})[0] == 413
    print('Control HTTP: verified JWT negatives, roles, tenant isolation, contracts, limits, idempotency, safe JSON/YAML import, audit and telemetry passed')
    return admin


def static_tests(runtime):
    """Proves embedded artifacts and browser headers in host/container/cluster modes."""
    import re
    status,html,headers=request(runtime+'/console/')
    assert status==200 and b'ToolGate' in html
    assert headers.get('Cache-Control')=='no-store'
    assert "script-src 'self'" in headers.get('Content-Security-Policy','')
    assert headers.get('X-Content-Type-Options')=='nosniff'
    asset=re.search(rb'src="([^"]+\.js)"',html).group(1).decode()
    status,content,headers=request(runtime+asset)
    assert status==200 and len(content)>1000 and 'immutable' in headers.get('Cache-Control','')
    status,metadata,_=request(runtime+'/console/release.json')
    assert status==200 and json.loads(metadata)['version']==(ROOT/'VERSION').read_text().strip()
    assert request(runtime+'/console/.vite/manifest.json')[0]==404
    assert request(runtime+'/console/THIRD-PARTY-NOTICES.txt')[0]==200


def smoke(db):
    root = ROOT/'.dev'; root.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='control-http-',dir=root) as temp, collector() as (trace_server, spans):
        work = Path(temp); key, public = keypair(work); env = environment(db, public)
        api_port, management_port = free_port(), free_port()
        env.update(QUARKUS_HTTP_HOST='127.0.0.1',QUARKUS_HTTP_PORT=str(api_port),QUARKUS_MANAGEMENT_HOST='127.0.0.1',QUARKUS_MANAGEMENT_PORT=str(management_port))
        env.update(TOOLGATE_CONTROL_TRACE_EXPORT_ENABLED='true',QUARKUS_OTEL_EXPORTER_OTLP_TRACES_PROTOCOL='http/protobuf',
                   QUARKUS_OTEL_EXPORTER_OTLP_TRACES_ENDPOINT=f'http://127.0.0.1:{trace_server.server_port}',QUARKUS_OTEL_BSP_SCHEDULE_DELAY='100ms')
        log_path = ROOT/'build/control/http-private.log'; log_path.parent.mkdir(parents=True,exist_ok=True)
        with log_path.open('w',encoding='utf-8') as log:
            process = subprocess.Popen(['java','-jar','apps/control-plane/build/quarkus-app/quarkus-run.jar'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
            try:
                management = f'http://127.0.0.1:{management_port}'; runtime = f'http://127.0.0.1:{api_port}'
                ready(management,process); credential = http_tests(runtime,management,key)
                run(['docker','pause',db['container']],capture_output=True)
                try:
                    assert request(management+'/q/health/ready')[0] == 503
                    status,body,_=request(runtime+'/api/control/v1/users',credential)
                    assert status == 503 and json.loads(body)['code']=='DEPENDENCY_UNAVAILABLE'
                finally: run(['docker','unpause',db['container']],capture_output=True)
                ready(management,process)
            finally:
                process.terminate(); process.wait(timeout=30)
        for invalid in ({'TOOLGATE_CONTROL_MAX_RECORDS':'0'},{'TOOLGATE_CONTROL_MAX_CONFIG_BYTES':'1.048576e+06'}):
            with (work/'invalid-startup.log').open('w',encoding='utf-8') as log:
                rejected=subprocess.run(['java','-jar','apps/control-plane/build/quarkus-app/quarkus-run.jar'],cwd=ROOT,env={**env,**invalid},stdout=log,stderr=subprocess.STDOUT,timeout=60)
            assert rejected.returncode != 0, 'Invalid limits must fail startup, before readiness or traffic'
        logs = log_path.read_text(encoding='utf-8')
        assert credential not in logs and db['CONTROL_TEST_PASSWORD'] not in logs and 'secret-redaction-sentinel' not in logs
        assert 'trace_id='+'1'*32 in logs, 'Incoming W3C trace context was not propagated'
        payload=b''.join(spans)
        assert b'control.request' in payload and bytes.fromhex('1'*32) in payload, 'Real OTLP export and incoming trace context required'
        for forbidden in (b'url.full',b'url.path',b'http.target',b'http.route',b'secret-redaction-sentinel',credential.encode(),db['CONTROL_TEST_PASSWORD'].encode()):
            assert forbidden not in payload, 'Sensitive/raw request data crossed OTLP export boundary'
        report = {'postgres':True,'jwtAuth':True,'roles':True,'tenantIsolation':True,'durableIdempotency':True,'jsonYaml':True,'audit':True,'healthMetricsTracing':True,'logsRedacted':True,'realOtlpRedacted':True}
        (ROOT/'build/control/http-smoke.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('--http-only',action='store_true'); args = parser.parse_args()
    with database() as db:
        env = dict(os.environ, CONTROL_TEST_URL=db['CONTROL_TEST_URL'],CONTROL_TEST_PASSWORD=db['CONTROL_TEST_PASSWORD'])
        if not args.http_only:
            wrapper = 'gradlew.bat' if os.name == 'nt' else './gradlew'
            run([str(ROOT/wrapper),'--no-daemon',':control-plane:build'],env=env)
        smoke(db)


@contextlib.contextmanager
def collector():
    """Test-only receiver captures real OTLP protobuf requests from the production SDK/exporter."""
    spans=[]
    class Receiver(http.server.BaseHTTPRequestHandler):
        def do_POST(self):
            spans.append(self.rfile.read(int(self.headers['Content-Length'])))
            self.send_response(200);self.send_header('Content-Type','application/x-protobuf');self.send_header('Content-Length','0');self.end_headers()
        def log_message(self,*args): pass
    server=http.server.ThreadingHTTPServer(('127.0.0.1',0),Receiver)
    thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
    try: yield server,spans
    finally: server.shutdown();thread.join(timeout=5);server.server_close()


if __name__ == '__main__': main()
