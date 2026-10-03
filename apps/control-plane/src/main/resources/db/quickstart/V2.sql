-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE INDEX control_audit_local_cursor ON control_audit(tenant_id,sequence);
-- statement
CREATE INDEX control_approvals_local_cursor ON control_approvals(tenant_id,binding_digest,expires_at);
-- statement
CREATE INDEX control_permit_leases_local_cursor ON control_permit_leases(tenant_id,expires_at);
-- statement
CREATE INDEX control_enrollments_local_cursor ON control_enrollments(tenant_id,expires_at);
-- statement
CREATE INDEX control_idempotency_local_cursor ON control_idempotency(tenant_id,expires_at);

-- statement
CREATE TABLE quickstart_vault(name TEXT PRIMARY KEY CHECK(length(name) BETWEEN 1 AND 128),cipher BLOB NOT NULL CHECK(length(cipher)<=32800));
