# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real API approval fixtures shared by browser and cluster gates; ephemeral identities only."""
import hashlib
import json
from control.check import ROOT, request, token


def seed_approvals(runtime, key, tenant='approval-ui'):
    api = runtime+'/api/control/v1'
    admin = token(key, tenant_id=tenant)
    approver = token(key, tenant_id=tenant, sub='approval-reviewer', user_id='approval-reviewer', groups=['toolgate-approver'])
    machine = token(key, tenant_id=tenant, sub='approval-gateway', groups=['toolgate-approval-gateway'])
    fixture = json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
    records = [('users', {'id':'approval-owner','name':'Requester','enabled':True,'revision':1}),
               ('users', {'id':'approval-reviewer','name':'Reviewer','enabled':True,'revision':1}),
               ('agents', {'id':'approval-agent','name':'Agent','enabled':True,'revision':1,'ownerUserId':'approval-owner'}),
               ('tools', {'id':'calculator','name':'Calculator','enabled':True,'revision':1,'definition':fixture['ToolDefinition']}),
               ('policies', {**fixture['ControlPolicy'],'id':'approval-policy','name':'Review calculation','decision':'ASK',
                    'userIds':['approval-owner'],'agentIds':['approval-agent']})]
    for index,(kind,record) in enumerate(records):
        status,body,_=request(api+'/'+kind,admin,record,'POST',{'Idempotency-Key':'approval-seed-'+str(index)})
        assert status==201,(status,body)
    revision=json.loads(request(api+'/config/export',admin)[1])['revision']
    status,body,_=request(api+'/bundles/publish',admin,{'directoryRevision':revision,'expectedSequence':0,'lifetimeMs':1800000,'graceMs':0},'POST',{'Idempotency-Key':'approval-publish'})
    assert status==201,(status,body)
    ids={}
    for name in ('once','temporary','deny','conflict'):
        submission={'input':{**fixture['PolicyInput'],'context':{'requestId':'approval-'+name,'tenantId':tenant,'userId':'approval-owner','agentId':'approval-agent'},
            'argumentsDigest':hashlib.sha256(name.encode()).hexdigest()},'policyVersion':'2.0.1'}
        status,body,_=request(api+'/approvals/resolve',machine,submission,'POST')
        assert status==200,(status,body)
        resolved=json.loads(body);assert resolved['state']=='PENDING'
        ids[name]=resolved['approvalId']
    return approver,ids
