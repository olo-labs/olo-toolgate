-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Tenant advisory locks serialize these mutable workflows with publication.
CREATE TABLE control_approvals (
    tenant_id varchar(128) NOT NULL,
    approval_id varchar(128) NOT NULL,
    binding_digest char(64) NOT NULL,
    document text NOT NULL CHECK (octet_length(document) <= 16384),
    expires_at bigint NOT NULL CHECK (expires_at BETWEEN 0 AND 9007199254740991),
    PRIMARY KEY (tenant_id, approval_id),
    CHECK (document::jsonb->>'id'=approval_id AND document::jsonb->'input'->'context'->>'tenantId'=tenant_id),
    CHECK ((document::jsonb->>'revision')::bigint BETWEEN 1 AND 9007199254740991),
    CHECK ((document::jsonb->>'expiresAtUnixMs')::bigint=expires_at)
);
-- Binding lookup preserves denied/consumed tombstones until their deadline.
CREATE INDEX control_approval_binding ON control_approvals (tenant_id,binding_digest,expires_at);
CREATE INDEX control_approval_expiry ON control_approvals (tenant_id,expires_at);
CREATE TABLE control_permit_leases (
    tenant_id varchar(128) NOT NULL,
    jti varchar(128) NOT NULL,
    approval_id varchar(128) NOT NULL,
    binding_digest char(64) NOT NULL,
    request_id varchar(128) NOT NULL,
    issued_at bigint NOT NULL,
    expires_at bigint NOT NULL,
    consumed_at bigint,
    PRIMARY KEY (tenant_id,jti),
    FOREIGN KEY (tenant_id,approval_id) REFERENCES control_approvals (tenant_id,approval_id),
    CHECK (issued_at >= 0 AND expires_at > issued_at AND expires_at-issued_at <= 10000),
    CHECK (consumed_at IS NULL OR consumed_at >= issued_at)
);
CREATE INDEX control_permit_expiry ON control_permit_leases (tenant_id,expires_at);
-- Persist the high-water clock across replicas/restarts, so backward time fails closed.
CREATE TABLE control_approval_clocks (
    tenant_id varchar(128) PRIMARY KEY,
    observed_at bigint NOT NULL CHECK (observed_at BETWEEN 0 AND 9007199254740991)
);
GRANT SELECT, INSERT, UPDATE ON control_approvals,control_permit_leases TO toolgate_control_runtime;
GRANT SELECT, INSERT, UPDATE ON control_approval_clocks TO toolgate_control_runtime;
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK (operation IN
    ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK',
     'APPROVAL_CREATE','APPROVAL_DECIDE','APPROVAL_EXPIRE','APPROVAL_SPEND','PERMIT_CONSUME'));
