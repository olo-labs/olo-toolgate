# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Build and smoke the actual production image with a read-only root filesystem."""
import argparse
import hashlib
import json
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

from local_credentials import prepare

ROOT = Path(__file__).resolve().parents[2]


def run(args, **kwargs):
    # Command arguments contain paths and public metadata, never bearer tokens.
    return subprocess.run(args,cwd=ROOT,check=True,**kwargs)


def request(url, token=None, body=None, extra_headers=None):
    headers = {'content-type':'application/json'}
    if token: headers['authorization'] = 'Bearer '+token
    headers.update(extra_headers or {})
    data = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data,headers=headers)
    try:
        with urllib.request.urlopen(req,timeout=2) as response: return response.status,response.read()
    except urllib.error.HTTPError as error: return error.code,error.read()


def smoke(image):
    root = ROOT/'.dev'; root.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='gateway-smoke-',dir=root) as work:
        work = Path(work); token, _ = prepare(work)
        # Digest-only credential input is intentionally world-readable inside the
        # non-root container mount; the bearer token is never mounted there.
        (work/'credentials.json').chmod(0o644)
        cfg = json.loads((ROOT/'docs/examples/gateway-static.json').read_text())
        cfg['listen']='0.0.0.0:8081'; cfg['managementListen']='0.0.0.0:9091'; cfg['trustedTlsProxy']=True
        (work/'gateway.json').write_text(json.dumps(cfg),encoding='utf-8')
        container = run(['docker','run','-d','--read-only','--cap-drop=ALL','--security-opt=no-new-privileges','--memory=256m','--cpus=1','-p','127.0.0.1::8081','-p','127.0.0.1::9091','-v',f'{(work/"gateway.json").as_posix()}:/config/gateway.json:ro','-v',f'{(work/"credentials.json").as_posix()}:/config/credentials.json:ro','-e','TOOLGATE_GATEWAY_CONFIG=/config/gateway.json','-e','TOOLGATE_GATEWAY_CREDENTIALS=/config/credentials.json',image],capture_output=True,text=True).stdout.strip()
        try:
            inspected = json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
            assert inspected['Config']['User']=='65532:65532'
            run(['docker','cp',container+':/usr/share/licenses/olo-toolgate/third-party/Cargo.lock',str(work/'image-Cargo.lock')],capture_output=True)
            assert (work/'image-Cargo.lock').read_bytes()==(ROOT/'Cargo.lock').read_bytes()
            run(['docker','cp',container+':/usr/share/licenses/olo-toolgate/third-party/rust/COPYRIGHT.html',str(work/'rust-COPYRIGHT.html')],capture_output=True)
            assert 'musl' in (work/'rust-COPYRIGHT.html').read_text(encoding='utf-8').lower()
            def url(port): return 'http://127.0.0.1:'+inspected['NetworkSettings']['Ports'][str(port)+'/tcp'][0]['HostPort']
            runtime, management = url(8081),url(9091)
            deadline = time.monotonic()+30
            # Operational readiness polling is bounded; test correctness depends
            # on the endpoint result, not a guessed startup delay.
            while True:
                try:
                    if request(management+'/v1/health/ready')[0]==200: break
                except (urllib.error.URLError, TimeoutError, ConnectionError): pass
                if time.monotonic()>=deadline: raise RuntimeError('container did not become ready')
                time.sleep(0.1)
            payload={'toolId':'files.read','action':'read','arguments':{'path':'workspace/readme.txt','private':'secret-redaction-sentinel'}}
            code,body=request(runtime+'/v1/authorize',token,payload); assert code==200 and json.loads(body)['decision']=='ALLOW'
            payload['arguments']['path']='workspace/other.txt'
            code,body=request(runtime+'/v1/authorize',token,payload); assert code==200 and json.loads(body)['decision']=='BLOCK'
            assert request(runtime+'/v1/authorize',None,payload)[0]==401
            assert request(runtime+'/v1/health/live',token)[0]==404
            assert request(management+'/v1/metrics')[0]==200
            mcp_headers={'accept':'application/json, text/event-stream','mcp-protocol-version':'2026-07-28','mcp-method':'ping'}
            ping={'jsonrpc':'2.0','id':7,'method':'ping','params':{'_meta':{'io.modelcontextprotocol/protocolVersion':'2026-07-28','io.modelcontextprotocol/clientCapabilities':{}}}}
            code,body=request(runtime+'/mcp',token,ping,mcp_headers)
            assert code==200 and json.loads(body)['result']['resultType']=='complete'
            code,body=request(runtime+'/mcp',token,b'{',mcp_headers)
            parsed=json.loads(body); assert code==400 and parsed['error']['code']==-32700 and 'id' in parsed and parsed['id'] is None
            run(['docker','stop','--time','20',container],capture_output=True)
            inspected = json.loads(run(['docker','inspect',container],capture_output=True,text=True).stdout)[0]
            assert inspected['State']['ExitCode']==0, inspected['State']['ExitCode']
            logs=run(['docker','logs',container],capture_output=True,text=True)
            combined=logs.stdout+logs.stderr
            assert token not in combined and 'secret-redaction-sentinel' not in combined and 'workspace/readme.txt' not in combined
            events=[json.loads(line) for line in logs.stdout.splitlines()]; assert len(events)==2
            info = json.loads(run(['docker','image','inspect',image],capture_output=True,text=True).stdout)[0]
            output=ROOT/'build/gateway'; output.mkdir(parents=True,exist_ok=True)
            (output/'container-smoke.json').write_text(json.dumps({'image':image,'imageId':info['Id'],'imageBytes':info['Size'],'thirdPartyNotices':True,'stdoutStderrRedacted':True,'nonRoot':True,'readOnlyRoot':True,'allowBlockAuthProbesMetrics':True,'mcpPingAndParseError':True,'sigtermExitCode':0,'memoryLimitBytes':inspected['HostConfig']['Memory']},indent=2)+'\n',encoding='utf-8')
            print('Production container smoke passed: ALLOW/BLOCK/auth, separate management, metrics, audit redaction and SIGTERM')
        finally:
            run(['docker','rm','-f',container],capture_output=True)


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--image',default='olo-toolgate-gateway:module05')
    parser.add_argument('--no-build',action='store_true')
    args=parser.parse_args()
    if not args.no_build:
        version=(ROOT/'VERSION').read_text().strip()
        revision=run(['git','rev-parse','HEAD'],capture_output=True,text=True).stdout.strip()
        run(['docker','build','-f','apps/gateway/Dockerfile','--build-arg','VERSION='+version,'--build-arg','REVISION='+revision,'-t',args.image,'.'])
    smoke(args.image)


if __name__=='__main__': main()
