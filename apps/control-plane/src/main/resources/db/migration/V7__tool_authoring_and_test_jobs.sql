-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Authoring state is tenant scoped and shares audit/replay transactions with fleet state.
CREATE TABLE control_builder (
 tenant_id TEXT NOT NULL,
 kind TEXT NOT NULL CHECK (kind IN ('DRAFT','VERSION','TEST')),
 record_id TEXT NOT NULL,
 revision BIGINT NOT NULL CHECK (revision > 0 AND revision <= 9007199254740991),
 document TEXT NOT NULL CHECK (octet_length(document) <= 65536),
 PRIMARY KEY (tenant_id,kind,record_id)
);
CREATE FUNCTION control_builder_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF TG_OP='DELETE' THEN
  IF OLD.kind='TEST' AND (OLD.document::jsonb->>'expiresAtUnixMs')::bigint <
    (extract(epoch from clock_timestamp())*1000)::bigint-604800000 THEN RETURN OLD; END IF;
  RAISE EXCEPTION 'authoring retention violation';
 END IF;
 IF OLD.kind='VERSION' OR (OLD.kind='DRAFT' AND (OLD.document::jsonb->>'sealed')::boolean)
  OR NEW.tenant_id<>OLD.tenant_id OR NEW.kind<>OLD.kind OR NEW.record_id<>OLD.record_id
  OR NEW.revision<>OLD.revision+1 THEN RAISE EXCEPTION 'authoring immutable identity violation'; END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER control_builder_guard BEFORE UPDATE OR DELETE ON control_builder FOR EACH ROW EXECUTE FUNCTION control_builder_guard();
GRANT SELECT,INSERT,UPDATE,DELETE ON control_builder TO toolgate_control_runtime;
ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK (operation IN
 ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK',
  'APPROVAL_CREATE','APPROVAL_DECIDE','APPROVAL_EXPIRE','APPROVAL_SPEND','PERMIT_CONSUME',
  'ENROLLMENT_CREATE','ENROLLMENT_APPROVE','ENROLLMENT_DENY','ENROLLMENT_CONSUME',
  'DEVICE_REVOKE','DEVICE_CHECK_IN','DEVICE_RENEW','PACKAGE_RELEASE','PACKAGE_ASSIGN','ROLLOUT_ADVANCE','ARTIFACT_GRANT',
  'BUILDER_SAVE','BUILDER_TEST','BUILDER_LEASE','BUILDER_RESULT','BUILDER_SEAL','BUILDER_EXPORT'));
