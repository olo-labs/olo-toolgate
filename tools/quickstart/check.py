# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real one-image SQLite, Gateway, ASK, direct TLS enrollment and offline recovery gate."""
import argparse
import base64
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import secrets
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools/control'))
from check import request


def run(args, **kwargs):
    if os.name=='nt' and args[0]=='npm':args=['npm.cmd',*args[1:]]
    return subprocess.run(args,cwd=ROOT,check=True,**kwargs)


def ready(container, port):
    deadline=time.monotonic()+100
    while time.monotonic()<deadline:
        try:
            if request(f'http://127.0.0.1:{port}/health/ready')[0]==200:return
        except Exception:pass
        state=json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]['State']
        if not state['Running']:
            # Production logs are redacted by design; print them only on a real startup failure.
            logs=run(['docker','logs',container],capture_output=True,text=True)
            output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True);(output/'startup-failure.log').write_text(logs.stdout+logs.stderr,encoding='utf-8')
            print('Startup diagnostics: build/quickstart/startup-failure.log')
            raise RuntimeError('Quickstart exited before readiness')
        time.sleep(.25)
    logs=run(['docker','logs',container],capture_output=True,text=True)
    output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True);(output/'startup-failure.log').write_text(logs.stdout+logs.stderr,encoding='utf-8')
    raise RuntimeError('Quickstart readiness timeout; see build/quickstart/startup-failure.log')


def tls(url, context, token=None, body=None, method='GET'):
    headers={'Content-Type':'application/json','Idempotency-Key':secrets.token_hex(16)}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(url,json.dumps(body).encode() if body is not None else None,headers,method=method)
    try:
        with urllib.request.urlopen(req,context=context,timeout=15) as response:return response.status,json.loads(response.read())
    except urllib.error.HTTPError as response:return response.code,json.loads(response.read())


def smoke(image,browser):
    name='toolgate-quickstart-test-'+secrets.token_hex(6)
    volumes=[name+'-data',name+'-backup',name+'-restore'];containers=[]
    report={'image':image,'gates':[]}
    def mark(gate):report['gates'].append(gate);print('PASS '+gate,flush=True)
    def launch(volume):
        container=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--memory=1g','--cpus=2',
            '--tmpfs','/tmp:rw,noexec,nosuid,size=128m,uid=65532,gid=65532','-p','127.0.0.1::8080','-p','127.0.0.1::8443','-v',volume+':/data',image],capture_output=True,text=True).stdout.strip()
        containers.append(container)
        info=json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
        port=info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort'];tls_port=info['NetworkSettings']['Ports']['8443/tcp'][0]['HostPort']
        ready(container,port);return container,port,tls_port
    try:
        for volume in volumes:run(['docker','volume','create',volume],capture_output=True)
        started=time.monotonic();container,port,tls_port=launch(volumes[0]);report['firstBootSeconds']=round(time.monotonic()-started,2)
        origin=f'http://127.0.0.1:{port}'
        def api(path,token=None,body=None,method='GET'):
            status,raw,_=request(origin+path,token,body,method,{'Idempotency-Key':secrets.token_hex(16)} if body is not None else None);return status,json.loads(raw) if raw else None
        # A real schema-1 persistent volume upgrades through the production image.
        run(['docker','stop','-t','35',container],capture_output=True)
        upgrade="import sqlite3;d=sqlite3.connect('/data/state/control.sqlite');d.execute('DROP TABLE quickstart_vault');[d.execute('DROP INDEX '+n+'_local_cursor') for n in ('control_audit','control_approvals','control_permit_leases','control_enrollments','control_idempotency')];d.execute('DELETE FROM quickstart_migrations WHERE version=2');d.commit();d.close()"
        run(['docker','run','--rm','--entrypoint','/opt/quickstart-python/bin/python','-v',volumes[0]+':/data',image,'-c',upgrade],capture_output=True)
        container,port,tls_port=launch(volumes[0]);origin=f'http://127.0.0.1:{port}'
        mark('production image schema 1-to-2 upgrade/retained identity/defaults')
        assert api('/api/quickstart/v1/status')[1]['nonHa'] is True
        status,manifest=api('/api/public/v1/clients');assert status==200
        assert {a['platform'] for a in manifest['artifacts']}=={'WINDOWS','MACOS','LINUX'}
        for artifact in manifest['artifacts']:
            status,archive,headers=request(origin+'/api/public/v1/clients/'+artifact['filename'])
            if artifact['platform']=='WINDOWS':
                assert status==404
                continue
            assert status==200 and len(archive)==artifact['bytes'] and hashlib.sha256(archive).hexdigest()==artifact['sha256']
            assert headers['X-Content-Type-Options']=='nosniff'
        status,installers=api('/api/public/v1/installers');assert status==200
        assert {a['platform'] for a in installers['artifacts']}=={'WINDOWS','MACOS','LINUX'}
        for installer in installers['artifacts']:
            status,data,headers=request(origin+'/api/public/v1/clients/'+installer['filename'])
            assert status==200 and len(data)==installer['bytes'] and hashlib.sha256(data).hexdigest()==installer['sha256']
            assert headers['X-Content-Type-Options']=='nosniff'
        mark('three-platform anonymous installer checksums/Windows ZIP rejected')
        assert api('/api/control/v1/users')[0]==401
        assert api('/api/quickstart/v1/vault')[0]==401
        assert request(origin+'/api/quickstart/v1/status',headers={'Host':'hostile.invalid'})[0]==401
        assert request(origin+'/api/quickstart/v1/login',body={'password':'wrong'},method='POST',headers={'Origin':'https://hostile.invalid'})[0]==401
        bootstrap=run(['docker','exec',container,'cat','/data/bootstrap-password'],capture_output=True,text=True).stdout.strip()
        password=secrets.token_urlsafe(32)
        assert api('/api/quickstart/v1/login',body={'password':bootstrap},method='POST')[0]==400
        assert api('/api/quickstart/v1/login',body={'password':bootstrap,'newPassword':'short'},method='POST')[0]==400
        status,result=api('/api/quickstart/v1/login',body={'password':bootstrap,'newPassword':password},method='POST');assert status==200
        admin=result['accessToken'];assert api('/api/control/v1/users',admin)[0]==200
        status,managed=api('/api/control/v1/endpoint/devices',admin);assert status==200,(status,managed)
        systems={item['deviceId']:item for item in managed['items'] if item['systemExecutor']}
        assert set(systems)=={'local-builtins','local-hotfolder','local-rest-forwarding'}
        assert all(item['systemAvailable'] and item['registeredUser']['id']=='local-tools' for item in systems.values())
        mark('clean boot/bootstrap/password/host-origin/auth')
        def invoke(tool,args):return api('/api/quickstart/v1/invoke',admin,{'toolId':tool,'arguments':args},'POST')
        status,result=invoke('calculator.evaluate',{'expression':'2+3*4'});assert status==200,(status,result);assert result['result']['value']==14
        assert invoke('hotfolder.read_text',{'path':'../identity.json'})[0]==400
        assert invoke('hotfolder.read_text',{'path':'other.txt'})[0]==403
        assert invoke('hotfolder.delete',{'path':'welcome.txt'})[0] in (400,501)
        status,result=invoke('hotfolder.read_text',{'path':'welcome.txt'});assert status==200,(status,result)
        def enable(device,revision,value):
            result=api('/api/control/v1/endpoint/devices/'+device+'/enabled',admin,{'expectedRevision':revision,'enabled':value},'POST')
            assert result[0]==200,result
            return result[1]['revision']
        revision=enable('local-hotfolder',1,False)
        assert invoke('hotfolder.read_text',{'path':'welcome.txt'})[0]==403
        assert invoke('calculator.evaluate',{'expression':'2+3*4'})[0]==200
        enable('local-hotfolder',revision,True)
        assert invoke('hotfolder.read_text',{'path':'welcome.txt'})[0]==200
        revision=enable('local-builtins',1,False)
        assert invoke('calculator.evaluate',{'expression':'2+3*4'})[0]==403
        enable('local-builtins',revision,True)
        # A separate registry row must also be the policy identity, not just a display label.
        status,policy=api('/api/control/v1/policies/default-hotfolder.read_text',admin);assert status==200
        def scope_hotfolder(ids):
            nonlocal policy
            path='/api/control/v1/policies/'+policy['id']
            updated={**policy,'deviceIds':ids}
            status,raw,_=request(origin+path,admin,updated,'PUT',{'If-Match':'"'+str(policy['revision'])+'"','Idempotency-Key':secrets.token_hex(16)})
            assert status==200,(status,raw);policy=json.loads(raw)
            revision=api('/api/control/v1/config/export',admin)[1]['revision']
            wire=api('/api/control/v1/bundles/current',admin)[1]['jws'].split('.')[1]
            sequence=json.loads(base64.urlsafe_b64decode(wire+'='*(-len(wire)%4)))['sequence']
            assert api('/api/control/v1/bundles/publish',admin,{'directoryRevision':revision,'expectedSequence':sequence,'lifetimeMs':86400000,'graceMs':0},'POST')[0]==201
        def wait_hotfolder(expected):
            deadline=time.monotonic()+10
            while True:
                status,result=invoke('hotfolder.read_text',{'path':'welcome.txt'})
                if status==expected:return
                if time.monotonic()>=deadline:raise AssertionError((status,result,expected))
                time.sleep(.2)
        scope_hotfolder(['local-builtins']);wait_hotfolder(403)
        scope_hotfolder(['local-hotfolder']);wait_hotfolder(200)
        scope_hotfolder([]);wait_hotfolder(200)
        mark('registered HotFolder identity/device filters/independent disable controls')
        write={'path':'welcome.txt','text':'Approved persistent note\n'}
        deadline=time.monotonic()+10
        while True:
            status,pending=invoke('hotfolder.write_text',write)
            if status==202:break
            if time.monotonic()>=deadline:raise AssertionError((status,pending))
            time.sleep(.2)  # ASK must use the exact current bundle after the scope publications.
        assert pending['decision']=='ASK'
        status,queue=api('/api/control/v1/approvals',admin);assert status==200,(status,queue)
        approval=next(item for item in queue['items'] if item['id']==pending['approvalId'])
        assert approval['input']['context']['deviceId']=='local-hotfolder'
        status,result=api('/api/control/v1/approvals/'+approval['id']+'/decision',admin,{'decision':'APPROVE_ONCE','expectedRevision':approval['revision']},'POST');assert status==200,(status,result)
        status,result=invoke('hotfolder.write_text',write);assert status==200,(status,result)
        status,result=invoke('hotfolder.write_text',write);assert status!=200,(status,result)
        assert run(['docker','exec',container,'cat','/data/hotfolder/welcome.txt'],capture_output=True,text=True).stdout==write['text']
        mark('real Gateway/built-in/ASK/single-use/traversal/default-deny')
        vault_value=secrets.token_urlsafe(48)
        assert api('/api/quickstart/v1/vault',admin,{'name':'demo/token','value':vault_value},'POST')[0]==201
        assert api('/api/quickstart/v1/vault',admin)[1]=={'names':['demo/token']}
        # Verify persistent bytes contain ciphertext, not plaintext, and the protected key recovers it.
        script="import sqlite3,pathlib;from cryptography.hazmat.primitives.ciphers.aead import AESGCM;c=sqlite3.connect('/data/state/control.sqlite').execute('select cipher from quickstart_vault').fetchone()[0];p=AESGCM(pathlib.Path('/data/keys/vault.key').read_bytes()).decrypt(c[:12],c[12:],b'demo/token');print(__import__('hashlib').sha256(p).hexdigest())"
        actual=run(['docker','exec',container,'/opt/quickstart-python/bin/python','-c',script],capture_output=True,text=True).stdout.strip()
        assert actual==hashlib.sha256(vault_value.encode()).hexdigest()
        mark('encrypted vault/audited ciphertext/no-value-export')
        with tempfile.TemporaryDirectory(prefix='toolgate-quickstart-tls-') as temp:
            temp=Path(temp);ca=temp/'ca.pem'
            ca.write_bytes(run(['docker','exec',container,'cat','/data/keys/device-ca.crt'],capture_output=True).stdout)
            context=ssl.create_default_context(cafile=str(ca));secure=f'https://127.0.0.1:{tls_port}'
            runtime_token=temp/'agent-token'
            runtime_token.write_bytes(run(['docker','exec',container,'cat','/data/run/runtime-token'],capture_output=True).stdout);runtime_token.chmod(0o600)
            spec=importlib.util.spec_from_file_location('quickstart_mimic',ROOT/'debug/agent-mimic.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
            agent=module.Agent(secure,runtime_token,ca)
            assert module.VERSION in agent.rpc('server/discover')['supportedVersions']
            assert agent.rpc('tools/list')['tools']==[]
            revision=enable('local-rest-forwarding',1,False)
            try:agent.rpc('tools/list')
            except ValueError as failure:assert str(failure).startswith('MCP HTTP 403:'),str(failure)
            else:raise AssertionError('Disabled forwarding unexpectedly returned tools')
            enable('local-rest-forwarding',revision,True)
            assert agent.rpc('tools/list')['tools']==[]
            mark('agent-facing TLS MCP/header forwarding/unenrolled catalog isolation')
            assert tls(secure+'/.well-known/olo-toolgate-client',context)[0]==200
            device_key=ec.generate_private_key(ec.SECP256R1())
            csr=x509.CertificateSigningRequestBuilder().subject_name(x509.Name([x509.NameAttribute(x509.NameOID.COMMON_NAME,'test-device')])).sign(device_key,hashes.SHA256())
            body={'deviceId':'quickstart-test-device','clientVersion':'0.10.0-dev','platform':'LINUX','csrPem':csr.public_bytes(serialization.Encoding.PEM).decode(),'capabilities':[]}
            status,challenge=tls(secure+'/api/control/v1/endpoint/enrollments',context,body=body,method='POST');assert status==200,(status,challenge)
            status,pending=api('/api/control/v1/endpoint/enrollments',admin);assert status==200 and pending['items'][0]['deviceId']==body['deviceId'],(status,pending)
            assert 'deviceCode' not in json.dumps(pending) and 'csrPem' not in json.dumps(pending)
            managed=api('/api/control/v1/endpoint/devices',admin)[1]
            assert next(item for item in managed['items'] if item['deviceId']==body['deviceId'])['enrollment']['state']=='PENDING'
            status,review=api('/api/control/v1/endpoint/enrollments/review?code='+challenge['userCode'],admin);assert status==200,(status,review)
            status,result=api('/api/control/v1/endpoint/enrollments/decision',admin,{'userCode':challenge['userCode'],'keyFingerprint':review['keyFingerprint'],'choice':'APPROVE'},'POST');assert status==200,(status,result)
            assert 0 < result['connectionExpiresAtUnixMs']-int(time.time()*1000) <= 86400000
            assert api('/api/control/v1/endpoint/enrollments',admin)[1]['items']==[]
            status,result=tls(secure+'/api/control/v1/endpoint/enrollments/poll',context,body={'enrollmentId':challenge['enrollmentId'],'deviceCode':challenge['deviceCode']},method='POST');assert status==200 and result['state']=='CONSUMED',(status,result)
            identity=result['identity'];certificate=temp/'device.pem';private=temp/'device-key.pem'
            certificate.write_text(identity['certificatePem'],encoding='utf-8');private.write_bytes(device_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
            context.load_cert_chain(str(certificate),str(private))
            report_body={'sequence':1,'report':{'deviceId':body['deviceId'],'clientVersion':body['clientVersion'],'appliedRevision':0,'packages':[]}}
            status,result=tls(secure+'/api/control/v1/endpoint/check-in',context,body=report_body,method='POST');assert status==200,(status,result)
            revision=enable(body['deviceId'],1,False)
            assert tls(secure+'/api/control/v1/endpoint/check-in',context,body=report_body,method='POST')[0]==423
            enable(body['deviceId'],revision,True)
            assert api('/api/control/v1/endpoint/devices/'+body['deviceId']+'/approval',admin,{'expectedApprovalRevision':1,'approved':False},'POST')[0]==200
            assert tls(secure+'/api/control/v1/endpoint/check-in',context,body=report_body,method='POST')[0]==423
            assert api('/api/control/v1/endpoint/devices/'+body['deviceId']+'/approval',admin,{'expectedApprovalRevision':2,'approved':True,'unlimitedConnection':True},'POST')[0]==200
            time.sleep(.5);report_body['sequence']=2
            assert tls(secure+'/api/control/v1/endpoint/check-in',context,body=report_body,method='POST')[0]==200
            row=next(item for item in api('/api/control/v1/endpoint/devices',admin)[1]['items'] if item['deviceId']==body['deviceId'])
            assert row['registeredUser']['id']=='admin' and row['endpointDevice']['connectionApproved'] is True and 'connectionExpiresAtUnixMs' not in row['endpointDevice']
        mark('real CSR/browser enrollment/direct mTLS check-in')
        if browser:
            env=dict(os.environ,UI_TEST_ORIGIN=origin,QUICKSTART_PASSWORD=password)
            run(['npm','--workspace','@olo-labs/toolgate-admin-ui','run','e2e','--','quickstart.spec.ts'],env=env)
            mark('browser login/tools/vault/accessibility')
        run(['docker','restart','-t','35',container],capture_output=True)
        info=json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
        port=info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort'];origin=f'http://127.0.0.1:{port}'
        ready(container,port)
        admin=api('/api/quickstart/v1/login',body={'password':password},method='POST')[1]['accessToken']
        assert api('/api/quickstart/v1/vault',admin)[1]['names']==['demo/token']
        assert run(['docker','exec',container,'cat','/data/hotfolder/welcome.txt'],capture_output=True,text=True).stdout==write['text']
        mark('restart/persisted password/vault/HotFolder/directory/keys')
        run(['docker','stop','-t','35',container],capture_output=True)
        # Backup directory initialization is test-only volume provisioning, not root service execution.
        run(['docker','run','--rm','--user','0','--entrypoint','sh','-v',volumes[1]+':/backup',image,'-c','chown 65532:65532 /backup && chmod 700 /backup'],capture_output=True)
        run(['docker','run','--rm','--entrypoint','/opt/quickstart-python/bin/python','-v',volumes[0]+':/data','-v',volumes[1]+':/backup',image,'/opt/quickstart/supervisor.py','--backup','/backup/snapshot'],capture_output=True)
        run(['docker','run','--rm','--entrypoint','/opt/quickstart-python/bin/python','-v',volumes[2]+':/data','-v',volumes[1]+':/backup:ro',image,'/opt/quickstart/supervisor.py','--restore','/backup/snapshot'],capture_output=True)
        restored,restore_port,_=launch(volumes[2]);origin=f'http://127.0.0.1:{restore_port}'
        admin=api('/api/quickstart/v1/login',body={'password':password},method='POST')[1]['accessToken']
        assert api('/api/quickstart/v1/vault',admin)[1]['names']==['demo/token']
        assert run(['docker','exec',restored,'cat','/data/hotfolder/welcome.txt'],capture_output=True,text=True).stdout==write['text']
        mark('offline consistent backup/checksummed restore/new volume')
        script="import urllib.request;[print(len(urllib.request.urlopen('http://127.0.0.1:'+p+path).read())) for p,path in [('9092','/q/metrics'),('9091','/v1/metrics')]]"
        metrics=run(['docker','exec',restored,'/opt/quickstart-python/bin/python','-c',script],capture_output=True,text=True).stdout.splitlines()
        assert len(metrics)==2 and all(int(n)>0 for n in metrics)
        mark('private real Control/Gateway metrics')
        script="""import pathlib,os,signal
for p in pathlib.Path('/proc').iterdir():
    if not p.name.isdigit():continue
    try:command=(p/'cmdline').read_bytes().split(bytes([0]))[0]
    except (FileNotFoundError,ProcessLookupError):continue
    if command==b'/usr/local/bin/olo-toolgate-gateway':os.kill(int(p.name),signal.SIGTERM);break
else:raise RuntimeError('Expected real Gateway child')
"""
        run(['docker','exec',restored,'/opt/quickstart-python/bin/python','-c',script],capture_output=True)
        deadline=time.monotonic()+40
        while time.monotonic()<deadline:
            state=json.loads(run(['docker','inspect',restored],capture_output=True,text=True).stdout)[0]['State']
            if not state['Running']:break
            time.sleep(.25)
        assert not state['Running'] and state['ExitCode']!=0
        mark('Gateway child outage removes readiness/stops composition/fail closed')
        logs=run(['docker','logs',container],capture_output=True,text=True)
        for secret in (bootstrap,password,vault_value,admin):assert secret not in logs.stdout+logs.stderr
        mark('redacted logs/non-root/read-only-root/graceful shutdown')
        output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True)
        (output/'smoke.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
        print('One-image real SQLite/Gateway/ASK/enrollment/persistence/backup gates passed')
    finally:
        for container in reversed(containers):subprocess.run(['docker','rm','-f',container],capture_output=True)
        for volume in volumes:subprocess.run(['docker','volume','rm',volume],capture_output=True)


def main():
    parser=argparse.ArgumentParser();parser.add_argument('--image',default='olo-toolgate-quickstart:module11');parser.add_argument('--build',action='store_true');parser.add_argument('--no-browser',action='store_true');args=parser.parse_args()
    if args.build:
        version=(ROOT/'VERSION').read_text().strip()
        revision=run(['git','rev-parse','HEAD'],capture_output=True,text=True).stdout.strip()
        run(['docker','build','-f','apps/control-plane/Dockerfile','--build-arg','VERSION='+version,'--build-arg','REVISION='+revision,'--build-arg','QUARKUS_PROFILE=quickstart','--build-arg','CLIENT_ASSETS_DIR=deploy/client-assets/release','--build-arg','CLIENT_DOWNLOADS_DIRECTORY=/opt/toolgate/client-downloads','-t','olo-toolgate-control:module11','.'])
        run(['docker','build','-f','apps/quickstart/Dockerfile','--build-arg','VERSION='+version,'--build-arg','REVISION='+revision,'-t',args.image,'.'])
    smoke(args.image,not args.no_browser)


if __name__=='__main__':main()
