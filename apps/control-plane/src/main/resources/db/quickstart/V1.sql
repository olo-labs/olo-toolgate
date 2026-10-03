-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_tenants(tenant_id TEXT PRIMARY KEY, revision INTEGER NOT NULL DEFAULT 0 CHECK(revision BETWEEN 0 AND 9007199254740991));
-- statement
CREATE TABLE control_records(tenant_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN('USER','TEAM','AGENT','TOOL','POLICY','DEVICE')),record_id TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(json_valid(document) AND json_extract(document,'$.id')=record_id AND json_extract(document,'$.revision')=revision),PRIMARY KEY(tenant_id,kind,record_id));
-- statement
CREATE TABLE control_record_ids(tenant_id TEXT NOT NULL,kind TEXT NOT NULL,record_id TEXT NOT NULL,PRIMARY KEY(tenant_id,kind,record_id));
-- statement
CREATE TABLE control_audit(sequence INTEGER PRIMARY KEY AUTOINCREMENT,tenant_id TEXT NOT NULL,actor_id TEXT NOT NULL,operation TEXT NOT NULL,target TEXT NOT NULL,revision INTEGER NOT NULL,request_id TEXT NOT NULL,request_digest TEXT NOT NULL,occurred_at TEXT NOT NULL DEFAULT(strftime('%Y-%m-%dT%H:%M:%fZ','now')));
-- statement
CREATE TABLE control_idempotency(tenant_id TEXT NOT NULL,actor_id TEXT NOT NULL,key TEXT NOT NULL,request_digest TEXT NOT NULL,status INTEGER NOT NULL,body TEXT NOT NULL,revision INTEGER NOT NULL,expires_at INTEGER NOT NULL DEFAULT(unixepoch()+86400),PRIMARY KEY(tenant_id,actor_id,key));
-- statement
CREATE TABLE control_policy_bundles(tenant_id TEXT NOT NULL,sequence INTEGER NOT NULL CHECK(sequence>0),directory_revision INTEGER NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=1500100),policy TEXT NOT NULL CHECK(length(CAST(policy AS BLOB))<=786432),published_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(tenant_id,sequence));
-- statement
CREATE TABLE control_approvals(tenant_id TEXT NOT NULL,approval_id TEXT NOT NULL,binding_digest TEXT NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=16384 AND json_extract(document,'$.id')=approval_id AND json_extract(document,'$.input.context.tenantId')=tenant_id AND json_extract(document,'$.revision') BETWEEN 1 AND 9007199254740991 AND json_extract(document,'$.expiresAtUnixMs')=expires_at),expires_at INTEGER NOT NULL,PRIMARY KEY(tenant_id,approval_id));
-- statement
CREATE TABLE control_permit_leases(tenant_id TEXT NOT NULL,jti TEXT NOT NULL,approval_id TEXT NOT NULL,binding_digest TEXT NOT NULL,request_id TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,consumed_at INTEGER,PRIMARY KEY(tenant_id,jti),FOREIGN KEY(tenant_id,approval_id) REFERENCES control_approvals(tenant_id,approval_id),CHECK(issued_at>=0 AND expires_at>issued_at AND expires_at-issued_at<=10000),CHECK(consumed_at IS NULL OR consumed_at>=issued_at));
-- statement
CREATE TABLE control_approval_clocks(tenant_id TEXT PRIMARY KEY,observed_at INTEGER NOT NULL CHECK(observed_at BETWEEN 0 AND 9007199254740991));
-- statement
CREATE TABLE control_enrollments(tenant_id TEXT NOT NULL,enrollment_id TEXT NOT NULL,code_digest TEXT NOT NULL,device_digest TEXT NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=16384),csr TEXT NOT NULL CHECK(length(CAST(csr AS BLOB))<=16384),user_id TEXT,certificate TEXT CHECK(length(CAST(certificate AS BLOB))<=32768),expires_at INTEGER NOT NULL CHECK(expires_at>=0),last_poll INTEGER NOT NULL DEFAULT 0 CHECK(last_poll>=0),PRIMARY KEY(tenant_id,enrollment_id),UNIQUE(tenant_id,code_digest));
-- statement
CREATE TABLE control_endpoints(tenant_id TEXT NOT NULL,device_id TEXT NOT NULL,key_fingerprint TEXT NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=1048576 AND json_extract(document,'$.deviceId')=device_id AND json_extract(document,'$.tenantId')=tenant_id),csr TEXT NOT NULL CHECK(length(CAST(csr AS BLOB))<=16384),report_digest TEXT NOT NULL,acknowledgment TEXT NOT NULL CHECK(length(CAST(acknowledgment AS BLOB))<=32768),PRIMARY KEY(tenant_id,device_id),UNIQUE(tenant_id,key_fingerprint));
-- statement
CREATE TABLE control_fleet(tenant_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN('RELEASE','DESIRED','ROLLOUT')),record_id TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=131072),PRIMARY KEY(tenant_id,kind,record_id));
-- statement
CREATE TABLE control_builder(tenant_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN('DRAFT','VERSION','TEST')),record_id TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=65536),PRIMARY KEY(tenant_id,kind,record_id));
-- statement
CREATE TRIGGER control_audit_update BEFORE UPDATE ON control_audit BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_audit_delete BEFORE DELETE ON control_audit BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_policy_bundles_update BEFORE UPDATE ON control_policy_bundles BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_policy_bundles_delete BEFORE DELETE ON control_policy_bundles BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_record_ids_update BEFORE UPDATE ON control_record_ids BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_record_ids_delete BEFORE DELETE ON control_record_ids BEGIN SELECT RAISE(ABORT,'append-only local state'); END;
-- statement
CREATE TRIGGER control_fleet_update BEFORE UPDATE ON control_fleet WHEN OLD.kind='RELEASE' OR NEW.tenant_id<>OLD.tenant_id OR NEW.kind<>OLD.kind OR NEW.record_id<>OLD.record_id OR NEW.revision<>OLD.revision+1 BEGIN SELECT RAISE(ABORT,'immutable local version'); END;
-- statement
CREATE TRIGGER control_fleet_delete BEFORE DELETE ON control_fleet  BEGIN SELECT RAISE(ABORT,'local retention violation'); END;
-- statement
CREATE TRIGGER control_builder_update BEFORE UPDATE ON control_builder WHEN OLD.kind='VERSION' OR (OLD.kind='DRAFT' AND json_extract(OLD.document,'$.sealed')=1) OR NEW.tenant_id<>OLD.tenant_id OR NEW.kind<>OLD.kind OR NEW.record_id<>OLD.record_id OR NEW.revision<>OLD.revision+1 BEGIN SELECT RAISE(ABORT,'immutable local version'); END;
-- statement
CREATE TRIGGER control_builder_delete BEFORE DELETE ON control_builder WHEN OLD.kind<>'TEST' OR json_extract(OLD.document,'$.expiresAtUnixMs')>=unixepoch()*1000-604800000 BEGIN SELECT RAISE(ABORT,'local retention violation'); END;
