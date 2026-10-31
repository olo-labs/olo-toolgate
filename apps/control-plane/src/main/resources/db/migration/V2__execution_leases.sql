-- Copyright 2026 OLO Labs; SPDX-License-Identifier: Apache-2.0
CREATE TABLE control_execution_leases (
    tenant_id varchar(128) NOT NULL REFERENCES control_tenants(tenant_id),
    invocation_id varchar(128) NOT NULL,
    fencing_token varchar(128) NOT NULL,
    device_id varchar(128) NOT NULL,
    expires_at bigint NOT NULL CHECK(expires_at BETWEEN 0 AND 9007199254740991),
    PRIMARY KEY(tenant_id,invocation_id),
    FOREIGN KEY(tenant_id,invocation_id) REFERENCES control_enterprise_invocations(tenant_id,invocation_id)
);
ALTER TABLE control_execution_leases ENABLE ROW LEVEL SECURITY;
ALTER TABLE control_execution_leases FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_access ON control_execution_leases AS PERMISSIVE FOR ALL TO PUBLIC USING(true) WITH CHECK(true);
CREATE POLICY tenant_fence ON control_execution_leases AS RESTRICTIVE FOR ALL TO PUBLIC
    USING (tenant_id=NULLIF(current_setting('app.tenant_id',true),'') AND public.control_tenant_access(false))
    WITH CHECK (tenant_id=NULLIF(current_setting('app.tenant_id',true),'') AND public.control_tenant_access(true));
CREATE TRIGGER tenant_immutable BEFORE UPDATE OF tenant_id ON control_execution_leases FOR EACH ROW EXECUTE FUNCTION public.control_tenant_identity_guard();
GRANT SELECT,INSERT,UPDATE ON control_execution_leases TO toolgate_control_runtime;
GRANT SELECT,INSERT,UPDATE ON control_execution_leases TO toolgate_control_platform;
CREATE FUNCTION control_execution_identity_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.tenant_id<>OLD.tenant_id OR NEW.invocation_id<>OLD.invocation_id OR NEW.fencing_token<>OLD.fencing_token OR NEW.device_id<>OLD.device_id THEN
        RAISE EXCEPTION 'execution identity is immutable';
    END IF;
    RETURN NEW;
END;
$$;
CREATE TRIGGER execution_identity BEFORE UPDATE ON control_execution_leases FOR EACH ROW EXECUTE FUNCTION control_execution_identity_guard();

CREATE TABLE control_execution_evidence (
    tenant_id varchar(128) NOT NULL REFERENCES control_tenants(tenant_id),
    invocation_id varchar(128) NOT NULL,
    fencing_token varchar(128) NOT NULL,
    report_digest char(64) NOT NULL CHECK(report_digest ~ '^[a-f0-9]{64}$'),
    document text NOT NULL CHECK(octet_length(document)<=5242880),
    recorded_at bigint NOT NULL CHECK(recorded_at>=0),
    PRIMARY KEY(tenant_id,invocation_id,fencing_token,report_digest),
    FOREIGN KEY(tenant_id,invocation_id) REFERENCES control_enterprise_invocations(tenant_id,invocation_id)
);
ALTER TABLE control_execution_evidence ENABLE ROW LEVEL SECURITY;
ALTER TABLE control_execution_evidence FORCE ROW LEVEL SECURITY;
CREATE POLICY tenant_access ON control_execution_evidence AS PERMISSIVE FOR ALL TO PUBLIC USING(true) WITH CHECK(true);
CREATE POLICY tenant_fence ON control_execution_evidence AS RESTRICTIVE FOR ALL TO PUBLIC
    USING (tenant_id=NULLIF(current_setting('app.tenant_id',true),'') AND public.control_tenant_access(false))
    WITH CHECK (tenant_id=NULLIF(current_setting('app.tenant_id',true),'') AND public.control_tenant_access(true));
GRANT SELECT,INSERT ON control_execution_evidence TO toolgate_control_runtime,toolgate_control_platform;
CREATE FUNCTION control_execution_evidence_guard() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'execution evidence is immutable';
END;
$$;
CREATE TRIGGER execution_evidence_immutable BEFORE UPDATE OR DELETE ON control_execution_evidence FOR EACH ROW EXECUTE FUNCTION control_execution_evidence_guard();
