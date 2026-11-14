// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.application.WorkflowRunStore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

final class JdbcWorkflowRunStore implements WorkflowRunStore {
    private final Connection connection;
    private final String tenant;

    JdbcWorkflowRunStore(Connection connection, String tenant) {
        this.connection = connection;
        this.tenant = tenant;
    }

    private PreparedStatement statement(String sql, String... values) throws SQLException {
        var statement = connection.prepareStatement(sql);
        statement.setString(1, tenant);
        for (int i = 0; i < values.length; i++) statement.setString(i + 2, values[i]);
        return statement;
    }

    @Override public Admission admission(String subscriptionUid, String cursor) {
        try (var statement = statement("""
                SELECT owner_id,admission_id,disposition,run_id FROM control_workflow_admissions
                WHERE tenant_id=? AND subscription_uid=? AND cursor=?
                """, subscriptionUid, cursor); var rows = statement.executeQuery()) {
            return rows.next() ? new Admission(subscriptionUid, cursor, rows.getString(1), rows.getString(2),
                rows.getString(3), rows.getString(4)) : null;
        } catch (SQLException failure) { throw Failure.unavailable(); }
    }

    @Override public void insertAdmission(Admission admission) {
        try (var statement = statement("""
                INSERT INTO control_workflow_admissions(tenant_id,subscription_uid,cursor,owner_id,admission_id,disposition,run_id)
                VALUES(?,?,?,?,?,?,?)
                """, admission.subscriptionUid(), admission.cursor(), admission.ownerId(), admission.admissionId(),
                admission.disposition(), admission.runId())) {
            statement.executeUpdate();
        } catch (SQLException failure) { throw Failure.unavailable(); }
    }

    @Override public void enqueue(QueuedRun run) {
        try (var statement = statement("""
                INSERT INTO control_workflow_runs(tenant_id,run_id,profile_id,profile_digest,workflow_id,workflow_version_id,
                    service_agent_id,service_agent_group_id,remediation_mode,state,document,created_at,key_version)
                VALUES(?,?,?,?,?,?,?,?,?,'QUEUED',?,?,?)
                """, run.id(), run.profileId(), run.profileDigest(), run.workflowId(), run.workflowVersionId(),
                run.serviceAgentId(), run.serviceAgentGroupId(), run.remediationMode(), run.document())) {
            statement.setLong(11, run.createdAt());
            statement.setLong(12, run.keyVersion());
            statement.executeUpdate();
        } catch (SQLException failure) { throw Failure.unavailable(); }
    }

    @Override public QueuedRun queued(String id) {
        try (var statement = statement("""
                SELECT profile_id,profile_digest,workflow_id,workflow_version_id,service_agent_id,service_agent_group_id,
                    remediation_mode,created_at,key_version,document FROM control_workflow_runs
                WHERE tenant_id=? AND run_id=? AND state='QUEUED'
                """, id); var rows = statement.executeQuery()) {
            return rows.next() ? new QueuedRun(id, rows.getString(1), rows.getString(2), rows.getString(3), rows.getString(4),
                rows.getString(5), rows.getString(6), rows.getString(7), rows.getLong(8), rows.getLong(9), rows.getString(10)) : null;
        } catch (SQLException failure) { throw Failure.unavailable(); }
    }
}
