# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""One-time local debug enrollment and exact agent/device/file grants for agent-mimic.bat."""
import hashlib
import json
import os
from pathlib import Path
import re
import secrets
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
CLIENT=Path(os.environ.get('ProgramFiles',r'C:\Program Files'))/'OLO/ToolGateSetup/olo-toolgate-client.exe'
CONSOLE='http://127.0.0.1:18090'
AGENT='debug-mimic-agent'

def main():
    def client(*args):
        result=subprocess.run([str(CLIENT),*args],capture_output=True,text=True,encoding='utf-8')
        if result.returncode: raise ValueError('Client command failed; repair the local installation and its CA first')
        return result.stdout
    def health_until(predicate, timeout=45):
        deadline=time.monotonic()+timeout
        while True:
            try:
                health=json.loads(client('health'))
                if predicate(health):return health
            except (ValueError, KeyError):pass
            if time.monotonic()>deadline:raise ValueError('Client IPC or authenticated check-in unavailable; inspect the client packet log')
            time.sleep(1)
    token=None
    opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))
    def api(path,body=None,method=None,headers=None):
        values={'Content-Type':'application/json','Idempotency-Key':secrets.token_hex(16)}
        if token:values['Authorization']='Bearer '+token
        values.update(headers or {})
        req=urllib.request.Request(CONSOLE+path,json.dumps(body).encode() if body is not None else None,values,method=method or ('POST' if body is not None else 'GET'))
        try:
            with opener.open(req,timeout=20) as response:return response.status,json.loads(response.read(131073))
        except urllib.error.HTTPError as response:return response.code,json.loads(response.read(131073))
    status,result=api('/api/quickstart/v1/status')
    if status!=200 or result.get('passwordRequired') is not False: raise ValueError('This helper requires the local debug stack with its configured password-free admin login')
    status,result=api('/api/quickstart/v1/login',{})
    if status!=200:raise ValueError('Local administrator login failed')
    token=result['accessToken']
    health=health_until(lambda value:'state' in value)
    if health['state'] in ('UNENROLLED','PENDING'):
        prompt=client('enroll')
        code=re.search(r'code=([A-F0-9]{16})',prompt)
        fingerprint=re.search(r'fingerprint in your browser: ([a-f0-9]{64})',prompt)
        if not code or not fingerprint:raise ValueError('Client did not return an enrollment code and fingerprint')
        status,review=api('/api/control/v1/endpoint/enrollments/review?code='+code.group(1))
        if status!=200 or review['keyFingerprint']!=fingerprint.group(1):raise ValueError('Enrollment fingerprint mismatch')
        status,result=api('/api/control/v1/endpoint/enrollments/decision',{'userCode':code.group(1),'keyFingerprint':fingerprint.group(1),'choice':'APPROVE'})
        if status!=200:raise ValueError('Local enrollment approval failed')
        device=review['deviceId']
        print('Approved enrollment after matching the real client key fingerprint.',flush=True)
    else:
        saved=ROOT/'.dev/debug/mimic-client.json'
        if not saved.exists():raise ValueError('Already enrolled: this helper needs .dev/debug/mimic-client.json from its previous successful run')
        previous=json.loads(saved.read_text(encoding='utf-8'))
        if previous.get('gateway')!='https://localhost:18450':raise ValueError('Saved client belongs to another Gateway')
        device=previous['deviceId']
    health_until(lambda value:value.get('ready') is True)
    status,record=api('/api/control/v1/endpoint/devices/'+urllib.parse.quote(device,safe=''))
    if status!=200 or record['state']!='ACTIVE' or record['reportSequence']<1:raise ValueError('Server has not observed the enrolled client')
    user=record['userId']
    local=ROOT/'.dev/debug';local.mkdir(parents=True,exist_ok=True)
    # Keep the non-secret binding early so an interrupted setup can resume without re-enrolling.
    (local/'mimic-client.json').write_text(json.dumps({'gateway':'https://localhost:18450','deviceId':device,'agentId':AGENT,'userId':user},indent=2)+'\n',encoding='utf-8')
    status,existing=api('/api/control/v1/agents/'+AGENT)
    if status==404:
        status,_=api('/api/control/v1/agents',{'id':AGENT,'name':'Debug mimic agent','enabled':True,'revision':1,'ownerUserId':user})
        if status!=201:raise ValueError('Agent registration failed')
    elif status!=200 or existing['ownerUserId']!=user or not existing['enabled']:raise ValueError('Existing debug agent does not match this client owner')
    for ident,tool,kind,locator in [('debug-mimic-write','hotfolder.write_text','FILE','rahul-nigam.txt'),('debug-mimic-log','client.read_log_entry','CUSTOM','hotfolder')]:
        policy={'id':ident,'name':'Debug mimic '+tool,'enabled':True,'revision':1,'toolId':tool,'action':'write' if kind=='FILE' else 'read','resource':{'kind':kind,'locator':locator},'decision':'ALLOW','userIds':[user],'teamIds':[],'agentIds':[AGENT],'deviceIds':[device]}
        status,existing=api('/api/control/v1/policies/'+ident)
        if status==404:status,_=api('/api/control/v1/policies',policy)
        elif status==200:
            policy['revision']=existing['revision']
            status,_=api('/api/control/v1/policies/'+ident,policy,'PUT',{'If-Match':f'"{existing["revision"]}"'})
        if status not in (200,201):raise ValueError('Scoped tool permission failed: '+tool)
    status,snapshot=api('/api/control/v1/config/export')
    if status!=200:raise ValueError('Directory configuration unavailable')
    status,bundle=api('/api/control/v1/bundles/current')
    if status!=200:raise ValueError('Current Gateway bundle unavailable')
    import base64
    sequence=json.loads(base64.urlsafe_b64decode(bundle['jws'].split('.')[1]+'=='))['sequence']
    status,_=api('/api/control/v1/bundles/publish',{'directoryRevision':snapshot['revision'],'expectedSequence':sequence,'lifetimeMs':86400000,'graceMs':0})
    if status!=201:raise ValueError('Gateway permission publication failed')
    agent_token=secrets.token_urlsafe(48)
    local=ROOT/'.dev/debug';local.mkdir(parents=True,exist_ok=True)
    token_file=local/'client-agent-token'
    token_file.write_text(agent_token,encoding='utf-8');token_file.chmod(0o600)
    if os.name=='nt':
        # Keep the bearer token private to this Windows account and SYSTEM.
        sid=subprocess.run(['powershell','-NoProfile','-Command','[Security.Principal.WindowsIdentity]::GetCurrent().User.Value'],capture_output=True,text=True,check=True).stdout.strip()
        subprocess.run(['icacls',str(token_file),'/inheritance:r','/grant:r',f'*{sid}:(F)','*S-1-5-18:(F)'],check=True,capture_output=True)
    credential={'tokenSha256':hashlib.sha256(agent_token.encode()).hexdigest(),'tenantId':record['tenantId'],'userId':user,'agentId':AGENT,'deviceId':device,'expiresAtUnixMs':int(time.time()*1000)+86400000}
    container=subprocess.run(['docker','compose','--project-name','toolgate-debug','--project-directory',str(ROOT/'debug'),'-f',str(ROOT/'debug/compose.yaml'),'ps','-q','quickstart'],capture_output=True,text=True,check=True).stdout.strip()
    if not container:raise ValueError('Local debug container unavailable')
    script="import json,os,pathlib,sys; p=pathlib.Path('/data/client-runtime-credentials.json'); values=json.loads(p.read_text()) if p.exists() else []; item=json.load(sys.stdin); values=[v for v in values if v['agentId']!=item['agentId']]; values.append(item); p.write_text(json.dumps(values)); p.chmod(0o600)"
    subprocess.run(['docker','exec','-i','--user','65532',container,'/opt/quickstart-python/bin/python','-c',script],input=json.dumps(credential),text=True,check=True,capture_output=True)
    (local/'mimic-client.json').write_text(json.dumps({'gateway':'https://localhost:18450','deviceId':device,'agentId':AGENT,'userId':user,'tokenExpiresAtUnixMs':credential['expiresAtUnixMs']},indent=2)+'\n',encoding='utf-8')
    subprocess.run(['docker','compose','--project-name','toolgate-debug','--project-directory',str(ROOT/'debug'),'-f',str(ROOT/'debug/compose.yaml'),'restart','quickstart'],cwd=ROOT,check=True)
    deadline=time.monotonic()+120
    while True:
        ready=subprocess.run(['docker','inspect','--format','{{.State.Health.Status}}',container],capture_output=True,text=True,check=True).stdout.strip()
        if ready=='healthy':break
        if time.monotonic()>deadline:raise ValueError('Gateway restart did not become healthy')
        time.sleep(1)
    health_until(lambda value:value.get('ready') is True)
    # Let the next two-second poll acknowledge the replacement permission set.
    time.sleep(3)
    print('Enrolled client '+device+'. Exact file/log grants and a 24-hour device-bound agent credential are ready.',flush=True)
    return 0

if __name__=='__main__':
    try:sys.exit(main())
    except (ValueError,OSError,subprocess.CalledProcessError) as failure:
        print('FAILED: '+(str(failure) if isinstance(failure,ValueError) else 'Local setup operation failed; check client and Gateway status'),file=sys.stderr);sys.exit(1)
