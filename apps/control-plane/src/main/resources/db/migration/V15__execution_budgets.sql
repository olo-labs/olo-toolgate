-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Per complete grant/delegation/policy tuple, shared by that group's callers across Gateway replicas.
CREATE TABLE control_execution_budgets(tenant_id varchar(128) NOT NULL,budget_id varchar(128) NOT NULL,invocation_id varchar(128) NOT NULL,created_at bigint NOT NULL,PRIMARY KEY(tenant_id,budget_id,invocation_id));
-- statement
CREATE INDEX control_budget_window ON control_execution_budgets(tenant_id,budget_id,created_at);
-- statement
GRANT SELECT,INSERT ON control_execution_budgets TO toolgate_control_runtime;
