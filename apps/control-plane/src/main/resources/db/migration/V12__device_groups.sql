-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
ALTER TABLE control_records DROP CONSTRAINT control_records_kind_check;
ALTER TABLE control_records ADD CONSTRAINT control_records_kind_check CHECK(kind IN ('USER','TEAM','AGENT','TOOL','POLICY','DEVICE','ROLE','DEVICE_GROUP'));
INSERT INTO control_records(tenant_id,kind,record_id,revision,document)
SELECT t.tenant_id,'DEVICE_GROUP','default-devices',1,jsonb_build_object('id','default-devices','name','Default device group','enabled',true,'revision',1,'deviceIds',COALESCE((SELECT jsonb_agg(record_id ORDER BY record_id) FROM control_records r WHERE r.tenant_id=t.tenant_id AND r.kind='DEVICE'),'[]'::jsonb)) FROM control_tenants t;
INSERT INTO control_record_ids SELECT tenant_id,kind,record_id FROM control_records WHERE kind='DEVICE_GROUP';
UPDATE control_tenants SET revision=revision+1;
