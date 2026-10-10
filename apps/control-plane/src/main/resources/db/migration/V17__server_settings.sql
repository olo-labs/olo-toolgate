-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Tenant control-plane settings, such as bounded device auto-approval.
CREATE TABLE control_server_settings(tenant_id varchar(128) PRIMARY KEY,revision bigint NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document text NOT NULL CHECK(octet_length(document)<=16384));

GRANT SELECT,INSERT,UPDATE ON control_server_settings TO toolgate_control_runtime;
