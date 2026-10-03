# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Browser gate against embedded production UI, real PostgreSQL and signed identities.

Only isolated test infrastructure is owned here. Credentials are private temporary
test inputs, not traces, screenshots, command arguments or application fixtures.
"""
import argparse
import json
import os
import subprocess
import sys
import tempfile
import time
from pathlib import Path
from cryptography.hazmat.primitives.asymmetric import rsa
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from control.check import ROOT, database, environment, free_port, keypair, ready, request, token
from approval.fixtures import seed_approvals
from policy.check import signing_key


def seed(runtime, key):
    api = runtime+'/api/control/v1'; credential=token(key)
    fixture=json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
    records={
        'users':{'id':'browser-owner','name':'Browser owner','enabled':True,'revision':1},
        'teams':{'id':'browser-team','name':'Browser team','enabled':True,'revision':1,'userIds':['browser-owner']},
        'agents':{'id':'browser-agent','name':'Browser agent','enabled':True,'revision':1,'ownerUserId':'browser-owner'},
        'devices':{'id':'browser-device','name':'Browser device','enabled':True,'revision':1,'ownerUserId':'browser-owner'},
        'tools':{'id':fixture['ToolDefinition']['id'],'name':'Browser tool','enabled':True,'revision':1,'definition':fixture['ToolDefinition']},
        'policies':{**fixture['ControlPolicy'],'id':'browser-policy','name':'Browser policy','userIds':['browser-owner'],'teamIds':[],'agentIds':[],'deviceIds':[]},
    }
    for kind,record in records.items():
        status,body,_=request(api+'/'+kind,credential,record,'POST',{'Idempotency-Key':'browser-seed-'+kind})
        assert status==201,(kind,status,body)
    paging=token(key,tenant_id='paging-tenant')
    for index in range(51):
        record={'id':f'page-{index:03}','name':f'Page user {index:03}','enabled':True,'revision':1}
        assert request(api+'/users',paging,record,'POST',{'Idempotency-Key':f'page-seed-{index}'})[0]==201
    return {'admin':credential,'reader':token(key,groups=['toolgate-reader']), 'paging':paging,
            'empty':token(key,tenant_id='empty-tenant'), 'expired':token(key,exp=int(time.time())-1),
            'invalid':token(rsa.generate_private_key(public_exponent=65537,key_size=2048))}


def browser(runtime, key, work):
    credentials=seed(runtime,key); approver,approval_ids=seed_approvals(runtime,key); credentials['approver']=approver; path=work/'browser-credentials.json'
    path.write_text(json.dumps(credentials),encoding='utf-8');path.chmod(0o600)
    env=dict(os.environ,UI_TEST_ORIGIN=runtime,UI_TEST_CREDENTIALS=str(path),UI_TEST_APPROVAL_IDS=json.dumps(approval_ids))
    result=subprocess.run(['npm.cmd' if os.name=='nt' else 'npm','--workspace','@olo-labs/toolgate-admin-ui','run','e2e'],cwd=ROOT,env=env,capture_output=True,text=True,encoding='utf-8')
    # Reporters can include locator arguments on failure. Test credentials must
    # not survive in uploaded JSON/text evidence either.
    for report in (ROOT/'build/ui').rglob('*'):
        if report.suffix not in ('.json','.md','.txt','.log'): continue
        content=report.read_text(encoding='utf-8')
        for secret in credentials.values(): content=content.replace(secret,'[redacted test credential]')
        report.write_text(content,encoding='utf-8')
    output=result.stdout+result.stderr
    for secret in credentials.values(): output=output.replace(secret,'[redacted test credential]')
    print(output,flush=True)
    if result.returncode: raise SystemExit(result.returncode)
    evidence=ROOT/'build/ui';evidence.mkdir(parents=True,exist_ok=True)
    (evidence/'smoke.json').write_text(json.dumps({'realPostgres':True,'signedTokens':True,'readerDenied':True,'expiredRejected':True,
        'embeddedAssets':True,'strictCsp':True,'noTokenStorage':True,'revisionConflict':True,'userCrud':True,'cursorPaging':True,
        'wcagAutomatedChecks':True,'keyboardFocus':True,'desktopMobile':True,'approvalOnceTemporaryDeny':True,'approvalConflict':True,'approverOnlySession':True},indent=2)+'\n',encoding='utf-8')


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--no-build',action='store_true');parser.add_argument('--serve',action='store_true',help='Keep an isolated development workspace running until Ctrl+C');args=parser.parse_args()
    (ROOT/'.dev').mkdir(exist_ok=True)
    with database() as db, tempfile.TemporaryDirectory(prefix='ui-browser-',dir=ROOT/'.dev') as temp:
        work=Path(temp);key,public=keypair(work);env=environment(db,public)
        _,signing_path,_=signing_key(work,'browser-bundle')
        env.update(TOOLGATE_CONTROL_BUNDLE_ENABLED='true',TOOLGATE_CONTROL_BUNDLE_KEY_ID='browser-bundle',TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH=str(signing_path),TOOLGATE_CONTROL_APPROVAL_ENABLED='true')
        env.update(CONTROL_TEST_URL=db['CONTROL_TEST_URL'],CONTROL_TEST_PASSWORD=db['CONTROL_TEST_PASSWORD'])
        if not args.no_build:
            subprocess.run([str(ROOT/('gradlew.bat' if os.name=='nt' else 'gradlew')),'--no-daemon',':control-plane:build'],cwd=ROOT,env=env,check=True)
        api_port,management_port=free_port(),free_port()
        env.update(QUARKUS_HTTP_HOST='127.0.0.1',QUARKUS_HTTP_PORT=str(api_port),QUARKUS_MANAGEMENT_HOST='127.0.0.1',QUARKUS_MANAGEMENT_PORT=str(management_port))
        evidence=ROOT/'build/ui';evidence.mkdir(parents=True,exist_ok=True)
        with (evidence/'control-private.log').open('w',encoding='utf-8') as log:
            process=subprocess.Popen(['java','-jar','apps/control-plane/build/quarkus-app/quarkus-run.jar'],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT)
            try:
                ready(f'http://127.0.0.1:{management_port}',process)
                runtime=f'http://127.0.0.1:{api_port}'
                if args.serve:
                    credentials=seed(runtime,key);path=work/'browser-credentials.json';path.write_text(json.dumps(credentials),encoding='utf-8');path.chmod(0o600)
                    print(f'Local-only console: {runtime}/console/\nGenerated short-lived test access tokens: {path}\nNo company credentials required. Stop with Ctrl+C.',flush=True)
                    try: process.wait()
                    except KeyboardInterrupt: pass
                else: browser(runtime,key,work)
            finally:
                if process.poll() is None:process.terminate()
                process.wait(timeout=30)


if __name__=='__main__': main()
