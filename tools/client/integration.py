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
from cryptography.hazmat.primitives.asymmetric import rsa, padding
import base64
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
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--control-image',default='olo-toolgate-control:module07');parser.add_argument('--gateway-image',default='olo-toolgate-gateway:module07');parser.add_argument('--binary',type=Path,default=ROOT/'target/client-release/x86_64-unknown-linux-gnu/release/olo-toolgate-client');parser.add_argument('--fleet',action='store_true');parser.add_argument('--builder',action='store_true');parser.add_argument('--runtime-image');parser.add_argument('--runtime-version');parser.add_argument('--docker-cli',type=Path);args=parser.parse_args();args.fleet=args.fleet or args.builder
    (ROOT/'.dev').mkdir(parents=True, exist_ok=True)
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
        if args.fleet:
            if not args.runtime_image: raise ValueError('Fleet requires a real reviewed runtime image')
            def b64(data): return base64.urlsafe_b64encode(data).rstrip(b'=').decode()
            release_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
            organization_key=rsa.generate_private_key(public_exponent=65537,key_size=2048)
            def jwk(key,kid):
                n=key.public_key().public_numbers().n
                return {'kid':kid,'n':b64(n.to_bytes((n.bit_length()+7)//8,'big')),'e':'AQAB'}
            fleet_settings={'releaseKeys':[jwk(release_key,'release')],'organizationKeys':[jwk(organization_key,'organization')]}
            (work/'fleet.pem').write_bytes(organization_key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
            (work/'release.json').write_text(json.dumps(fleet_settings['releaseKeys']))
            (work/'organization.json').write_text(json.dumps(fleet_settings['organizationKeys']))
            (work/'artifact-token').write_text('isolated-test-only')
            env.update(TOOLGATE_CONTROL_FLEET_ENABLED='true',TOOLGATE_CONTROL_FLEET_KEY_ID='organization',TOOLGATE_CONTROL_FLEET_PRIVATE_KEY_PATH='/config/fleet.pem',TOOLGATE_CONTROL_FLEET_RELEASE_KEYS_PATH='/config/release.json',TOOLGATE_CONTROL_FLEET_ORGANIZATION_KEYS_PATH='/config/organization.json',TOOLGATE_CONTROL_FLEET_ARTIFACT_ORIGIN='https://127.0.0.1:8444',TOOLGATE_CONTROL_FLEET_ARTIFACT_CA_PATH='/config/server.crt')
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
            if args.runtime_image:
                if not args.runtime_version or not args.docker_cli: raise ValueError('Runtime version and Linux Docker CLI required')
                config['execution']={'enginePath':'/state/docker-cli','engineEndpoint':'unix:///var/run/docker.sock','stateDirectory':'/state/private/runtimes','allowFirstUsePull':False,'pullTimeoutSeconds':30,
                    'runtimes':[{'id':'python-test','kind':'PYTHON','image':args.runtime_image,'version':args.runtime_version}],
                    'tools':[{'toolId':'local.echo','action':'execute','runtimeId':'python-test','entryPoint':'/opt/tool/tool.py',
                        'inputSchema':{'type':'object','additionalProperties':False,'properties':{'text':{'type':'string','maxLength':256},'mode':{'type':'string'}},'required':['text','mode']},
                        'outputSchema':{'type':'object','additionalProperties':False,'properties':{'text':{'type':'string'}},'required':['text']},
                        'limits':{'timeoutMs':3000,'memoryMiB':128,'maxInputBytes':4096,'maxOutputBytes':4096}}]}
            if args.fleet:
                package_runtime=config['execution']['runtimes'][0];package_tool=config['execution']['tools'][0]
                config['execution']['runtimes']=[];config['execution']['tools']=[];config['deployment']=fleet_settings
            (work/'client.json').write_text(json.dumps(config))
            volume=run(['docker','volume','create','toolgate-client-e2e-'+secrets.token_hex(8)],capture_output=True,text=True).stdout.strip()
            boot='mkdir -p /run/olo-toolgate; if [ ! -f /state/client.json ]; then cp /input/client.json /input/server.crt /input/gateway-tls.crt /input/gateway-token /state/; chmod 600 /state/*; fi; if [ -f /docker-cli ]; then cp /docker-cli /state/docker-cli; chmod 555 /state/docker-cli; fi; exec /client service --config /state/client.json'
            engine_mounts=['-v',f'{args.docker_cli.resolve().as_posix()}:/docker-cli:ro','-v','/var/run/docker.sock:/var/run/docker.sock'] if args.runtime_image else []
            client=start(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+db['container'],'--tmpfs','/run:rw,nosuid,size=8m','--mount','type=volume,source='+volume+',target=/state','-v',f'{work.as_posix()}:/input:ro','-v',f'{args.binary.resolve().as_posix()}:/client:ro',*engine_mounts,'ubuntu:24.04','sh','-c',boot])
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
            if args.runtime_image:
                gateway['extractors'].append({'toolId':'local.echo','action':'execute','pointer':'/path','kind':'CUSTOM'})
                gateway['policy']['rules'].append({'tenantId':'http-tenant','userId':'alice','agentId':'agent-demo','deviceId':device,'toolId':'local.echo','action':'execute','resource':{'kind':'CUSTOM','locator':'runtime/local.echo'},'effect':'ALLOW'})
            if args.builder:
                for tool_id in ['custom.echo','custom.ui']:
                    gateway['extractors'].append({'toolId':tool_id,'action':'execute','pointer':'/path','kind':'CUSTOM'})
                    gateway['policy']['rules'].append({'tenantId':'http-tenant','userId':'alice','agentId':'agent-demo','deviceId':device,'toolId':tool_id,'action':'execute','resource':{'kind':'CUSTOM','locator':'runtime/'+tool_id},'effect':'ALLOW'})
            (work/'gateway.json').write_text(json.dumps(gateway))
            nginx='events {} http { access_log off; error_log /dev/stderr warn; server { listen 8443 ssl; ssl_certificate /config/gateway-tls.crt; ssl_certificate_key /config/gateway-tls.pem; location / { proxy_pass http://127.0.0.1:8081; proxy_set_header X-Forwarded-Proto https; proxy_set_header X-Request-ID $http_x_request_id; } } }'
            (work/'nginx.conf').write_text(nginx)
            for path in work.iterdir():path.chmod(0o444)
            gw=start(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+db['container'],'-v',f'{work.as_posix()}:/config:ro','-e','TOOLGATE_GATEWAY_CONFIG=/config/gateway.json','-e','TOOLGATE_GATEWAY_CREDENTIALS=/config/credentials.json',args.gateway_image])
            until(lambda:urllib.request.urlopen('http://127.0.0.1:'+db['gatewayManagementPort']+'/v1/health/ready',timeout=2).status,200,'static Gateway')
            start(['docker','run','-d','--read-only','--network','container:'+db['container'],'--tmpfs','/var/cache/nginx:rw,size=8m','--tmpfs','/var/run:rw,size=1m','-v',f'{work.as_posix()}:/config:ro','nginx:1.28-alpine','nginx','-c','/config/nginx.conf','-g','daemon off;'])
            if args.fleet:
                (work/'artifacts').mkdir()
                artifact_nginx='events {} http { access_log off; error_log /dev/stderr warn; default_type application/json; server { listen 8444 ssl; ssl_certificate /config/server.crt; ssl_certificate_key /config/server.pem; root /config/artifacts; location / { try_files $uri =404; } } }'
                (work/'artifact-nginx.conf').write_text(artifact_nginx)
                start(['docker','run','-d','--read-only','--network','container:'+db['container'],'--tmpfs','/var/cache/nginx:rw,size=8m','--tmpfs','/var/run:rw,size=1m','-v',f'{work.as_posix()}:/config:ro','nginx:1.28-alpine','nginx','-c','/config/artifact-nginx.conf','-g','daemon off;'])
                def publish_package(version,mode='echo',corrupt=False,platform='LINUX',architecture='x86_64',runtime_version=None):
                    runtime=dict(package_runtime);runtime['version']=runtime_version or runtime['version']
                    doc={'formatVersion':1,'packageId':'fleet-echo','version':version,'platforms':[platform],'architectures':[architecture],'minimumClientVersion':'0.8.0','runtimes':[runtime],'tools':[package_tool],'selfTests':[{'toolId':'local.echo','arguments':{'text':'health','mode':mode},'expectedOutput':{'text':'health'}}]}
                    raw=json.dumps(doc,separators=(',',':')).encode();digest=hashlib.sha256(raw).hexdigest()
                    header=b64(json.dumps({'alg':'RS256','typ':'toolgate-package-release+jws','kid':'release'},separators=(',',':')).encode());message=header+'.'+b64(raw)
                    signed={'jws':message+'.'+b64(release_key.sign(message.encode(),padding.PKCS1v15(),hashes.SHA256()))}
                    release={'packageId':'fleet-echo','version':version,'manifestDigest':digest,'sizeBytes':len(raw),'release':signed}
                    (work/'artifacts'/(digest+'.json')).write_bytes(b'corrupted' if corrupt else raw)
                    response=request('/fleet/releases',admin,release,'POST','publish-'+version);assert response[0]==200,response
                    return release
                def assign_package(version,present=True):
                    response=request('/fleet/rollouts',admin,{'id':'rollout-'+secrets.token_hex(8),'packageId':'fleet-echo','version':version,'deviceIds':[device],'desiredPresence':present,'percentage':100},'POST','assign-'+secrets.token_hex(8));assert response[0]==200,response
                def reported(version,state):
                    execute(client,['/client','check-in'],check=False)
                    record=request('/endpoint/devices/'+device,admin)[1];report=record.get('report') or {}
                    return any(p['version']==version and p['state']==state for p in report.get('packages',[]))
                publish_package('1.0.0');assign_package('1.0.0')
                until(lambda:reported('1.0.0','READY'),True,'signed package atomic activation',timeout=90)
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
            def managed(success=True, user=None):
                result=execute(client,['/client','run','local.echo',json.dumps({'text':'safe; $(touch /escape)','mode':'echo'})],user=user,check=False)
                assert (result.returncode==0)==success,(result.returncode,result.stdout,result.stderr)
                if success: assert json.loads(result.stdout)['result']['output']['text']=='safe; $(touch /escape)'
            if args.runtime_image:
                managed();managed(False,'1000')
            if args.fleet:
                interrupted=publish_package('1.0.4');artifact_path=work/'artifacts'/(interrupted['manifestDigest']+'.json')
                artifact_bytes=artifact_path.read_bytes();artifact_path.unlink();assign_package('1.0.4')
                until(lambda:reported('1.0.4','FAILED'),True,'interrupted/unavailable descriptor transfer',timeout=90);managed(False)
                active=execute(client,['cat','/state/private/fleet-active.json']).stdout;assert '1.0.0' in active and '1.0.4' not in active
                run(['docker','restart',client],capture_output=True);artifact_path.write_bytes(artifact_bytes)
                until(lambda:reported('1.0.4','READY'),True,'durable retry after interrupted transfer/service restart',timeout=90);managed()
                publish_package('1.0.1',corrupt=True);assign_package('1.0.1')
                until(lambda:reported('1.0.1','FAILED'),True,'hash mismatch blocks update',timeout=90);managed(False)
                assign_package('1.0.0');until(lambda:reported('1.0.0','READY'),True,'higher-generation rollback',timeout=90);managed()
                assign_package('1.0.0',False);until(lambda:reported('1.0.0','ABSENT'),True,'uninstall',timeout=90);managed(False)
                publish_package('1.0.2',platform='MACOS');assign_package('1.0.2');until(lambda:reported('1.0.2','FAILED'),True,'incompatible platform',timeout=90);managed(False)
                publish_package('1.0.5',architecture='aarch64');assign_package('1.0.5');until(lambda:reported('1.0.5','FAILED'),True,'incompatible architecture',timeout=90);managed(False)
                publish_package('1.0.6',runtime_version='0.0.1');assign_package('1.0.6');until(lambda:reported('1.0.6','FAILED'),True,'incompatible runtime version',timeout=90);managed(False)
                publish_package('1.0.3',mode='timeout');assign_package('1.0.3');until(lambda:reported('1.0.3','FAILED'),True,'health probe timeout blocks activation',timeout=90);managed(False)
                assign_package('1.0.0');until(lambda:reported('1.0.0','READY'),True,'recovery after failed staging',timeout=90);managed()
                run(['docker','restart',client],capture_output=True);until(lambda:healthy(client),True,'signed intent recovery after service restart',timeout=90);managed()
                status=request('/fleet/rollouts',admin)[1];assert any(s['ready']==1 for s in status['items']);assert all(sum(s[k] for k in ['ready','failed','offline','waiting','pending','superseded'])==1 for s in status['items'])
                credentials=work/'fleet-browser-credentials.json';credentials.write_text(json.dumps({'admin':admin}));credentials.chmod(0o600)
                browser_env=dict(os.environ,UI_TEST_ORIGIN=api.removesuffix('/api/control/v1'),UI_TEST_CREDENTIALS=str(credentials.resolve()),UI_TEST_FLEET='1',UI_TEST_FLEET_DEVICE=device)
                subprocess.run(['npx.cmd' if os.name=='nt' else 'npx','--no-install','playwright','test','tests/e2e/fleet.spec.ts'],cwd=ROOT/'apps/admin-ui',env=browser_env,check=True)
                until(lambda:reported('1.0.0','READY'),True,'browser-created assignment reconciled',timeout=90)


            builder_invoke=None
            if args.builder:
                from builder.integration import check as builder_check
                # Real test issuer refresh: the expanded flow can exceed the original five-minute JWT.
                admin=token(identity);enroller=token(identity,user_id='alice',groups=['toolgate-enroller'])
                builder_invoke=builder_check(ROOT,work,request,execute,client,admin,enroller,device,package_runtime,release_key,until,api)
            if args.fleet:
                run(['docker','pause',control],capture_output=True)
                try:
                    until(lambda:execute(client,['/client','check-in'],check=False).returncode,1,'Control unavailable',timeout=35);managed(False)
                    if builder_invoke:builder_invoke(False)
                finally:run(['docker','unpause',control],capture_output=True)
                until(lambda:healthy(client),True,'offline heartbeat recovery',timeout=45);managed()
                if builder_invoke:builder_invoke(True)
            run(['docker','pause',gw],capture_output=True)
            try:
                tool('hotfolder.write_text',{'path':'a.txt','text':'must not write'},False)
                if args.runtime_image: managed(False)
                if builder_invoke:builder_invoke(False)
            finally:run(['docker','unpause',gw],capture_output=True)
            assert tool('hotfolder.read_text',{'path':'a.txt'})['output']['text']=='logged-out service'
            admin=token(identity)
            status,record=request('/endpoint/devices/'+device,admin);assert status==200,record
            revoked=request('/endpoint/devices/'+device+'/revoke',admin,{'expectedRevision':record['revision']},'POST','revoke-device')
            assert revoked[0]==200,revoked
            until(lambda:execute(client,['/client','check-in'],check=False).returncode,1,'revoked device check-in',timeout=25)
            tool('system.info',{},False)
            if args.runtime_image: managed(False)
            if builder_invoke:builder_invoke(False)
            info=json.loads(run(['docker','inspect',client],capture_output=True,text=True).stdout)[0]
            assert not info['Config']['Tty'] and not info['Config']['OpenStdin']
            threads=int(re.search(r'Threads:\s+(\d+)',execute(client,['cat','/proc/1/status']).stdout).group(1))
            assert threads<=16,threads
            output=ROOT/'build/client';output.mkdir(parents=True,exist_ok=True)
            (output/'module07-integration.json').write_text(json.dumps({'realControlTls':True,'realDeviceEnrollmentMtls':True,'persistentRestart':True,'realGatewayHttps':True,'freshAuthorization':True,'sourceDestinationBinding':True,'unauthorizedOsPeerRejected':True,'gatewayOutageBlocked':True,'revokedDeviceBlocked':True,'noInteractiveSession':True},indent=2)+'\n')
            if args.runtime_image:
                (output/'module08-integration.json').write_text(json.dumps({'realControlEnrollmentMtls':True,'realGatewayHttps':True,'managedJsonInvocation':True,'osPeerDenied':True,'gatewayOutageBlocked':True,'revokedDeviceBlocked':True,'noInteractiveSession':True},indent=2)+'\n')
            if args.fleet:
                (output/'module09-integration.json').write_text(json.dumps({'realSignedReleaseAndDesired':True,'realMtlsArtifactGrant':True,'interruptedTransferAndServiceRecovery':True,'hashMismatchBlocked':True,'atomicActivation':True,'rollbackNewGeneration':True,'uninstall':True,'incompatiblePlatformBlocked':True,'incompatibleArchitectureBlocked':True,'incompatibleRuntimeBlocked':True,'controlOutageAndRecovery':True,'healthTimeoutBlocked':True,'restartRecovery':True,'rolloutAggregation':True,'realBrowserStatusAndAssignment':True,'gatewayOutageBlocked':True,'revocationBlocked':True},indent=2)+'\n')
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
