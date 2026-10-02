-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_policy_bundles (
    tenant_id varchar(128) NOT NULL,
    sequence bigint NOT NULL CHECK (sequence BETWEEN 1 AND 9007199254740991),
    directory_revision bigint NOT NULL CHECK (directory_revision BETWEEN 0 AND 9007199254740991),
    document text NOT NULL CHECK (octet_length(document) <= 1500100),
    policy text NOT NULL CHECK (octet_length(policy) <= 786432),
    published_at timestamptz NOT NULL DEFAULT clock_timestamp(),
    PRIMARY KEY (tenant_id, sequence)
);
-- Append-only privileges protect immutable bytes. Current is max(sequence), so
-- there is no mutable pointer that can accidentally move backwards.
GRANT SELECT, INSERT ON control_policy_bundles TO toolgate_control_runtime;
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check
    CHECK (operation IN ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK'));
