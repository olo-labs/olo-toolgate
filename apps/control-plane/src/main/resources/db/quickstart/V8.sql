-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_execution_budgets(tenant_id TEXT NOT NULL,budget_id TEXT NOT NULL,invocation_id TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(tenant_id,budget_id,invocation_id));
-- statement
CREATE INDEX control_budget_window ON control_execution_budgets(tenant_id,budget_id,created_at);
