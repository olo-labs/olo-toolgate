-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
-- Composite primary key serves tenant/kind cursor scans and immutable release lookup.
CREATE TABLE control_fleet (
    tenant_id TEXT NOT NULL,
    kind TEXT NOT NULL CHECK (kind IN ('RELEASE','DESIRED','ROLLOUT')),
    record_id TEXT NOT NULL,
    revision BIGINT NOT NULL CHECK (revision > 0 AND revision <= 9007199254740991),
    document TEXT NOT NULL CHECK (octet_length(document) <= 131072),
    PRIMARY KEY (tenant_id, kind, record_id)
);

-- Immutable published releases and monotonic mutable revisions remain enforced by the database.
CREATE FUNCTION control_fleet_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'DELETE' OR OLD.kind = 'RELEASE' OR NEW.tenant_id <> OLD.tenant_id
      OR NEW.kind <> OLD.kind OR NEW.record_id <> OLD.record_id OR NEW.revision <> OLD.revision + 1 THEN
    RAISE EXCEPTION 'fleet immutable identity or generation violation';
  END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER control_fleet_guard BEFORE UPDATE OR DELETE ON control_fleet
  FOR EACH ROW EXECUTE FUNCTION control_fleet_guard();
GRANT SELECT, INSERT, UPDATE ON control_fleet TO toolgate_control_runtime;
REVOKE DELETE ON control_fleet FROM toolgate_control_runtime;

ALTER TABLE control_audit DROP CONSTRAINT control_audit_operation_check;
ALTER TABLE control_audit ALTER COLUMN operation TYPE varchar(32);
ALTER TABLE control_audit ADD CONSTRAINT control_audit_operation_check CHECK (operation IN
 ('CREATE','UPDATE','DELETE','IMPORT','BUNDLE_PUBLISH','BUNDLE_ROLLBACK',
  'APPROVAL_CREATE','APPROVAL_DECIDE','APPROVAL_EXPIRE','APPROVAL_SPEND','PERMIT_CONSUME',
  'ENROLLMENT_CREATE','ENROLLMENT_APPROVE','ENROLLMENT_DENY','ENROLLMENT_CONSUME',
  'DEVICE_REVOKE','DEVICE_CHECK_IN','DEVICE_RENEW',
  'PACKAGE_RELEASE','PACKAGE_ASSIGN','ROLLOUT_ADVANCE','ARTIFACT_GRANT'));
