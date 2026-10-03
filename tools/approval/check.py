# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Real production ASK -> human decision -> signed permit -> atomic consume gate."""
import argparse
import base64
import concurrent.futures
import hashlib
import json
import os
import secrets
import sys
import tempfile
import time
from pathlib import Path
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from control.check import ROOT, database, environment, keypair, ready, request, run, token
from policy.check import signing_key, until


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--build', action='store_true')
    parser.add_argument('--control-image', default='olo-toolgate-control:module05')
    parser.add_argument('--gateway-image', default='olo-toolgate-gateway:module05')
    args = parser.parse_args()
    if args.build:
        version = (ROOT/'VERSION').read_text().strip()
        revision = run(['git', 'rev-parse', 'HEAD'], capture_output=True, text=True).stdout.strip()
        for component, image in [('control-plane', args.control_image), ('gateway', args.gateway_image)]:
            run(['docker', 'build', '-f', f'apps/{component}/Dockerfile', '--build-arg', f'VERSION={version}',
                 '--build-arg', f'REVISION={revision}', '-t', image, '.'])
    ids = {name:run(['docker', 'image', 'inspect', '--format', '{{.Id}}', image], capture_output=True, text=True).stdout.strip()
           for name, image in [('control', args.control_image), ('gateway', args.gateway_image)]}
    output = ROOT/'build/approval'; output.mkdir(parents=True, exist_ok=True)
    with database() as db, tempfile.TemporaryDirectory(prefix='approval-e2e-', dir=ROOT/'.dev') as temp:
        work = Path(temp); control_work = work/'control'; gateway_work = work/'gateway'
        control_work.mkdir(); gateway_work.mkdir()
        identity, public = keypair(control_work)
        policy_key, _, policy_public = signing_key(control_work, 'bundle-key')
        permit_key, _, _ = signing_key(gateway_work, 'permit-key')
        (gateway_work/'keyring.json').write_text(json.dumps({'keys':[policy_public]}), encoding='utf-8')
        machine = token(identity, sub='approval-gateway', groups=['toolgate-bundle-reader', 'toolgate-approval-gateway'])
        for name in ('bundle-token', 'approval-token'):
            (gateway_work/name).write_text(machine, encoding='utf-8')
        admin = token(identity)
        approver = token(identity, sub='bob', user_id='bob', groups=['toolgate-approver'])
        requester = token(identity, sub='alice', user_id='alice', groups=['toolgate-approver'])
        runtime_token = secrets.token_urlsafe(48)
        credentials = [{'tokenSha256':hashlib.sha256(runtime_token.encode()).hexdigest(), 'tenantId':'http-tenant',
                        'userId':'alice', 'agentId':'agent-demo', 'expiresAtUnixMs':int(time.time()*1000)+300000}]
        (gateway_work/'credentials.json').write_text(json.dumps(credentials), encoding='utf-8')
        config = json.loads((ROOT/'docs/examples/gateway-static.json').read_text())
        config.update(listen='0.0.0.0:8081', managementListen='0.0.0.0:9091', trustedTlsProxy=True)
        config.pop('policy')
        config['bundleSource'] = {'url':'http://127.0.0.1:8082/api/control/v1/bundles/current', 'tenantId':'http-tenant',
            'issuer':'control', 'audience':'gateway', 'keyringPath':'/config/keyring.json', 'tokenPath':'/config/bundle-token',
            'minimumSequence':0, 'maxGraceMs':0, 'pollIntervalMs':100, 'fetchTimeoutMs':500, 'developmentLoopbackHttp':True}
        config['approval'] = {'url':'http://127.0.0.1:8082', 'tokenPath':'/config/approval-token',
            'privateKeyPath':'/config/permit-key.pem', 'keyId':'permit-key', 'issuer':'gateway', 'audience':'endpoint',
            'requestTimeoutMs':500, 'permitLifetimeMs':10000, 'developmentLoopbackHttp':True}
        (gateway_work/'gateway.json').write_text(json.dumps(config), encoding='utf-8')
        env = environment(db, Path('/config/jwt-public.pem'))
        env.update(QUARKUS_DATASOURCE_JDBC_URL='jdbc:postgresql://127.0.0.1:5432/control?sslmode=disable',
            TOOLGATE_CONTROL_BUNDLE_ENABLED='true', TOOLGATE_CONTROL_BUNDLE_KEY_ID='bundle-key',
            TOOLGATE_CONTROL_BUNDLE_PRIVATE_KEY_PATH='/config/bundle-key.pem', TOOLGATE_CONTROL_APPROVAL_ENABLED='true',
            TOOLGATE_CONTROL_APPROVAL_PENDING_TTL_MS='5000')
        env_path = work/'control.env'
        env_path.write_text('\n'.join(k+'='+v for k,v in env.items() if k.startswith(('QUARKUS_', 'MP_JWT_', 'TOOLGATE_CONTROL_')))+'\n', encoding='utf-8')
        env_path.chmod(0o600)
        for path in gateway_work.iterdir(): path.chmod(0o444)
        containers = []
        api = 'http://127.0.0.1:'+db['runtimePort']+'/api/control/v1'
        gateway = 'http://127.0.0.1:'+db['gatewayPort']
        management = 'http://127.0.0.1:'+db['gatewayManagementPort']
        def call(path, credential, body=None, method='GET', key=None):
            status, data, _ = request(api+path, credential, body, method, {'Idempotency-Key':key} if key else None)
            return status, json.loads(data) if data else None
        def outcome(payload):
            status, body, _ = request(gateway+'/v2/authorize', runtime_token, payload, 'POST')
            assert status == 200, (status, body)
            return json.loads(body)
        def consume(permit, payload):
            status, body, _ = request(gateway+'/v1/permits/consume', runtime_token, {'permit':permit, 'request':payload}, 'POST')
            return status, json.loads(body)
        def signed_claims(permit):
            parts = permit['jws'].split('.')
            permit_key.public_key().verify(base64.urlsafe_b64decode(parts[2]+'=='), (parts[0]+'.'+parts[1]).encode(), padding.PKCS1v15(), hashes.SHA256())
            header = json.loads(base64.urlsafe_b64decode(parts[0]+'=='))
            claims = json.loads(base64.urlsafe_b64decode(parts[1]+'=='))
            assert header == {'alg':'RS256', 'typ':'toolgate-execution-permit+jws', 'kid':'permit-key'}
            assert claims['tenantId']=='http-tenant' and claims['userId']=='alice' and claims['agentId']=='agent-demo'
            assert claims['expiresAtUnixMs']-claims['issuedAtUnixMs']<=10000
            return claims
        try:
            control = run(['docker', 'run', '-d', '--read-only', '--cap-drop=ALL', '--security-opt=no-new-privileges',
                '--network', 'container:'+db['container'], '--memory=768m', '--cpus=2',
                '--tmpfs', '/tmp:rw,noexec,nosuid,size=64m,uid=65532,gid=65532', '--env-file', str(env_path),
                '-v', f'{control_work.as_posix()}:/config:ro', ids['control']], capture_output=True, text=True).stdout.strip()
            containers.append(control); ready('http://127.0.0.1:'+db['managementPort'])
            fixture = json.loads((ROOT/'tests/fixtures/contracts/v1/valid.json').read_text())
            tool = {'id':'files.read', 'name':'Files', 'description':'Read exact file', 'actions':[{'name':'read', 'resourceKinds':['FILE']}],
                    'inputSchema':{'type':'object'}, 'outputSchema':{'type':'string'}}
            records = [('users', {'id':'alice', 'name':'Requester', 'enabled':True, 'revision':1}),
                       ('users', {'id':'bob', 'name':'Approver', 'enabled':True, 'revision':1}),
                       ('agents', {'id':'agent-demo', 'name':'Agent', 'enabled':True, 'revision':1, 'ownerUserId':'alice'}),
                       ('tools', {'id':'files.read', 'name':'Files', 'enabled':True, 'revision':1, 'definition':tool}),
                       ('policies', {**fixture['ControlPolicy'], 'id':'ask', 'name':'Human review', 'toolId':'files.read', 'action':'read',
                            'resource':{'kind':'FILE', 'locator':'workspace/readme.txt'}, 'decision':'ASK', 'userIds':['alice'], 'agentIds':['agent-demo']})]
            for index, (kind, record) in enumerate(records): assert call('/'+kind, admin, record, 'POST', 'seed-'+str(index))[0]==201
            def publish(expected):
                revision = call('/config/export', admin)[1]['revision']
                status, bundle = call('/bundles/publish', admin, {'directoryRevision':revision, 'expectedSequence':expected,
                    'lifetimeMs':180000, 'graceMs':0}, 'POST', 'publish-'+str(expected))
                assert status==201, (status, bundle)
                parts = bundle['jws'].split('.')
                policy_key.public_key().verify(base64.urlsafe_b64decode(parts[2]+'=='), (parts[0]+'.'+parts[1]).encode(), padding.PKCS1v15(), hashes.SHA256())
            publish(0)
            gw = run(['docker', 'run', '-d', '--read-only', '--cap-drop=ALL', '--security-opt=no-new-privileges',
                '--network', 'container:'+db['container'], '--memory=256m', '--cpus=1',
                '-v', f'{gateway_work.as_posix()}:/config:ro', '-e', 'TOOLGATE_GATEWAY_CONFIG=/config/gateway.json',
                '-e', 'TOOLGATE_GATEWAY_CREDENTIALS=/config/credentials.json', ids['gateway']], capture_output=True, text=True).stdout.strip()
            containers.append(gw)
            until(lambda:request(management+'/v1/health/ready')[0], 200, 'verified ASK policy')
            payload = {'toolId':'files.read', 'action':'read', 'arguments':{'path':'workspace/readme.txt', 'marker':'once'}}
            pending = outcome(payload); assert pending['decision']['decision']=='ASK' and 'permit' not in pending
            approval_id = pending['approvalId']
            decision = {'decision':'APPROVE_ONCE', 'expectedRevision':1}
            assert call('/approvals/'+approval_id+'/decision', admin, decision, 'POST', 'wrong-role')[0]==403
            assert call('/approvals/'+approval_id+'/decision', requester, decision, 'POST', 'self')[0]==403
            assert call('/approvals/'+approval_id, token(identity, user_id='bob', groups=['toolgate-approver'], tenant_id='other'))[0] in (403,404)
            # Independent decision keys race against the same revision; exactly one wins.
            with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
                replies = list(pool.map(lambda key:call('/approvals/'+approval_id+'/decision', approver, decision, 'POST', key), ['race-a', 'race-b']))
            assert sorted(status for status,_ in replies)==[200,409], replies
            approved = outcome(payload); assert approved['decision']['decision']=='ALLOW'
            claims = signed_claims(approved['permit']); assert claims['approvalId']==approval_id
            assert claims['argumentsDigest']==hashlib.sha256(json.dumps(payload['arguments'],sort_keys=True,separators=(',',':')).encode()).hexdigest()
            altered = {**payload, 'arguments':{**payload['arguments'], 'marker':'changed'}}
            wrong = consume(approved['permit'], altered)
            assert wrong[0]>=400 or wrong[1]['decision']=='BLOCK', wrong
            spent = consume(approved['permit'], payload); assert spent[0]==200 and spent[1]['decision']=='ALLOW', spent
            repeated = consume(approved['permit'], payload); assert repeated[0]>=400 or repeated[1]['decision']=='BLOCK', repeated
            assert outcome(payload)['decision']['decision']=='BLOCK'
            temp_payload = {**payload, 'arguments':{**payload['arguments'], 'marker':'temporary'}}
            temp_pending = outcome(temp_payload); temp_id = temp_pending['approvalId']
            assert call('/approvals/'+temp_id+'/decision', approver, {'decision':'APPROVE_TEMPORARY', 'expectedRevision':1, 'durationMs':20000}, 'POST', 'temporary')[0]==200
            first, second = outcome(temp_payload), outcome(temp_payload)
            assert first['decision']['decision']==second['decision']['decision']=='ALLOW'
            assert signed_claims(first['permit'])['jti'] != signed_claims(second['permit'])['jti']
            run(['docker','pause',control],capture_output=True)
            try: assert outcome(temp_payload)['decision']['decision']=='BLOCK'
            finally: run(['docker','unpause',control],capture_output=True)
            assert outcome(temp_payload)['decision']['decision']=='ALLOW'
            deny_payload = {**payload, 'arguments':{**payload['arguments'], 'marker':'denied'}}
            denied_id = outcome(deny_payload)['approvalId']
            assert call('/approvals/'+denied_id+'/decision', approver, {'decision':'DENY','expectedRevision':1}, 'POST', 'deny')[0]==200
            denied = outcome(deny_payload); assert denied['decision']['decision']=='BLOCK' and denied['approvalId']==denied_id
            expired_payload = {**payload, 'arguments':{**payload['arguments'], 'marker':'expires'}}
            expired_id = outcome(expired_payload)['approvalId']
            until(lambda:call('/approvals/'+expired_id, approver)[1]['state'], 'EXPIRED', 'durable pending expiry')
            assert call('/approvals/'+expired_id+'/decision', approver, {'decision':'APPROVE_ONCE','expectedRevision':1}, 'POST', 'late')[0]==409
            # Revoke policy while a temporary lease is outstanding; old permits never override BLOCK.
            policy = call('/policies/ask', admin)[1]; policy['decision']='BLOCK'
            assert request(api+'/policies/ask',admin,policy,'PUT',{'If-Match':'"'+str(policy['revision'])+'"','Idempotency-Key':'revoke'})[0]==200
            publish(1)
            until(lambda:outcome(temp_payload)['decision']['decision'], 'BLOCK', 'policy revocation')
            invalidated = consume(first['permit'], temp_payload)
            assert invalidated[0]>=400 or invalidated[1]['decision']=='BLOCK'
            audits = call('/audit?limit=100', admin)[1]['items']
            assert any(item['operation']=='APPROVAL_DECIDE' for item in audits)
            assert any(item['operation']=='PERMIT_CONSUME' for item in audits)
            for container in containers:
                logs = run(['docker','logs',container],capture_output=True,text=True).stdout
                for secret in (machine, admin, approver, runtime_token): assert secret not in logs
                assert 'BEGIN PRIVATE KEY' not in logs
            evidence = {'productionImages':True,'realPostgres':True,'askPending':True,'wrongApprover':True,'selfDenied':True,
                'doubleDecision':True,'approveOnce':True,'temporary':True,'denySticky':True,'expiration':True,
                'outageBlocks':True,'permitSignatureBinding':True,'singleUseConsume':True,'revocationBlocks':True,'audit':True,'logsRedacted':True,
                'controlImageId':ids['control'],'gatewayImageId':ids['gateway']}
            (output/'e2e.json').write_text(json.dumps(evidence,indent=2)+'\n',encoding='utf-8')
            print('Production ASK human decisions, exact signed permits, atomic consumption, expiry, outage and revocation passed')
        finally:
            for container in reversed(containers): run(['docker','rm','-f',container],capture_output=True)


if __name__=='__main__': main()
