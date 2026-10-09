-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
ALTER TABLE control_records DROP CONSTRAINT control_records_kind_check;
-- statement
ALTER TABLE control_records ALTER COLUMN kind TYPE varchar(32);
-- statement
ALTER TABLE control_record_ids ALTER COLUMN kind TYPE varchar(32);
-- statement
ALTER TABLE control_records ADD CONSTRAINT control_records_kind_check CHECK(kind IN ('USER','TEAM','AGENT','TOOL','POLICY','DEVICE','ROLE','DEVICE_GROUP','AGENT_GROUP','TOOL_GROUP','GRANT','DELEGATION','AGENT_DELEGATION','BINDING','EXTRACTOR','WORKLOAD_BINDING','IDENTITY_BINDING','DEVICE_EVIDENCE'));
-- statement
CREATE TABLE control_access_migration_archive(tenant_id varchar(128) NOT NULL,kind varchar(32) NOT NULL,record_id varchar(128) NOT NULL,revision bigint NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,kind,record_id));
-- statement
INSERT INTO control_access_migration_archive SELECT tenant_id,kind,record_id,revision,document::text FROM control_records;
-- statement
UPDATE control_records SET document=document-'access' WHERE kind='USER';
-- statement
UPDATE control_records SET document=document-'allowedToolIds' WHERE kind='AGENT';
-- statement
UPDATE control_records SET document=(document-'deviceIds'-'deviceGroupIds')||'{"roleIds":[]}'::jsonb WHERE kind='TEAM';
-- statement
UPDATE control_records SET document=(document-'deviceGroupId')||'{"enabled":false,"extractorId":"migration-review-extractor","version":"0.0.0","packageDigest":"0000000000000000000000000000000000000000000000000000000000000000"}'::jsonb WHERE kind='TOOL';
-- statement
DELETE FROM control_records WHERE kind IN('POLICY','ROLE');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'TEAM','team-default',1,jsonb_build_object('id','team-default','name','Default team','enabled',true,'revision',1,'userIds',COALESCE((SELECT jsonb_agg(record_id ORDER BY record_id) FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='USER'),'[]'::jsonb),'roleIds','[]'::jsonb) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TEAM' AND r.record_id='team-default');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'AGENT_GROUP','default-agents',1,jsonb_build_object('id','default-agents','name','Default agent group','enabled',true,'revision',1,'agentIds',COALESCE((SELECT jsonb_agg(record_id ORDER BY record_id) FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='AGENT'),'[]'::jsonb),'roleIds','[]'::jsonb) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='AGENT_GROUP' AND r.record_id='default-agents');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'TOOL_GROUP','default-tools',1,jsonb_build_object('id','default-tools','name','Default tool group','enabled',true,'revision',1,'toolIds',COALESCE((SELECT jsonb_agg(record_id ORDER BY record_id) FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TOOL'),'[]'::jsonb)) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='TOOL_GROUP' AND r.record_id='default-tools');
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT t.tenant_id,'DEVICE_GROUP','default-devices',1,jsonb_build_object('id','default-devices','name','Default device group','enabled',true,'revision',1,'deviceIds',COALESCE((SELECT jsonb_agg(record_id ORDER BY record_id) FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE'),'[]'::jsonb)) FROM control_tenants t WHERE NOT EXISTS(SELECT 1 FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE_GROUP' AND r.record_id='default-devices');
-- statement
UPDATE control_records SET document=jsonb_set(document,'{userIds}',COALESCE((SELECT jsonb_agg(r.record_id ORDER BY r.record_id) FROM control_records r WHERE r.tenant_id=control_records.tenant_id AND r.kind='USER'),'[]'::jsonb)) WHERE kind='TEAM' AND record_id='team-default';
-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document) SELECT tenant_id,'EXTRACTOR','migration-review-extractor',1,'{"id":"migration-review-extractor","name":"Migration requires extractor review","enabled":false,"revision":1,"extractorKind":"FIXED","version":"0.0.0","fields":[],"fixedResources":[],"maxResources":1}'::jsonb FROM control_tenants;
-- statement
UPDATE control_records SET document=jsonb_set(document,'{enabled}','true'::jsonb) WHERE (kind='TEAM' AND record_id='team-default') OR (kind='DEVICE_GROUP' AND record_id='default-devices');
-- statement
INSERT INTO control_record_ids SELECT tenant_id,kind,record_id FROM control_records WHERE kind IN('TEAM','AGENT_GROUP','TOOL_GROUP','DEVICE_GROUP','EXTRACTOR') ON CONFLICT DO NOTHING;
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
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
-- statement
ALTER TABLE control_audit ALTER COLUMN operation TYPE varchar(40);
-- statement
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK(operation ~ '^[A-Z][A-Z0-9_]{0,39}$');
-- statement
ALTER TABLE control_policy_bundles DROP CONSTRAINT control_policy_bundles_policy_check;
-- statement
ALTER TABLE control_policy_bundles ADD CONSTRAINT control_policy_bundles_policy_check CHECK(octet_length(policy)<=1048576);
-- statement
CREATE FUNCTION control_epoch_guard() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.revision<OLD.revision OR NEW.authorization_epoch<OLD.authorization_epoch OR NEW.cutover_revision<OLD.cutover_revision THEN RAISE EXCEPTION 'authorization cannot move backwards'; END IF; RETURN NEW; END $$;
-- statement
CREATE TRIGGER control_epoch_guard BEFORE UPDATE ON control_tenants FOR EACH ROW EXECUTE FUNCTION control_epoch_guard();
-- statement
GRANT SELECT ON control_access_migration_archive TO toolgate_control_runtime;
-- statement
GRANT SELECT,INSERT,UPDATE ON control_authorization_outbox,control_enterprise_invocations,control_enterprise_approvals,control_enterprise_nonces,control_enterprise_adoptions TO toolgate_control_runtime;
-- statement
REVOKE ALL ON control_approvals,control_permit_leases FROM toolgate_control_runtime;
-- statement
ALTER TABLE control_policy_bundles DROP CONSTRAINT control_policy_bundles_document_check;
-- statement
ALTER TABLE control_policy_bundles ADD CONSTRAINT control_policy_bundles_document_check CHECK(octet_length(document)<=2000100);

-- statement
CREATE TABLE control_credential_history(tenant_id varchar(128) NOT NULL,credential_sha256 char(64) NOT NULL,PRIMARY KEY(tenant_id,credential_sha256));
-- statement
GRANT SELECT,INSERT ON control_credential_history TO toolgate_control_runtime;
-- statement
CREATE FUNCTION control_credential_immutable() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE EXCEPTION 'immutable credential history'; END $$;
-- statement
CREATE TRIGGER control_credential_history_immutable BEFORE UPDATE OR DELETE ON control_credential_history FOR EACH ROW EXECUTE FUNCTION control_credential_immutable();

-- statement
CREATE TABLE control_reviewed_recovery(tenant_id varchar(128) NOT NULL,review_digest char(64) NOT NULL,PRIMARY KEY(tenant_id,review_digest));
-- statement
GRANT SELECT,INSERT ON control_reviewed_recovery TO toolgate_control_runtime;
-- statement
CREATE TRIGGER control_reviewed_recovery_immutable BEFORE UPDATE OR DELETE ON control_reviewed_recovery FOR EACH ROW EXECUTE FUNCTION control_credential_immutable();

-- statement
CREATE TABLE control_configuration_changes(tenant_id varchar(128) NOT NULL,change_id varchar(128) NOT NULL,revision bigint NOT NULL,request_digest char(64) NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,change_id));
-- statement
GRANT SELECT,INSERT,UPDATE ON control_configuration_changes TO toolgate_control_runtime;

-- statement
DELETE FROM control_endpoint_configurations;

-- statement
CREATE TABLE control_secrets(tenant_id varchar(128) NOT NULL,name varchar(128) NOT NULL,tool_group_id varchar(128) NOT NULL,device_group_id varchar(128) NOT NULL,cipher text NOT NULL CHECK(octet_length(cipher)<=65536),PRIMARY KEY(tenant_id,name));
-- statement
ALTER TABLE control_enterprise_adoptions ADD COLUMN authorization_epoch bigint NOT NULL DEFAULT 0;
-- statement
GRANT SELECT,INSERT,UPDATE ON control_secrets TO toolgate_control_runtime;
