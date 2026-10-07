-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_endpoints_v4(tenant_id TEXT NOT NULL,device_id TEXT NOT NULL,key_fingerprint TEXT NOT NULL,document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=1048576 AND json_extract(document,'$.deviceId')=device_id AND json_extract(document,'$.tenantId')=tenant_id),csr TEXT NOT NULL CHECK(length(CAST(csr AS BLOB))<=16384),report_digest TEXT NOT NULL,acknowledgment TEXT NOT NULL CHECK(length(CAST(acknowledgment AS BLOB))<=131072),PRIMARY KEY(tenant_id,device_id),UNIQUE(tenant_id,key_fingerprint));
-- statement
INSERT INTO control_endpoints_v4 SELECT * FROM control_endpoints;
-- statement
DROP TABLE control_endpoints;
-- statement
ALTER TABLE control_endpoints_v4 RENAME TO control_endpoints;
-- statement
CREATE TABLE control_endpoint_configurations (
    tenant_id TEXT NOT NULL,
    device_id TEXT NOT NULL,
    source_revision INTEGER NOT NULL CHECK (source_revision>=0),
    document TEXT NOT NULL CHECK (length(CAST(document AS BLOB))<=98304),
    acknowledged_digest TEXT,
    local_tools TEXT NOT NULL CHECK (length(CAST(local_tools AS BLOB))<=49152),
    PRIMARY KEY (tenant_id,device_id),
    FOREIGN KEY (tenant_id,device_id) REFERENCES control_endpoints(tenant_id,device_id)
);
-- statement
CREATE TABLE control_mcp_requests (
    tenant_id TEXT NOT NULL, request_id TEXT NOT NULL, device_id TEXT NOT NULL,
    state TEXT NOT NULL CHECK (state IN ('RECEIVED','WAITING_FOR_POLL','SUBMITTED','RESPONSE_RECEIVED','DONE','FAILED','EXPIRED')),
    received_at INTEGER NOT NULL, expires_at INTEGER NOT NULL,
    record TEXT NOT NULL CHECK (length(CAST(record AS BLOB))<=8192),
    task TEXT NOT NULL CHECK (length(CAST(task AS BLOB))<=16384),
    result TEXT CHECK (length(CAST(result AS BLOB))<=65536),
    PRIMARY KEY(tenant_id,request_id),
    FOREIGN KEY(tenant_id,device_id) REFERENCES control_endpoints(tenant_id,device_id)
);
-- statement
CREATE INDEX control_mcp_pending ON control_mcp_requests(tenant_id,device_id,state,received_at);
