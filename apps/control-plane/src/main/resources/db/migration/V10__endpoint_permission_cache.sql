-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_endpoint_configurations (
    tenant_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_revision bigint NOT NULL CHECK (source_revision>=0),
    document text NOT NULL CHECK (octet_length(document)<=98304),
    acknowledged_digest char(64),
    local_tools text NOT NULL CHECK (octet_length(local_tools)<=49152),
    PRIMARY KEY (tenant_id,device_id),
    FOREIGN KEY (tenant_id,device_id) REFERENCES control_endpoints(tenant_id,device_id)
);
GRANT SELECT,INSERT,UPDATE ON control_endpoint_configurations TO toolgate_control_runtime;
ALTER TABLE control_endpoints DROP CONSTRAINT control_endpoints_acknowledgment_check;
ALTER TABLE control_endpoints ADD CONSTRAINT control_endpoints_acknowledgment_check CHECK (octet_length(acknowledgment)<=131072);
CREATE TABLE control_mcp_requests (
    tenant_id varchar(128) NOT NULL,
    request_id varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    state varchar(32) NOT NULL CHECK (state IN ('RECEIVED','WAITING_FOR_POLL','SUBMITTED','RESPONSE_RECEIVED','DONE','FAILED','EXPIRED')),
    received_at bigint NOT NULL,
    expires_at bigint NOT NULL,
    record text NOT NULL CHECK (octet_length(record)<=8192),
    task text NOT NULL CHECK (octet_length(task)<=16384),
    result text CHECK (octet_length(result)<=65536),
    PRIMARY KEY (tenant_id,request_id),
    FOREIGN KEY (tenant_id,device_id) REFERENCES control_endpoints(tenant_id,device_id)
);
CREATE INDEX control_mcp_pending ON control_mcp_requests(tenant_id,device_id,state,received_at);
GRANT SELECT,INSERT,UPDATE,DELETE ON control_mcp_requests TO toolgate_control_runtime;
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK (operation IN
 ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK',
  'APPROVAL_CREATE','APPROVAL_DECIDE','APPROVAL_EXPIRE','APPROVAL_SPEND','PERMIT_CONSUME',
  'ENROLLMENT_CREATE','ENROLLMENT_APPROVE','ENROLLMENT_DENY','ENROLLMENT_CONSUME',
  'DEVICE_REVOKE','DEVICE_CHECK_IN','DEVICE_RENEW','PACKAGE_RELEASE','PACKAGE_ASSIGN','ROLLOUT_ADVANCE','ARTIFACT_GRANT',
  'BUILDER_SAVE','BUILDER_TEST','BUILDER_LEASE','BUILDER_RESULT','BUILDER_SEAL','BUILDER_EXPORT',
  'MCP_RECEIVED','MCP_DISPATCHED','MCP_RESULT','MCP_DONE','MCP_FAILED'));
