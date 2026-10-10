-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_server_settings(tenant_id TEXT PRIMARY KEY,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(length(CAST(document AS BLOB))<=16384));
