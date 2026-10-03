# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real direct-TLS Control enrollment/mTLS + protected service IPC + HTTPS Gateway/tool E2E.

Owns only disposable containers/volumes. No interactive login, production bypass,
fixed enrollment identity, fake executable, or mocked authorization is used.
"""
import argparse
import hashlib
import ipaddress
import json
import os
import re
import secrets
import ssl
import subprocess
import sys
import tempfile
import time
import urllib.request
import urllib.error
from datetime import datetime, timezone, timedelta
from pathlib import Path
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
sys.path.insert(0,str(Path(__file__).resolve().parents[1]))
from control.check import ROOT, database, environment, keypair, ready, run, token
from policy.check import until

def certificate(work,name,ca=False):
    key=rsa.generate_private_key(public_exponent=65537,key_size=3072);subject=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,name)])
    builder=x509.CertificateBuilder().subject_name(subject).issuer_name(subject).public_key(key.public_key()).serial_number(x509.random_serial_number())
    now=datetime.now(timezone.utc);builder=builder.not_valid_before(now-timedelta(minutes=1)).not_valid_after(now+timedelta(days=7))
    builder=builder.add_extension(x509.BasicConstraints(ca=ca,path_length=0 if ca else None),critical=True)
    builder=builder.add_extension(x509.KeyUsage(digital_signature=True,key_encipherment=not ca,key_cert_sign=ca,crl_sign=ca,content_commitment=False,data_encipherment=False,key_agreement=False,encipher_only=None,decipher_only=None),critical=True)
    if not ca:builder=builder.add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address('127.0.0.1'))]),critical=False)
    cert=builder.sign(key,hashes.SHA256())
    (work/(name+'.pem')).write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
    (work/(name+'.crt')).write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    return cert

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--control-image',default='olo-toolgate-control:module07');parser.add_argument('--gateway-image',default='olo-toolgate-gateway:module07');parser.add_argument('--binary',type=Path,default=ROOT/'target/client-release/x86_64-unknown-linux-gnu/release/olo-toolgate-client');args=parser.parse_args()
    with database() as db,tempfile.TemporaryDirectory(prefix='client-tools-e2e-',dir=ROOT/'.dev') as temp:
        work=Path(temp);identity,public=keypair(work);certificate(work,'device-ca',True);certificate(work,'server');certificate(work,'gateway-tls')
        password=secrets.token_hex(16)
        subprocess.run(['keytool','-importcert','-noprompt','-storetype','PKCS12','-keystore',str(work/'trust.p12'),'-storepass',password,'-alias','device','-file',str(work/'device-ca.crt')],check=True,capture_output=True)
        context=ssl.create_default_context(cafile=str(work/'server.crt'))
        api='https://127.0.0.1:'+db['runtimePort']+'/api/control/v1'
        def request(path,credential=None,body=None,method='GET',key=None):
            headers={'Content-Type':'application/json'}
            if credential:headers['Authorization']='Bearer '+credential
            if key:headers['Idempotency-Key']=key
            req=urllib.request.Request(api+path,data=json.dumps(body).encode() if body is not None else None,headers=headers,method=method)
            try:
                with urllib.request.urlopen(req,context=context,timeout=15) as reply:return reply.status,json.loads(reply.read())
            except urllib.error.HTTPError as failure:return failure.code,json.loads(failure.read())
        env=environment(db,Path('/config/jwt-public.pem'))
        env.update(QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://127.0.0.1:5432/control?sslmode=disable',QUARKUS_HTTP_INSECURE_REQUESTS='disabled',QUARKUS_HTTP_SSL_PORT='8082',QUARKUS_HTTP_SSL_CLIENT_AUTH='request',
            QUARKUS_HTTP_SSL_CERTIFICATE_FILES='/config/server.crt',QUARKUS_HTTP_SSL_CERTIFICATE_KEY_FILES='/config/server.pem',QUARKUS_HTTP_SSL_CERTIFICATE_TRUST_STORE_FILE='/config/trust.p12',QUARKUS_HTTP_SSL_CERTIFICATE_TRUST_STORE_PASSWORD=password,
            TOOLGATE_CONTROL_ENDPOINT_ENABLED='true',TOOLGATE_CONTROL_ENDPOINT_TENANT_ID='http-tenant',TOOLGATE_CONTROL_ENDPOINT_SERVER_ID='control-e2e',TOOLGATE_CONTROL_ENDPOINT_ORGANIZATION='Service smoke',
            TOOLGATE_CONTROL_ENDPOINT_CONTROL_URL='https://127.0.0.1:8082',TOOLGATE_CONTROL_ENDPOINT_GATEWAY_URL='https://127.0.0.1:8443',TOOLGATE_CONTROL_ENDPOINT_PRIVATE_KEY_PATH='/config/device-ca.pem',TOOLGATE_CONTROL_ENDPOINT_CA_CERTIFICATE_PATH='/config/device-ca.crt')
        (work/'control.env').write_text('\n'.join(k+'='+v for k,v in env.items() if k.startswith(('QUARKUS_','MP_JWT_','TOOLGATE_CONTROL_')))+'\n')
        for path in work.iterdir():path.chmod(0o444)
        containers=[];volume=None
        def start(command):
            container=run(command,capture_output=True,text=True).stdout.strip();containers.append(container);return container
        def execute(container,arguments,user=None,check=True):
            command=['docker','exec',*(['--user',user] if user else []),container,*arguments]
            result=subprocess.run(command,check=False,capture_output=True,text=True,encoding='utf-8')
            if check and result.returncode:raise AssertionError('Client command failed: '+result.stdout+result.stderr)
            return result
        def healthy(container):
            result=execute(container,['/client','health'],check=False)
            if result.returncode:return False
            return json.loads(result.stdout)['ready']
        try:
            control=start(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+db['container'],'--tmpfs','/tmp:rw,noexec,nosuid,size=64m,uid=65532,gid=65532','--env-file',str(work/'control.env'),'-v',f'{work.as_posix()}:/config:ro',args.control_image])
            ready('http://127.0.0.1:'+db['managementPort'])
            until(lambda:request('/users')[0],401,'Control HTTPS authentication route')
            admin=token(identity);enroller=token(identity,user_id='alice',groups=['toolgate-enroller'])
            assert request('/users',admin,{'id':'alice','name':'Device owner','enabled':True,'revision':1},'POST','seed-owner')[0]==201
            runtime_token=secrets.token_urlsafe(48);(work/'gateway-token').write_text(runtime_token)
            config={'serverUrl':'https://127.0.0.1:8082','stateDirectory':'/state/private','ipcEndpoint':'/run/olo-toolgate/client.sock','authorizedPeers':['0'],'caCertificatePath':'/state/server.crt','requestTimeoutSeconds':5,
                'tools':{'hotfolder':{'root':'/state/private/hotfolder','maxFileBytes':65536,'maxEntries':128,'extensions':['txt','json']},'gatewayUrl':'https://127.0.0.1:8443','gatewayTokenPath':'/state/gateway-token','gatewayCaPath':'/state/gateway-tls.crt','deviceId':'pending-enrollment','webSearchTokenPath':None}}
            (work/'client.json').write_text(json.dumps(config))
            volume=run(['docker','volume','create','toolgate-client-e2e-'+secrets.token_hex(8)],capture_output=True,text=True).stdout.strip()
            boot='mkdir -p /run/olo-toolgate; if [ ! -f /state/client.json ]; then cp /input/client.json /input/server.crt /input/gateway-tls.crt /input/gateway-token /state/; chmod 600 /state/*; fi; exec /client service --config /state/client.json'
            client=start(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+db['container'],'--tmpfs','/run:rw,nosuid,size=8m','--mount','type=volume,source='+volume+',target=/state','-v',f'{work.as_posix()}:/input:ro','-v',f'{args.binary.resolve().as_posix()}:/client:ro','rust:1.94-bookworm','sh','-c',boot])
            until(lambda:execute(client,['/client','health'],check=False).returncode,0,'service IPC')
            health=json.loads(execute(client,['/client','health']).stdout);assert not health['ready']
            prompt=execute(client,['/client','enroll']).stdout
            code=re.search(r'code=([A-F0-9]{16})',prompt).group(1);fingerprint=re.search(r'fingerprint in your browser: ([a-f0-9]{64})',prompt).group(1)
            device='device-'+fingerprint[:32]
            status,review=request('/endpoint/enrollments/review?code='+code,enroller);assert status==200
            assert review['deviceId']==device and review['keyFingerprint']==fingerprint
            decision=request('/endpoint/enrollments/decision',enroller,{'userCode':code,'keyFingerprint':fingerprint,'choice':'APPROVE'},'POST','enroll-device')
            assert decision[0]==200,decision
            until(lambda:healthy(client),True,'real mTLS device check-in',timeout=45)
            config['tools']['deviceId']=device
            subprocess.run(['docker','exec','-i',client,'sh','-c','cat > /state/client.json; chmod 600 /state/client.json'],input=json.dumps(config),text=True,check=True,capture_output=True)
            run(['docker','restart',client],capture_output=True)
            until(lambda:healthy(client),True,'persistent enrolled service restart',timeout=45)
            credential=[{'tokenSha256':hashlib.sha256(runtime_token.encode()).hexdigest(),'tenantId':'http-tenant','userId':'alice','agentId':'agent-demo','deviceId':device,'expiresAtUnixMs':int(time.time()*1000)+600000}]
            (work/'credentials.json').write_text(json.dumps(credential))
            catalog=json.loads((ROOT/'packages/contracts/tools/builtins.json').read_text())['tools']
            gateway=json.loads((ROOT/'docs/examples/gateway-static.json').read_text());gateway.update(listen='0.0.0.0:8081',managementListen='0.0.0.0:9091',trustedTlsProxy=True)
            gateway['extractors']=[{'toolId':t['toolId'],'action':t['action'],'pointer':'/path','kind':'FILE' if t['toolId'].startswith('hotfolder.') else 'CUSTOM'} for t in catalog]
            gateway['policy']['version']=(ROOT/'VERSION').read_text().strip();gateway['policy']['expiresAtUnixMs']=int(time.time()*1000)+600000
            gateway['policy']['rules']=[{'tenantId':'http-tenant','userId':'alice','agentId':'agent-demo','deviceId':device,'toolId':t['toolId'],'action':t['action'],'resource':{'kind':'FILE' if t['toolId'].startswith('hotfolder.') else 'CUSTOM','locator':p},'effect':'ALLOW'} for t in catalog if t['toolId']!='web.search' for p in ('a.txt','b.txt','hotfolder')]
            (work/'gateway.json').write_text(json.dumps(gateway))
            nginx='events {} http { access_log off; error_log /dev/stderr warn; server { listen 8443 ssl; ssl_certificate /config/gateway-tls.crt; ssl_certificate_key /config/gateway-tls.pem; location / { proxy_pass http://127.0.0.1:8081; proxy_set_header X-Forwarded-Proto https; proxy_set_header X-Request-ID $http_x_request_id; } } }'
            (work/'nginx.conf').write_text(nginx)
            for path in work.iterdir():path.chmod(0o444)
            gw=start(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+db['container'],'-v',f'{work.as_posix()}:/config:ro','-e','TOOLGATE_GATEWAY_CONFIG=/config/gateway.json','-e','TOOLGATE_GATEWAY_CREDENTIALS=/config/credentials.json',args.gateway_image])
            until(lambda:urllib.request.urlopen('http://127.0.0.1:'+db['gatewayManagementPort']+'/v1/health/ready',timeout=2).status,200,'static Gateway')
            start(['docker','run','-d','--read-only','--network','container:'+db['container'],'--tmpfs','/var/cache/nginx:rw,size=8m','--tmpfs','/var/run:rw,size=1m','-v',f'{work.as_posix()}:/config:ro','nginx:1.28-alpine','nginx','-c','/config/nginx.conf','-g','daemon off;'])
            def tool(name,arguments,success=True):
                result=execute(client,['/client','tools',name,json.dumps(arguments)],check=False);body=json.loads(result.stdout)
                assert (result.returncode==0)==success,(name,result.returncode,body);return body
            until(lambda:execute(client,['/client','tools','system.info','{}'],check=False).returncode,0,'HTTPS Gateway grant')
            tool('hotfolder.write_text',{'path':'a.txt','text':'logged-out service'})
            assert tool('hotfolder.read_text',{'path':'a.txt'})['output']['text']=='logged-out service'
            tool('hotfolder.copy',{'path':'a.txt','destination':'b.txt'})
            tool('hotfolder.copy',{'path':'a.txt','destination':'c.txt'},False)
            tool('hotfolder.read_text',{'path':'../outside.txt'},False);tool('hotfolder.delete',{'path':'a.txt'},False)
            assert execute(client,['/client','tools','system.info','{}'],user='1000',check=False).returncode!=0
            run(['docker','pause',gw],capture_output=True)
            try:tool('hotfolder.write_text',{'path':'a.txt','text':'must not write'},False)
            finally:run(['docker','unpause',gw],capture_output=True)
            assert tool('hotfolder.read_text',{'path':'a.txt'})['output']['text']=='logged-out service'
            record=request('/endpoint/devices/'+device,admin)[1]
            revoked=request('/endpoint/devices/'+device+'/revoke',admin,{'expectedRevision':record['revision']},'POST','revoke-device')
            assert revoked[0]==200,revoked
            until(lambda:execute(client,['/client','check-in'],check=False).returncode,1,'revoked device check-in',timeout=25)
            tool('system.info',{},False)
            info=json.loads(run(['docker','inspect',client],capture_output=True,text=True).stdout)[0]
            assert not info['Config']['Tty'] and not info['Config']['OpenStdin']
            threads=int(re.search(r'Threads:\s+(\d+)',execute(client,['cat','/proc/1/status']).stdout).group(1))
            assert threads<=16,threads
            output=ROOT/'build/client';output.mkdir(parents=True,exist_ok=True)
            (output/'module07-integration.json').write_text(json.dumps({'realControlTls':True,'realDeviceEnrollmentMtls':True,'persistentRestart':True,'realGatewayHttps':True,'freshAuthorization':True,'sourceDestinationBinding':True,'unauthorizedOsPeerRejected':True,'gatewayOutageBlocked':True,'revokedDeviceBlocked':True,'noInteractiveSession':True},indent=2)+'\n')
            print('Real TLS enrollment, mTLS check-in, protected service IPC, HTTPS Gateway tools, outage and revocation passed')
        finally:
            for container in reversed(containers):
                logs=subprocess.run(['docker','logs',container],capture_output=True,text=True,encoding='utf-8')
                text=logs.stdout+logs.stderr
                for secret in [password,env['QUARKUS_DATASOURCE_PASSWORD'],locals().get('runtime_token',''),locals().get('admin',''),locals().get('enroller','')]:
                    if secret:text=text.replace(secret,'[redacted]')
                (ROOT/'.dev'/('module07-container-'+container[:12]+'.log')).write_text(text,encoding='utf-8')
                run(['docker','rm','-f',container],capture_output=True)
            if volume:run(['docker','volume','rm',volume],capture_output=True)

if __name__=='__main__':main()
