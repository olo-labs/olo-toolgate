-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_queued_authority(tenant_id TEXT NOT NULL,kind TEXT NOT NULL,record_id TEXT NOT NULL,creator_user_id TEXT NOT NULL,PRIMARY KEY(tenant_id,kind,record_id));
