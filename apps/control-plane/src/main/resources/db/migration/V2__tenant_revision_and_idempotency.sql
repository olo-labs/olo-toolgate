-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_tenants (
    tenant_id varchar(128) PRIMARY KEY,
    revision bigint NOT NULL DEFAULT 0 CHECK (revision BETWEEN 0 AND 9007199254740991)
);
INSERT INTO control_tenants (tenant_id, revision)
SELECT tenant_id, max(revision) FROM control_records GROUP BY tenant_id;
CREATE TABLE control_idempotency (
    tenant_id varchar(128) NOT NULL,
    actor_id char(64) NOT NULL,
    key varchar(128) NOT NULL,
    request_digest char(64) NOT NULL,
    status integer NOT NULL,
    body text NOT NULL,
    revision bigint NOT NULL,
    expires_at timestamptz NOT NULL DEFAULT (clock_timestamp() + interval '7 days'),
    PRIMARY KEY (tenant_id, actor_id, key)
);
CREATE INDEX control_idempotency_expiry ON control_idempotency (tenant_id, expires_at);
-- Deleted identities remain retired, preventing revision-reset ABA conflicts.
CREATE TABLE control_record_ids (
    tenant_id varchar(128) NOT NULL,
    kind varchar(16) NOT NULL,
    record_id varchar(128) NOT NULL,
    PRIMARY KEY (tenant_id, kind, record_id)
);
INSERT INTO control_record_ids SELECT tenant_id,kind,record_id FROM control_records;
GRANT SELECT, INSERT ON control_record_ids TO toolgate_control_runtime;
GRANT SELECT, INSERT, UPDATE ON control_tenants TO toolgate_control_runtime;
GRANT SELECT, INSERT, DELETE ON control_idempotency TO toolgate_control_runtime;
