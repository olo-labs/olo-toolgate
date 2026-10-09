-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_records_groups(tenant_id TEXT NOT NULL,kind TEXT NOT NULL CHECK(kind IN('USER','TEAM','AGENT','TOOL','POLICY','DEVICE','ROLE','DEVICE_GROUP')),record_id TEXT NOT NULL,revision INTEGER NOT NULL CHECK(revision BETWEEN 1 AND 9007199254740991),document TEXT NOT NULL CHECK(json_valid(document) AND json_extract(document,'$.id')=record_id AND json_extract(document,'$.revision')=revision),PRIMARY KEY(tenant_id,kind,record_id));
-- statement
INSERT INTO control_records_groups SELECT * FROM control_records;
-- statement
DROP TABLE control_records;
-- statement
ALTER TABLE control_records_groups RENAME TO control_records;

-- statement
INSERT INTO control_records(tenant_id,kind,record_id,revision,document)
SELECT t.tenant_id,'DEVICE_GROUP','default-devices',1,json_object('id','default-devices','name','Default device group','enabled',json('true'),'revision',1,'deviceIds',json(COALESCE((SELECT json_group_array(record_id) FROM (SELECT record_id FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE' ORDER BY record_id)),'[]'))) FROM control_tenants t;
-- statement
INSERT INTO control_record_ids SELECT tenant_id,kind,record_id FROM control_records WHERE kind='DEVICE_GROUP';
-- statement
UPDATE control_tenants SET revision=revision+1;
