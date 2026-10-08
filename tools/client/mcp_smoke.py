# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real isolated HTTPS Gateway -> poll -> native client -> result -> agent-mimic regression."""
import argparse
import hashlib
import importlib.util
import json
from pathlib import Path
import re
import secrets
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(0,str(ROOT/'tools'))
from quickstart.check import ready, run

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image',default='olo-toolgate-quickstart:debug')
    parser.add_argument('--binary',type=Path,default=ROOT/'target/client-release/x86_64-unknown-linux-gnu/release/olo-toolgate-client')
    args=parser.parse_args()
    name='toolgate-mcp-smoke-'+secrets.token_hex(6)
    containers=[];volumes=[name+'-server',name+'-client']
    (ROOT/'.dev/debug').mkdir(parents=True,exist_ok=True)
    try:
        with tempfile.TemporaryDirectory(prefix='mcp-smoke-',dir=ROOT/'.dev/debug') as directory:
            directory=Path(directory)
            server=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--memory=1g','--cpus=2','--tmpfs','/tmp:rw,noexec,nosuid,size=128m,uid=65532,gid=65532','-e','TOOLGATE_DISABLE_ADMIN_PASSWORD=true','-e','TOOLGATE_QUICKSTART_CLIENT_CREDENTIALS=true','-p','127.0.0.1::8080','-p','127.0.0.1::8443','-v',volumes[0]+':/data',args.image],capture_output=True,text=True).stdout.strip();containers.append(server)
            info=json.loads(run(['docker','inspect',server],capture_output=True,text=True).stdout)[0]
            port=info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
            tls_port=info['NetworkSettings']['Ports']['8443/tcp'][0]['HostPort']
            ready(server,port)
            origin='http://127.0.0.1:'+port
            admin=None
            def api(path,body=None,method=None,extra=None):
                headers={'Content-Type':'application/json','Idempotency-Key':secrets.token_hex(16)}
                if admin:headers['Authorization']='Bearer '+admin
                headers.update(extra or {})
                req=urllib.request.Request(origin+path,json.dumps(body).encode() if body is not None else None,headers,method=method or ('POST' if body is not None else 'GET'))
                try:
                    with urllib.request.urlopen(req,timeout=20) as response:return response.status,json.loads(response.read())
                except urllib.error.HTTPError as response:return response.code,json.loads(response.read())
            admin=api('/api/quickstart/v1/login',{})[1]['accessToken']
            ca=directory/'ca.crt';ca.write_bytes(run(['docker','exec',server,'cat','/data/keys/device-ca.crt'],capture_output=True).stdout)
            config={'serverUrl':'https://localhost:8443','stateDirectory':'/state/private','ipcEndpoint':'/run/olo-toolgate/client.sock','authorizedPeers':['0'],'caCertificatePath':'/state/ca.crt','requestTimeoutSeconds':10}
            (directory/'client.json').write_text(json.dumps(config),encoding='utf-8')
            boot='mkdir -p /run/olo-toolgate; cp /input/client.json /input/ca.crt /state/; chmod 600 /state/client.json /state/ca.crt; exec /client service --config /state/client.json'
            client=run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--network','container:'+server,'--tmpfs','/run:rw,nosuid,size=8m','-v',volumes[1]+':/state','-v',directory.as_posix()+':/input:ro','-v',args.binary.resolve().as_posix()+':/client:ro','ubuntu:24.04','sh','-c',boot],capture_output=True,text=True).stdout.strip();containers.append(client)
            def cli(*arguments):return run(['docker','exec',client,'/client',*arguments],capture_output=True,text=True).stdout
            def healthy():
                try:return json.loads(cli('health')).get('ready',False)
                except (subprocess.CalledProcessError,ValueError):return False
            deadline=time.monotonic()+30
            while True:
                try:health=json.loads(cli('health'));break
                except subprocess.CalledProcessError:
                    if time.monotonic()>deadline:raise RuntimeError('Client IPC unavailable')
                    time.sleep(.5)
            assert health['state']=='UNENROLLED'
            prompt=cli('enroll');code=re.search(r'code=([A-F0-9]{16})',prompt).group(1);fingerprint=re.search(r'fingerprint in your browser: ([a-f0-9]{64})',prompt).group(1)
            status,review=api('/api/control/v1/endpoint/enrollments/review?code='+code);assert status==200 and review['keyFingerprint']==fingerprint
            device=review['deviceId']
            assert api('/api/control/v1/endpoint/enrollments/decision',{'userCode':code,'keyFingerprint':fingerprint,'choice':'APPROVE'})[0]==200
            deadline=time.monotonic()+45
            while not healthy():
                if time.monotonic()>deadline:raise RuntimeError('mTLS check-in unavailable')
                time.sleep(.5)
            agent='mcp-smoke-agent'
            assert api('/api/control/v1/agents',{'id':agent,'name':'MCP smoke agent','enabled':True,'revision':1,'ownerUserId':'admin'})[0]==201
            for ident,tool,action,kind,path in [('smoke-write','hotfolder.write_text','write','FILE','rahul-nigam.txt'),('smoke-log','client.read_log_entry','read','CUSTOM','hotfolder')]:
                policy={'id':ident,'name':ident,'enabled':True,'revision':1,'toolId':tool,'action':action,'resource':{'kind':kind,'locator':path},'decision':'ALLOW','userIds':['admin'],'teamIds':[],'agentIds':[agent],'deviceIds':[device]}
                status,value=api('/api/control/v1/policies',policy);assert status==201,(status,value)
            token=secrets.token_urlsafe(48);token_file=directory/'agent-token';token_file.write_text(token);token_file.chmod(0o600)
            credential={'tokenSha256':hashlib.sha256(token.encode()).hexdigest(),'tenantId':'quickstart','userId':'admin','agentId':agent,'deviceId':device,'expiresAtUnixMs':int(time.time()*1000)+600000}
            script="import json,pathlib,sys; p=pathlib.Path('/data/client-runtime-credentials.json'); p.write_text(json.dumps([json.load(sys.stdin)])); p.chmod(0o600)"
            run(['docker','exec','-i',server,'/opt/quickstart-python/bin/python','-c',script],input=json.dumps(credential),text=True,capture_output=True)
            run(['docker','restart','-t','35',server],capture_output=True)
            info=json.loads(run(['docker','inspect',server],capture_output=True,text=True).stdout)[0]
            port=info['NetworkSettings']['Ports']['8080/tcp'][0]['HostPort']
            tls_port=info['NetworkSettings']['Ports']['8443/tcp'][0]['HostPort']
            origin='http://127.0.0.1:'+port
            ready(server,port)
            run(['docker','restart',client],capture_output=True)
            deadline=time.monotonic()+45
            while not healthy():
                if time.monotonic()>deadline:raise RuntimeError('Client did not reconnect')
                time.sleep(.5)
            spec=importlib.util.spec_from_file_location('mcp_smoke_mimic',ROOT/'debug/agent-mimic.py');module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
            # Permission replacement is acknowledged by an actual client poll before discovery.
            time.sleep(2)
            entry=module.mimic(module.Agent('https://127.0.0.1:'+tls_port,token_file,ca),'rahul-nigam.txt')
            assert run(['docker','exec',client,'cat','/state/private/hotfolder/rahul-nigam.txt'],capture_output=True,text=True).stdout==module.TEXT
            assert entry['service']=='client' and entry['event']=='protocol_packet'
            status,records=api('/api/control/v1/mcp/requests');assert status==200
            done=[record for record in records['items'] if record['deviceId']==device and record['agentId']==agent]
            assert len(done)==2 and all(record['state']=='DONE' and record['submittedAtUnixMs'] and record['responseAtUnixMs'] and record['completedAtUnixMs'] for record in done),done
            server_logs=run(['docker','logs',server],capture_output=True,text=True)
            client_logs=run(['docker','logs',client],capture_output=True,text=True)
            for logs in (server_logs.stdout+server_logs.stderr,client_logs.stdout+client_logs.stderr):
                assert 'protocol_packet' in logs and 'SEND' in logs and 'RECEIVE' in logs
                for secret in (token,admin,code,module.TEXT):assert secret not in logs
            print('PASS real TLS discovery, two sequential polled client calls, exact file, one log entry, DONE handoffs and packet redaction',flush=True)
            output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True)
            (output/'mcp-smoke.json').write_text(json.dumps({'deviceId':device,'requests':done,'fileTextVerified':True,'oneLogEntry':True,'packetRedaction':True},indent=2)+'\n')
    except Exception:
        output=ROOT/'build/quickstart';output.mkdir(parents=True,exist_ok=True)
        for index,container in enumerate(containers):
            result=subprocess.run(['docker','logs',container],capture_output=True,text=True,encoding='utf-8')
            (output/f'mcp-failure-{index}.log').write_text(result.stdout+result.stderr,encoding='utf-8')
        raise
    finally:
        for container in reversed(containers):subprocess.run(['docker','rm','-f',container],capture_output=True)
        for volume in volumes:subprocess.run(['docker','volume','rm',volume],capture_output=True)

if __name__=='__main__':main()
