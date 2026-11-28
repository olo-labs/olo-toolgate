# Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
"""Destination-local processor custody and least-authority setup; Control is scripted."""
from copy import deepcopy
import hashlib
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from bot.business import installation
from tools.admin.control import Refused

ROOT=Path(__file__).resolve().parents[2]


class BusinessConnectionTests(unittest.TestCase):
    def setUp(self):
        temporary=tempfile.TemporaryDirectory();self.addCleanup(temporary.cleanup)
        self.root=Path(temporary.name)
        self.ca=self.root/'ca.pem';self.ca.write_text('-----BEGIN CERTIFICATE-----\nfixture\n-----END CERTIFICATE-----\n')
        self.key=self.root/'api-key';self.key.write_text('generated_'+'x'*40+'\n')
        self.path=self.root/'connection.json'
        self.evidence=self.root/'host.json';self.evidence.write_text('{}')
        self.value=dict(schemaVersion=1,host='business-processor',port=8443,
                        caFile=str(self.ca.resolve()),apiKeyFile=str(self.key.resolve()),privateCidr='172.30.0.0/24',toolHostEvidenceFile=str(self.evidence.resolve()))

    def write(self,value=None):
        self.path.write_text(json.dumps(value if value is not None else self.value))
        return self.path

    def test_current_connection_reads_custody_without_modifying_files(self):
        self.write();before={p.name:p.read_bytes() for p in (self.path,self.ca,self.key)}
        value=installation.connection(self.path)
        self.assertEqual('generated_'+'x'*40,value['secret'])
        self.assertEqual(self.ca.read_bytes().decode('ascii'),value['ca'])
        self.assertEqual('business-processor',value['host'])
        self.assertEqual(before,{p.name:p.read_bytes() for p in (self.path,self.ca,self.key)})

    def test_unsupported_format_destination_and_network_fail_closed(self):
        mutations=[]
        for key,value in [('schemaVersion',True),('schemaVersion',2),('host','attacker.example'),('port',443),
                          ('port',True),('privateCidr','8.8.8.0/24'),('privateCidr','127.0.0.0/8'),
                          ('privateCidr','169.254.0.0/16'),('privateCidr','172.30.0.1/24'),
                          ('caFile','relative-ca.pem'),('apiKeyFile','relative-key')]:
            changed=deepcopy(self.value);changed[key]=value;mutations.append(changed)
        changed=deepcopy(self.value);changed['legacySecret']='never';mutations.append(changed)
        for value in mutations:
            with self.subTest(value=value),self.assertRaises(ValueError):installation.connection(self.write(value))
        with self.assertRaises(Refused):installation.connection(self.root/'missing.json')

    def test_invalid_custody_is_not_rewritten_or_returned(self):
        self.write()
        for raw in ('short','x'*513,'valid_length_'+'x'*32+' bad'):
            self.key.write_text(raw)
            with self.subTest(secret_length=len(raw)),self.assertRaises(Refused):installation.connection(self.path)
            self.assertEqual(raw,self.key.read_text())
        self.key.write_text('x'*40);self.ca.write_text('no certificate here')
        with self.assertRaises(Refused):installation.connection(self.path)
        self.assertEqual('no certificate here',self.ca.read_text())

    @unittest.skipUnless(os.name=='posix','Actual POSIX custody links')
    def test_symlinked_custody_is_refused_before_read(self):
        linked=self.root/'linked-key';linked.symlink_to(self.key)
        changed={**self.value,'apiKeyFile':str(linked)}
        with self.assertRaises(Refused):installation.connection(self.write(changed))

    def test_host_evidence_requires_exact_tenant_placement_and_public_key(self):
        evidence=dict(hostId='business-tool-host',tenantId='verified-tenant',userCode='OBSERVED123',keyFingerprint='a'*64)
        self.write()
        value=installation.connection(self.path)
        self.evidence.write_text(json.dumps(evidence))
        self.assertEqual(evidence,installation.host_evidence(value,'verified-tenant',['business-tool-host']))
        for key,changed in [('tenantId','foreign'),('hostId','other-host'),('keyFingerprint','sha256:'+'a'*64),
                            ('keyFingerprint',None),('userCode','../code'),('userCode',42)]:
            self.evidence.write_text(json.dumps({**evidence,key:changed}))
            with self.subTest(key=key,value=changed),self.assertRaises(Refused):
                installation.host_evidence(value,'verified-tenant',['business-tool-host'])


class BusinessIntakeStagingTests(unittest.TestCase):
    def setUp(self):
        temporary=tempfile.TemporaryDirectory();self.addCleanup(temporary.cleanup)
        self.root=Path(temporary.name);self.folder=self.root/'authored';self.folder.mkdir()
        self.output=self.root/'intakes';self.raw=b'Organization: Fictional supplier\n'
        (self.folder/'source.txt').write_bytes(self.raw)
        self.manifest=dict(schemaVersion=1,useCase='ocr',documents=[dict(id='source',file='source.txt',
            mediaType='text/plain',bytes=len(self.raw),sha256='sha256:'+hashlib.sha256(self.raw).hexdigest())],
            request=dict(fieldSchema=[dict(id='organization',labels=['Organization'])]))
        (self.folder/'sample-input.txt').write_text('starter-ocr-sample\n');self.write_manifest()

    def write_manifest(self):
        (self.folder/'sample-intake.json').write_text(json.dumps(self.manifest))

    def test_fresh_staging_preserves_pinned_bytes_and_refuses_replacement(self):
        self.assertEqual('starter-ocr-sample',installation.stage_intake(self.folder,'sample',self.output))
        destination=self.output/'starter-ocr-sample'
        self.assertEqual(self.raw,(destination/'source.txt').read_bytes())
        self.assertEqual(self.manifest,json.loads((destination/'manifest.json').read_text()))
        original=(destination/'manifest.json').read_bytes()
        with self.assertRaises(Refused):installation.stage_intake(self.folder,'sample',self.output)
        self.assertEqual(original,(destination/'manifest.json').read_bytes())

    def test_changed_hash_size_or_unsafe_paths_create_no_destination(self):
        original=deepcopy(self.manifest)
        for key,value in [('sha256','sha256:'+'0'*64),('bytes',len(self.raw)+1),('file','../private.txt'),('file','manifest.json')]:
            self.manifest=deepcopy(original);self.manifest['documents'][0][key]=value;self.write_manifest()
            with self.subTest(key=key),self.assertRaises(Refused):installation.stage_intake(self.folder,'sample',self.output)
            self.assertFalse(self.output.exists())
        self.manifest=original;self.write_manifest()
        for value in ('../outside','/absolute','starter/ocr'):
            (self.folder/'sample-input.txt').write_text(value)
            with self.subTest(identifier=value),self.assertRaises(Refused):installation.stage_intake(self.folder,'sample',self.output)
            self.assertFalse(self.output.exists())

    @unittest.skipUnless(os.name=='posix','Actual POSIX source links')
    def test_symlink_source_is_refused_without_copying_its_target(self):
        outside=self.root/'private.txt';outside.write_bytes(self.raw)
        (self.folder/'source.txt').unlink();(self.folder/'source.txt').symlink_to(outside)
        with self.assertRaises(Refused):installation.stage_intake(self.folder,'sample',self.output)
        self.assertFalse(self.output.exists());self.assertEqual(self.raw,outside.read_bytes())


class BusinessAuthorityTests(unittest.TestCase):
    def snapshot(self,name='starter-ocr'):
        references=json.loads((ROOT/'bot/configurations'/name/'referenced-config.json').read_text())
        template=next(g for g in references['grants'] if g['id']==name+'-processor-capability')
        return dict(tenantId='destination-tenant',grants=[template])

    def invoke(self,snapshot,profile=None,vault=None):
        writes=[]
        def control(client,path,body=None,*args,**kwargs):
            if path=='/api/control/v1/config/export':return deepcopy(snapshot)
            if path=='/api/control/v1/vault' and body is None:return dict(items=deepcopy(vault or []))
            writes.append((path,deepcopy(body)));return body
        with patch.object(installation,'call',side_effect=control):
            installation.authorize(object(),profile or dict(profileId='starter-ocr',tenantId='forged-source-tenant'),dict(secret='x'*40))
        return writes

    def test_current_grants_bind_only_the_two_verified_tenant_intakes_and_canonical_secret(self):
        snapshot=self.snapshot();original=deepcopy(snapshot)
        writes=self.invoke(snapshot)
        grants=[body for path,body in writes if path=='/api/control/v1/grants']
        self.assertEqual({'CAPABILITY','SERVICE','SECRET'},{g['purpose'] for g in grants})
        expected={'documents://destination-tenant/intake/starter-ocr-sample',
                  'documents://destination-tenant/intake/starter-ocr-exception','secret://businessprocessorapikey'}
        for grant in grants:
            self.assertEqual('AGENT_GROUP',grant['sourceType'])
            self.assertEqual('starter-ocr-agents',grant['sourceId'])
            self.assertEqual(['document.process'],grant['scope']['actions'])
            self.assertFalse(grant['scope']['allActions'])
            self.assertEqual({'ids':['business-processor-tools'],'all':False},grant['scope']['toolGroups'])
            self.assertEqual({'ids':['business-processor-hosts'],'all':False},grant['scope']['deviceGroups'])
            self.assertTrue(all(r['match']=='EXACT' for r in grant['scope']['resources']))
            self.assertEqual({'secret://businessprocessorapikey'} if grant['purpose']=='SECRET' else expected,
                             {r['locator'] for r in grant['scope']['resources']})
        vault=[body for path,body in writes if path=='/api/control/v1/vault']
        self.assertEqual([dict(name='businessprocessorapikey',value='x'*40,toolGroupId='business-processor-tools',deviceGroupId='business-processor-hosts')],vault)
        self.assertEqual(original,snapshot)

    def test_current_existing_grants_and_scoped_secret_are_never_replaced(self):
        snapshot=self.snapshot()
        created=self.invoke(snapshot)
        snapshot['grants'] += [{**g,'revision':9} for path,g in created if path=='/api/control/v1/grants']
        vault=[dict(name='businessprocessorapikey',toolGroupId='business-processor-tools',deviceGroupId='business-processor-hosts')]
        original=deepcopy(snapshot)
        self.assertEqual([],self.invoke(snapshot,vault=vault))
        self.assertEqual(original,snapshot)

    def test_conflicting_existing_grant_prevents_every_setup_write(self):
        snapshot=self.snapshot();created=self.invoke(snapshot)
        service=next(g for path,g in created if path=='/api/control/v1/grants' and g['purpose']=='SERVICE')
        service['scope']['resources']=[dict(kind='CUSTOM',locator='documents://another/intake/secret',match='EXACT')]
        snapshot['grants'].append(service)
        writes=[]
        def control(client,path,body=None,*args,**kwargs):
            if body is None:return deepcopy(snapshot) if path.endswith('/config/export') else dict(items=[])
            writes.append((path,body));return body
        with patch.object(installation,'call',side_effect=control),self.assertRaisesRegex(Refused,'differs'):
            installation.authorize(object(),dict(profileId='starter-ocr'),dict(secret='new-private-key'))
        self.assertEqual([],writes)

    def test_conflicting_existing_secret_scope_preserves_custody_and_creates_no_grants(self):
        snapshot=self.snapshot();writes=[]
        vault=[dict(name='businessprocessorapikey',toolGroupId='another-tools',deviceGroupId='business-processor-hosts')]
        original=deepcopy(vault)
        def control(client,path,body=None,*args,**kwargs):
            if body is None:return deepcopy(snapshot) if path.endswith('/config/export') else dict(items=deepcopy(vault))
            writes.append((path,body));return body
        with patch.object(installation,'call',side_effect=control),self.assertRaisesRegex(Refused,'custody was preserved'):
            installation.authorize(object(),dict(profileId='starter-ocr'),dict(secret='new-private-key'))
        self.assertEqual([],writes);self.assertEqual(original,vault)

    def test_malformed_destination_tenant_or_unrecognized_profile_cannot_create_grants(self):
        for tenant,profile in [('tenant/escape','starter-ocr'),('valid-tenant','arbitrary-profile')]:
            snapshot=self.snapshot();snapshot['tenantId']=tenant
            with patch.object(installation,'call',return_value=snapshot) as call,self.assertRaises(Refused):
                installation.authorize(object(),dict(profileId=profile),dict(secret='x'*40))
            self.assertTrue(all(len(item.args)<3 for item in call.call_args_list))

    def test_deployment_uses_exact_artifact_host_private_range_and_destination_ca(self):
        profile=dict(packages=[dict(packageId='olo.business',packageDigest='sha256:'+'a'*64,hostIds=['tool-host-local'])])
        value=dict(host='business-processor',port=8443,privateCidr='172.30.0.0/24',ca='reviewed-CA',secret='never-deploy-this')
        with patch.object(installation,'call',side_effect=[dict(tenantId='verified-tenant'),{}]) as call:installation.deploy_processor(object(),profile,value)
        self.assertEqual('/api/control/v1/packages/deployments',call.call_args.args[1])
        self.assertEqual(dict(packageDigest='sha256:'+'a'*64,globals=dict(ProcessorHost='business-processor'),hostIds=['tool-host-local'],
            privateRanges=[dict(host='business-processor',cidr='172.30.0.0/24',ports=[8443])],
            destinationTls=dict(processor=dict(caBundle='reviewed-CA',spkiPins=[])),
            effectPolicies=[dict(routeId=route,classification='WRITE',consentRequired=False,acknowledgedUnfencedRisk=False,
                fencedTarget=dict(targetId='document-jobs',targetClass='SHARED_SQL'),
                intent=dict(path='/v1/'+route,query={},headers={},contentType='application/json',bodyMode='CANONICAL_JSON',
                    body=dict(tenantId='verified-tenant',useCase={'$arg':'/useCase'},intakeId={'$arg':'/intakeId'})))
                for route in ('submit','result')]),call.call_args.args[2])
