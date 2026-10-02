-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Provision the NOLOGIN role toolgate_control_runtime before running migrations.
CREATE TABLE control_records (
    tenant_id varchar(128) NOT NULL,
    kind varchar(16) NOT NULL CHECK (kind IN ('USER','TEAM','AGENT','TOOL','POLICY','DEVICE')),
    record_id varchar(128) NOT NULL,
    revision bigint NOT NULL CHECK (revision BETWEEN 1 AND 9007199254740991),
    document jsonb NOT NULL CHECK (jsonb_typeof(document) = 'object'),
    PRIMARY KEY (tenant_id, kind, record_id),
    CHECK (document->>'id' = record_id AND (document->>'revision')::bigint = revision)
);
CREATE TABLE control_audit (
    sequence bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    tenant_id varchar(128) NOT NULL,
    actor_id char(64) NOT NULL,
    operation varchar(16) NOT NULL CHECK (operation IN ('CREATE','UPDATE','DELETE','IMPORT')),
    target varchar(256) NOT NULL,
    revision bigint NOT NULL,
    request_id varchar(128) NOT NULL,
    request_digest char(64) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT clock_timestamp()
);
CREATE INDEX control_audit_tenant_cursor ON control_audit (tenant_id, sequence);
GRANT USAGE ON SCHEMA public TO toolgate_control_runtime;
GRANT SELECT, INSERT, UPDATE, DELETE ON control_records TO toolgate_control_runtime;
GRANT SELECT, INSERT ON control_audit TO toolgate_control_runtime;
GRANT USAGE ON SEQUENCE control_audit_sequence_seq TO toolgate_control_runtime;
