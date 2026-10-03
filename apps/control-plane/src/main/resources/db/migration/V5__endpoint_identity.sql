-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Tenant locks serialize enrollment decisions, revocation and batched check-in.
CREATE TABLE control_enrollments (
    tenant_id varchar(128) NOT NULL,
    enrollment_id varchar(128) NOT NULL,
    code_digest char(64) NOT NULL,
    device_digest char(64) NOT NULL,
    document text NOT NULL CHECK (octet_length(document)<=16384),
    csr text NOT NULL CHECK (octet_length(csr)<=16384),
    user_id varchar(128),
    certificate text CHECK (octet_length(certificate)<=32768),
    expires_at bigint NOT NULL CHECK (expires_at>=0),
    last_poll bigint NOT NULL DEFAULT 0 CHECK (last_poll>=0),
    PRIMARY KEY (tenant_id,enrollment_id),
    UNIQUE (tenant_id,code_digest)
);
CREATE INDEX control_enrollment_expiry ON control_enrollments (tenant_id,expires_at);
CREATE TABLE control_endpoints (
    tenant_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    key_fingerprint char(64) NOT NULL,
    document text NOT NULL CHECK (octet_length(document)<=1048576),
    csr text NOT NULL CHECK (octet_length(csr)<=16384),
    report_digest char(64) NOT NULL,
    acknowledgment text NOT NULL CHECK (octet_length(acknowledgment)<=32768),
    PRIMARY KEY (tenant_id,device_id),
    UNIQUE (tenant_id,key_fingerprint),
    CHECK (document::jsonb->>'deviceId'=device_id AND document::jsonb->>'tenantId'=tenant_id)
);
-- Only transient enrollment rows are pruned; device identity tombstones are durable.
GRANT SELECT,INSERT,UPDATE,DELETE ON control_enrollments TO toolgate_control_runtime;
GRANT SELECT,INSERT,UPDATE ON control_endpoints TO toolgate_control_runtime;
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ALTER COLUMN operation TYPE varchar(32);
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK (operation IN
 ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK',
  'APPROVAL_CREATE','APPROVAL_DECIDE','APPROVAL_EXPIRE','APPROVAL_SPEND','PERMIT_CONSUME',
  'ENROLLMENT_CREATE','ENROLLMENT_APPROVE','ENROLLMENT_DENY','ENROLLMENT_CONSUME',
  'DEVICE_REVOKE','DEVICE_CHECK_IN','DEVICE_RENEW'));
