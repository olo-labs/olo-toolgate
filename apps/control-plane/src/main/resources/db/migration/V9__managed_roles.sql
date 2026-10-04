-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
ALTER TABLE control_records DROP CONSTRAINT control_records_kind_check;
ALTER TABLE control_records ADD CONSTRAINT control_records_kind_check CHECK(kind IN ('USER','TEAM','AGENT','TOOL','POLICY','DEVICE','ROLE'));
