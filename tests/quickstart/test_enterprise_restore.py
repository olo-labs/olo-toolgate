# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Restore authority cannot come from archived keys or revive retired runtime rights."""
from contextlib import closing
import base64,hashlib,importlib.util,json,sqlite3,tempfile,unittest
from pathlib import Path
from cryptography.hazmat.primitives import hashes
from cryptography.hazmat.primitives.asymmetric import padding,rsa
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('restore_gate',ROOT/'tools/enterprise/restore.py');restore=importlib.util.module_from_spec(spec);spec.loader.exec_module(restore)
class RestoreTests(unittest.TestCase):
 def setUp(self):
  self.temp=tempfile.TemporaryDirectory();self.root=Path(self.temp.name);self.source=self.root/'backup';self.data=self.source/'data';(self.data/'state').mkdir(parents=True);self.target=self.root/'restored';self.target.mkdir();self.now=1800000000000
  db=self.data/'state/control.sqlite'
  with closing(sqlite3.connect(db)) as c,c:
   for version in range(1,10):
    script=(ROOT/f'apps/control-plane/src/main/resources/db/quickstart/V{version}.sql').read_text()
    for statement in script.split('-- statement'):
     if statement.strip():c.executescript(statement)
   c.execute("INSERT INTO control_tenants(tenant_id,revision,authorization_epoch) VALUES('tenant',7,9)")
   records=[('USER',dict(id='user',name='User',enabled=True,revision=1)),('TEAM',dict(id='team-default',name='Default',enabled=True,revision=1,userIds=['user'],roleIds=['management'])),('ROLE',dict(id='management',enabled=True,revision=1)),('WORKLOAD_BINDING',dict(id='workload',enabled=True,revision=1,credentialEpoch=3)),('IDENTITY_BINDING',dict(id='identity',enabled=True,revision=1,sessionEpoch=2,sessionsValidAfterUnixMs=0))]
   for kind,doc in records:c.execute('INSERT INTO control_records VALUES(?,?,?,?,?)',('tenant',kind,doc['id'],1,json.dumps(doc)))
   c.execute("INSERT INTO control_enterprise_invocations VALUES('tenant','running',?,'EXECUTING',9,1,2,1,?)",('a'*64,json.dumps(dict(id='running',state='EXECUTING',revision=1))))
   c.execute("INSERT INTO control_enterprise_nonces VALUES('tenant','consumed','running',?,9,2,1)",('a'*64,))
   c.execute("INSERT INTO control_credential_history VALUES('tenant',?)",('f'*64,))
  self.manifest=dict(layoutVersion=1,files={p.relative_to(self.data).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in self.data.rglob('*') if p.is_file()});restore.write(self.source/'manifest.json',self.manifest)
  self.keys=[rsa.generate_private_key(public_exponent=65537,key_size=2048) for _ in range(2)]
  encode=lambda b:base64.urlsafe_b64encode(b).rstrip(b'=').decode()
  number=lambda n:encode(n.to_bytes((n.bit_length()+7)//8,'big'))
  self.trust=dict(keys=[dict(kid='reviewer-'+str(n),n=number(k.public_key().public_numbers().n),e='AQAB') for n,k in enumerate(self.keys)]);restore.write(self.root/'trust.json',self.trust)
  self.authorization=dict(formatVersion=1,backupManifestDigest=hashlib.sha256(restore.canonical(self.manifest)).hexdigest(),tenantId='tenant',directoryRevisionFloor=100,authorizationEpochFloor=200,snapshotSequenceFloor=20,sessionEpochFloor=30,credentialEpochFloor=40,retiredCredentialDigests=['e'*64],retiredRecordIds=[dict(kind='USER',id='post-backup-retired-user')],issuedAtUnixMs=self.now-1000,expiresAtUnixMs=self.now+60000,reasonDigest='a'*64)
  self.proofs=[dict(keyId='reviewer-'+str(n),signature=encode(k.sign(restore.DOMAIN+restore.canonical(self.authorization),padding.PKCS1v15(),hashes.SHA256()))) for n,k in enumerate(self.keys)]
  restore.write(self.root/'review.json',dict(authorization=self.authorization,proofs=self.proofs))
 def tearDown(self):self.temp.cleanup()
 def test_external_floors_quarantine_sessions_credentials_effects_and_management(self):
  restore.restore(self.source,self.target,self.root/'review.json',self.root/'trust.json',self.now)
  with closing(sqlite3.connect(self.target/'state/control.sqlite')) as c,c:
   self.assertEqual(c.execute('SELECT revision,authorization_epoch FROM control_tenants').fetchone(),(101,201));self.assertEqual(c.execute('SELECT snapshot_sequence FROM control_recovery_floors').fetchone()[0],20)
   records={kind:json.loads(doc) for kind,doc in c.execute('SELECT kind,document FROM control_records')}
   self.assertTrue(records['USER']['enabled']);self.assertEqual(records['TEAM']['roleIds'],[]);self.assertFalse(records['ROLE']['enabled']);self.assertFalse(records['WORKLOAD_BINDING']['enabled']);self.assertEqual(records['WORKLOAD_BINDING']['credentialEpoch'],41);self.assertEqual(records['IDENTITY_BINDING']['sessionEpoch'],31)
   self.assertEqual(json.loads(c.execute('SELECT document FROM control_enterprise_invocations').fetchone()[0])['state'],'OUTCOME_UNKNOWN');self.assertEqual(c.execute('SELECT consumed_at FROM control_enterprise_nonces').fetchone()[0],1);self.assertEqual(c.execute('SELECT count(*) FROM control_credential_history').fetchone()[0],2);self.assertEqual(c.execute('SELECT operation FROM control_audit').fetchone()[0],'RESTORE_QUARANTINE')
   self.assertEqual(c.execute("SELECT count(*) FROM control_record_ids WHERE record_id='post-backup-retired-user'").fetchone()[0],1)
 def test_one_reviewer_and_self_selected_archived_trust_are_rejected(self):
  with self.assertRaises(ValueError):restore.validate(dict(authorization=self.authorization,proofs=self.proofs[:1]),self.trust,self.manifest,self.now)
  restore.write(self.source/'trust.json',self.trust)
  with self.assertRaises(ValueError):restore.restore(self.source,self.target,self.root/'review.json',self.source/'trust.json',self.now)
  self.assertEqual(list(self.target.iterdir()),[])
 def test_post_backup_retired_active_user_and_credential_cannot_return(self):
  a={**self.authorization,'retiredRecordIds':[dict(kind='USER',id='user')],'retiredCredentialDigests':['a'*64]}
  with closing(sqlite3.connect(self.data/'state/control.sqlite')) as c,c:
   doc=json.loads(c.execute("SELECT document FROM control_records WHERE kind='WORKLOAD_BINDING'").fetchone()[0]);doc['credentialSha256']='a'*64;c.execute("UPDATE control_records SET document=? WHERE kind='WORKLOAD_BINDING'",(json.dumps(doc),))
   restore.quarantine_connection(c,a,self.now)
   self.assertEqual(c.execute("SELECT count(*) FROM control_records WHERE kind IN ('USER','WORKLOAD_BINDING')").fetchone()[0],0)
   self.assertEqual(json.loads(c.execute("SELECT document FROM control_records WHERE kind='TEAM'").fetchone()[0])['userIds'],[])
   self.assertEqual(c.execute("SELECT count(*) FROM control_credential_history WHERE credential_sha256=?",('a'*64,)).fetchone()[0],1)
   self.assertEqual(c.execute("SELECT count(*) FROM control_record_ids WHERE record_id IN ('user','workload')").fetchone()[0],2)
 def test_tamper_expiry_and_nonempty_restore_never_modify_destination(self):
  with self.assertRaises(ValueError):restore.restore(self.source,self.target,self.root/'review.json',self.root/'trust.json',self.now+60000)
  (self.data/'state/control.sqlite').write_bytes(b'tampered')
  with self.assertRaises(ValueError):restore.restore(self.source,self.target,self.root/'review.json',self.root/'trust.json',self.now)
  self.assertEqual(list(self.target.iterdir()),[])
if __name__=='__main__':unittest.main()
