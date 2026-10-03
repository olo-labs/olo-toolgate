# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Builder assertions within the owned real TLS/fleet harness; no execution mocks."""
import copy
import hashlib
import json
import os
import secrets
import subprocess
from pathlib import Path
from builder.sign import sign_release

def check(root, work, request, execute, client, admin, enroller, device, runtime, release_key, until, api):
    definition=copy.deepcopy(json.loads((root/'tests/fixtures/contracts/v1/valid.json').read_text())['BuilderDefinition'])
    definition['runtime']={**runtime,'id':'builder-python'};definition['tool']['runtimeId']='builder-python'
    draft_id='builder-'+secrets.token_hex(8)
    body={'id':draft_id,'expectedRevision':0,'definition':definition}
    assert request('/builder/drafts',enroller,body,'POST','wrong-author')[0]==403
    for mutation,expected in [('secret',400),('permission',403),('schema',400)]:
        invalid=copy.deepcopy(body)
        if mutation=='secret':
            code="api_key = '"+'fixture-sensitive-value'+"'\n"+definition['tool']['source']['code']
            invalid['definition']['tool']['source']={'code':code,'sha256':hashlib.sha256(code.encode()).hexdigest()}
        if mutation=='permission':invalid['definition']['permissions']=['FILE_READ']
        if mutation=='schema':invalid['definition']['tool']['inputSchema']['$ref']='https://example.invalid/schema'
        assert request('/builder/drafts',admin,invalid,'POST','invalid-'+mutation)[0]==expected
    response=request('/builder/drafts',admin,body,'POST','save-builder');assert response[0]==200,response
    draft=response[1]
    def invoke(success):
        result=execute(client,['/client','run','custom.echo',json.dumps({'text':'safe; $(touch /escape)'})],check=False)
        assert (result.returncode==0)==success,(result.stdout,result.stderr)
        if success:assert json.loads(result.stdout)['result']['output']['text']=='safe; $(touch /escape)'
    invoke(False)  # A draft is never an assigned executable capability.
    job={'id':'job-'+secrets.token_hex(8),'draftId':draft_id,'expectedRevision':draft['revision'],'deviceId':device,'exampleIndex':0}
    assert request('/builder/tests',admin,{**job,'deviceId':'wrong-device'},'POST','wrong-device-test')[0]==403
    response=request('/builder/tests',admin,job,'POST','test-builder');assert response[0]==200,response
    def completed():
        execute(client,['/client','check-in'],check=False)
        status=request('/builder/tests',admin);assert status[0]==200,status
        record=next((j for j in status[1]['items'] if j['id']==job['id']),None)
        if record and record['state'] in ('FAILED','EXPIRED'):raise AssertionError('Real designated sandbox rejected test: '+json.dumps(record))
        return record is not None and record['state']=='PASSED'
    until(completed,True,'real signed designated-client source execution',timeout=90)
    invoke(False)  # A passed test still grants no ordinary invocation capability.
    response=request('/builder/drafts/'+draft_id+'/seal',admin,{'expectedRevision':draft['revision']},'POST','seal-builder');assert response[0]==200,response
    sealed=response[1]
    assert request('/builder/drafts',admin,{**body,'expectedRevision':sealed['revision']},'POST','edit-sealed')[0]==409
    raw=json.dumps(sealed['packageDocument'],separators=(',',':')).encode()
    release=sign_release(raw,release_key,'release');(work/'artifacts'/(release['manifestDigest']+'.json')).write_bytes(raw)
    path='/builder/drafts/'+draft_id
    assert request(path+'/release',admin,release,'POST','release-builder')[0]==200
    rollout={'id':'builder-rollout','packageId':definition['packageId'],'version':definition['version'],'deviceIds':[device],'desiredPresence':True,'percentage':100}
    response=request(path+'/deploy',admin,rollout,'POST','deploy-builder');assert response[0]==200,response
    def ready():
        execute(client,['/client','check-in'],check=False)
        report=request('/endpoint/devices/'+device,admin)[1].get('report') or {}
        return any(p['packageId']==definition['packageId'] and p['state']=='READY' for p in report.get('packages',[]))
    until(ready,True,'sealed source package deployment and activation',timeout=90);invoke(True)
    assert request('/builder/drafts',admin,{**body,'expectedRevision':sealed['revision']},'POST','edit-assigned')[0]==409
    status,publication=request(path+'/publication',admin,None,'POST');assert status==200 and publication==definition
    assert not any(field in publication for field in ['tenantId','deviceId','leaseId'])
    credentials=work/'builder-browser-credentials.json';credentials.write_text(json.dumps({'admin':admin}));credentials.chmod(0o600)
    env=dict(os.environ,UI_TEST_ORIGIN=api.removesuffix('/api/control/v1'),UI_TEST_CREDENTIALS=str(credentials.resolve()),UI_TEST_BUILDER='1',UI_TEST_FLEET_DEVICE=device,UI_TEST_BUILDER_IMAGE=runtime['image'],UI_TEST_BUILDER_VERSION=runtime['version'])
    subprocess.run(['npx.cmd' if os.name=='nt' else 'npx','--no-install','playwright','test','tests/e2e/builder.spec.ts'],cwd=root/'apps/admin-ui',env=env,check=True)
    (root/'build/client/module10-integration.json').write_text(json.dumps({'realTlsAuthoring':True,'secretPermissionSchemaRejected':True,'wrongRoleRejected':True,'wrongClientRejected':True,'signedMtlsSandboxTestPassed':True,'testDoesNotAssignCapability':True,'sealAndAssignmentImmutable':True,'independentReleaseVerified':True,'realFleetDeployment':True,'freshGatewayInvocation':True,'publicationPreparation':True,'realBrowserAuthoring':True},indent=2)+'\n')
    return invoke
