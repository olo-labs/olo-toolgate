-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Creator attribution is rechecked against current group management rights at every delivery.
CREATE TABLE control_queued_authority(tenant_id varchar(128) NOT NULL,kind varchar(32) NOT NULL,record_id varchar(128) NOT NULL,creator_user_id varchar(128) NOT NULL,PRIMARY KEY(tenant_id,kind,record_id));
-- statement
GRANT SELECT,INSERT,UPDATE ON control_queued_authority TO toolgate_control_runtime;
