-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_execution_leases (
    tenant_id TEXT NOT NULL REFERENCES control_tenants(tenant_id),
    invocation_id TEXT NOT NULL,
    fencing_token TEXT NOT NULL,
    device_id TEXT NOT NULL,
    expires_at INTEGER NOT NULL CHECK(expires_at BETWEEN 0 AND 9007199254740991),
    PRIMARY KEY(tenant_id,invocation_id),
    FOREIGN KEY(tenant_id,invocation_id) REFERENCES control_enterprise_invocations(tenant_id,invocation_id)
);
-- statement
CREATE TRIGGER control_execution_leases_identity_immutable BEFORE UPDATE ON control_execution_leases
WHEN NEW.tenant_id<>OLD.tenant_id OR NEW.invocation_id<>OLD.invocation_id OR NEW.fencing_token<>OLD.fencing_token OR NEW.device_id<>OLD.device_id
BEGIN SELECT RAISE(ABORT,'execution identity is immutable'); END;
-- statement
CREATE TABLE control_execution_evidence (
    tenant_id TEXT NOT NULL REFERENCES control_tenants(tenant_id),
    invocation_id TEXT NOT NULL,
    fencing_token TEXT NOT NULL,
    report_digest TEXT NOT NULL CHECK(length(report_digest)=64 AND report_digest NOT GLOB '*[^a-f0-9]*'),
    document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=5242880),
    recorded_at INTEGER NOT NULL CHECK(recorded_at>=0),
    PRIMARY KEY(tenant_id,invocation_id,fencing_token,report_digest),
    FOREIGN KEY(tenant_id,invocation_id) REFERENCES control_enterprise_invocations(tenant_id,invocation_id)
);
-- statement
CREATE TRIGGER control_execution_evidence_no_update BEFORE UPDATE ON control_execution_evidence
BEGIN SELECT RAISE(ABORT,'execution evidence is immutable'); END;
-- statement
CREATE TRIGGER control_execution_evidence_no_delete BEFORE DELETE ON control_execution_evidence
BEGIN SELECT RAISE(ABORT,'execution evidence is immutable'); END;
