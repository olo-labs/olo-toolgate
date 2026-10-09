-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_outcome_reconciliations(tenant_id varchar(128) NOT NULL,reconciliation_id varchar(128) NOT NULL,revision bigint NOT NULL,document text NOT NULL,PRIMARY KEY(tenant_id,reconciliation_id));
-- statement
CREATE TABLE control_recovery_floors(tenant_id varchar(128) PRIMARY KEY,snapshot_sequence bigint NOT NULL CHECK(snapshot_sequence>=0));
