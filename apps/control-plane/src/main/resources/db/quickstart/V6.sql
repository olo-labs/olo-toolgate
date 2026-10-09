-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
DROP TRIGGER control_policy_bundles_update;
-- statement
DROP TRIGGER control_policy_bundles_delete;
-- statement
CREATE TABLE control_policy_bundles_enterprise(tenant_id TEXT NOT NULL,sequence INTEGER NOT NULL CHECK(sequence>0),directory_revision INTEGER NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=2000100),policy TEXT NOT NULL CHECK(length(CAST(policy AS BLOB))<=1048576),published_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(tenant_id,sequence));
-- statement
INSERT INTO control_policy_bundles_enterprise SELECT * FROM control_policy_bundles;
-- statement
DROP TABLE control_policy_bundles;
-- statement
ALTER TABLE control_policy_bundles_enterprise RENAME TO control_policy_bundles;
-- statement
CREATE TRIGGER control_policy_bundles_update BEFORE UPDATE ON control_policy_bundles BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_policy_bundles_delete BEFORE DELETE ON control_policy_bundles BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TABLE control_records_enterprise(tenant_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN('USER','TEAM','AGENT','TOOL','POLICY','DEVICE','ROLE','DEVICE_GROUP','AGENT_GROUP','TOOL_GROUP','GRANT','DELEGATION','AGENT_DELEGATION','BINDING','EXTRACTOR','WORKLOAD_BINDING','IDENTITY_BINDING','DEVICE_EVIDENCE')),record_id TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(json_valid(document) AND json_extract(document,'$.id')=record_id AND json_extract(document,'$.revision')=revision),PRIMARY KEY(tenant_id,kind,record_id));
-- statement
INSERT INTO control_records_enterprise SELECT * FROM control_records;
-- statement
DROP TABLE control_records;
-- statement
ALTER TABLE control_records_enterprise RENAME TO control_records;
-- statement
CREATE TABLE control_access_migration_archive(tenant_id varchar(128) NOT NULL,kind varchar(32) NOT NULL,record_id varchar(128) NOT NULL,revision bigint NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,kind,record_id));
-- statement
INSERT INTO control_access_migration_archive SELECT tenant_id,kind,record_id,revision,document FROM control_records;
-- statement
UPDATE control_records SET document=json_remove(document,'$.access') WHERE kind='USER';
-- statement
UPDATE control_records SET document=json_remove(document,'$.allowedToolIds') WHERE kind='AGENT';
-- statement
UPDATE control_records SET document=json_set(json_remove(document,'$.deviceIds','$.deviceGroupIds'),'$.roleIds',json('[]')) WHERE kind='TEAM';
-- statement
UPDATE control_records SET document=json_set(json_remove(document,'$.deviceGroupId'),'$.enabled',json('false'),'$.extractorId','migration-review-extractor','$.version','0.0.0','$.packageDigest','0000000000000000000000000000000000000000000000000000000000000000') WHERE kind='TOOL';
-- statement
DELETE FROM control_records WHERE kind IN('POLICY','ROLE');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'TEAM','team-default',1,json_object('id','team-default','name','Default team','enabled',json('true'),'revision',1,'userIds',json(COALESCE((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='USER' ORDER BY record_id)),'[]')),'roleIds',json('[]')) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TEAM' AND r.record_id='team-default');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'AGENT_GROUP','default-agents',1,json_object('id','default-agents','name','Default agent group','enabled',json('true'),'revision',1,'agentIds',json(COALESCE((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='AGENT' ORDER BY record_id)),'[]')),'roleIds',json('[]')) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='AGENT_GROUP' AND r.record_id='default-agents');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'TOOL_GROUP','default-tools',1,json_object('id','default-tools','name','Default tool group','enabled',json('true'),'revision',1,'toolIds',json(COALESCE((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TOOL' ORDER BY record_id)),'[]'))) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TOOL_GROUP' AND r.record_id='default-tools');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'DEVICE_GROUP','default-devices',1,json_object('id','default-devices','name','Default device group','enabled',json('true'),'revision',1,'deviceIds',json(COALESCE((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE' ORDER BY record_id)),'[]'))) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE_GROUP' AND r.record_id='default-devices');
-- statement
UPDATE control_records SET document=json_set(document,'$.userIds',json((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=control_records.tenant_id AND r.kind='USER' ORDER BY record_id)))) WHERE kind='TEAM' AND record_id='team-default';
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT tenant_id,'EXTRACTOR','migration-review-extractor',1,'{"id":"migration-review-extractor","name":"Migration requires extractor review","enabled":false,"revision":1,"extractorKind":"FIXED","version":"0.0.0","fields":[],"fixedResources":[],"maxResources":1}' FROM control_tenants;
-- statement
UPDATE control_records SET document=json_set(document,'$.enabled',json('true')) WHERE (kind='TEAM' AND record_id='team-default') OR (kind='DEVICE_GROUP' AND record_id='default-devices');
-- statement
INSERT OR IGNORE INTO control_record_ids SELECT tenant_id,kind,record_id FROM control_records WHERE kind IN('TEAM','AGENT_GROUP','TOOL_GROUP','DEVICE_GROUP','EXTRACTOR');
-- statement
ALTER TABLE control_tenants ADD COLUMN authorization_epoch bigint NOT NULL DEFAULT 0 CHECK(authorization_epoch BETWEEN 0 AND 9007199254740991);
-- statement
ALTER TABLE control_tenants ADD COLUMN cutover_revision bigint NOT NULL DEFAULT 0 CHECK(cutover_revision BETWEEN 0 AND 9007199254740991);
-- statement
UPDATE control_tenants SET revision=revision+1,authorization_epoch=revision+1,cutover_revision=revision+1;
-- statement
CREATE TABLE control_authorization_outbox(tenant_id varchar(128) NOT NULL,revision bigint NOT NULL,authorization_epoch bigint NOT NULL,occurred_at bigint NOT NULL,published_at bigint,PRIMARY KEY(tenant_id,revision));
-- statement
CREATE TABLE control_enterprise_invocations(tenant_id varchar(128) NOT NULL,invocation_id varchar(128) NOT NULL,request_digest char(64) NOT NULL,state varchar(32) NOT NULL,authorization_epoch bigint NOT NULL,created_at bigint NOT NULL,expires_at bigint NOT NULL,revision bigint NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,invocation_id));
-- statement
CREATE INDEX control_enterprise_invocation_window ON control_enterprise_invocations(tenant_id,created_at);
-- statement
CREATE TABLE control_enterprise_approvals(tenant_id varchar(128) NOT NULL,approval_id varchar(128) NOT NULL,approval_type varchar(16) NOT NULL CHECK(approval_type IN('OPERATION','CONFIGURATION')),invocation_id varchar(128) NOT NULL,request_digest char(64) NOT NULL,authorization_epoch bigint NOT NULL,expires_at bigint NOT NULL,revision bigint NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,approval_id));
-- statement
CREATE TABLE control_enterprise_nonces(tenant_id varchar(128) NOT NULL,nonce varchar(128) NOT NULL,invocation_id varchar(128) NOT NULL,evaluation_digest char(64) NOT NULL,authorization_epoch bigint NOT NULL,expires_at bigint NOT NULL,consumed_at bigint,PRIMARY KEY(tenant_id,nonce));
-- statement
CREATE TABLE control_enterprise_adoptions(tenant_id varchar(128) NOT NULL,actor_type varchar(16) NOT NULL,actor_id varchar(128) NOT NULL,directory_revision bigint NOT NULL,snapshot_sequence bigint NOT NULL,graph_digest char(64) NOT NULL,observed_at bigint NOT NULL,PRIMARY KEY(tenant_id,actor_type,actor_id));
-- statement
CREATE TRIGGER control_access_archive_update BEFORE UPDATE ON control_access_migration_archive BEGIN SELECT RAISE(ABORT,'immutable migration evidence'); END;
-- statement
CREATE TRIGGER control_access_archive_delete BEFORE DELETE ON control_access_migration_archive BEGIN SELECT RAISE(ABORT,'immutable migration evidence'); END;
-- statement
CREATE TRIGGER control_epoch_guard BEFORE UPDATE ON control_tenants WHEN NEW.revision<OLD.revision OR NEW.authorization_epoch<OLD.authorization_epoch OR NEW.cutover_revision<OLD.cutover_revision BEGIN SELECT RAISE(ABORT,'authorization cannot move backwards'); END;

-- statement
CREATE TABLE control_credential_history(tenant_id TEXT NOT NULL,credential_sha256 TEXT NOT NULL CHECK(length(credential_sha256)=64),PRIMARY KEY(tenant_id,credential_sha256));
-- statement
CREATE TRIGGER control_credential_history_immutable_update BEFORE UPDATE ON control_credential_history BEGIN SELECT RAISE(ABORT,'immutable credential history'); END;
-- statement
CREATE TRIGGER control_credential_history_immutable_delete BEFORE DELETE ON control_credential_history BEGIN SELECT RAISE(ABORT,'immutable credential history'); END;

-- statement
CREATE TABLE control_reviewed_recovery(tenant_id TEXT NOT NULL,review_digest TEXT NOT NULL CHECK(length(review_digest)=64),PRIMARY KEY(tenant_id,review_digest));
-- statement
CREATE TRIGGER control_reviewed_recovery_immutable_update BEFORE UPDATE ON control_reviewed_recovery BEGIN SELECT RAISE(ABORT,'immutable reviewed recovery'); END;
-- statement
CREATE TRIGGER control_reviewed_recovery_immutable_delete BEFORE DELETE ON control_reviewed_recovery BEGIN SELECT RAISE(ABORT,'immutable reviewed recovery'); END;

-- statement
CREATE TABLE control_configuration_changes(tenant_id varchar(128) NOT NULL,change_id varchar(128) NOT NULL,revision bigint NOT NULL,request_digest char(64) NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,change_id));

-- statement
DELETE FROM control_endpoint_configurations;

-- statement
CREATE TABLE control_secrets(tenant_id varchar(128) NOT NULL,name varchar(128) NOT NULL,tool_group_id varchar(128) NOT NULL,device_group_id varchar(128) NOT NULL,cipher text NOT NULL CHECK(length(cipher)<=65536),PRIMARY KEY(tenant_id,name));
-- statement
ALTER TABLE control_enterprise_adoptions ADD COLUMN authorization_epoch bigint NOT NULL DEFAULT 0;
