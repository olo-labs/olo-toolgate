// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.v2.WorkflowModelDeployment;
import io.ololabs.toolgate.control.domain.Ids;
import java.time.Clock;

/** Control-owned model selection. Configuration and current authority must both permit the deployment. */
public final class WorkflowModelSelection {
    private final WorkflowProfiles profiles;
    private final WorkflowServiceAccess access;
    private final Clock clock;

    public WorkflowModelSelection(WorkflowProfiles profiles, Codec codec, Clock clock) {
        this.profiles = profiles;
        this.access = new WorkflowServiceAccess(codec);
        this.clock = clock;
    }

    public WorkflowModelDeployment resolve(Store.Session tx, Ids.TenantId tenant, String runId, String stepToken) {
        long now = clock.millis();
        var runs = tx.workflowRuns();
        var step = WorkflowLeases.requireStep(runs, runId, stepToken, now);
        if (tx.kills().effective().stream().anyMatch(kill -> "TENANT".equals(kill.scope()))) throw ManagementAccess.denied();
        var run = runs.running(runId);
        if (run == null) throw ManagementAccess.denied();
        var config = profiles.runnableRevision(tx, tenant, run.profileId(), run.profileDigest()).workflows();
        var workflow = config.workflows().stream().filter(item -> item.id().equals(run.workflowId())
            && item.versionId().equals(run.workflowVersionId())).findFirst().orElseThrow(Failure::unavailable);
        var directory = tx.load();
        access.require(directory, run.serviceAgentId(), run.serviceAgentGroupId(), workflow, now);
        access.requireGrant(directory, run.serviceAgentGroupId(), "model.infer", "model-group:" + step.modelGroupId(), now);
        var workflowModels = config.modelGroups().stream().filter(group -> group.id().equals(workflow.modelGroupId()))
            .findFirst().orElseThrow(Failure::unavailable).deploymentIds();
        var stepModels = config.modelGroups().stream().filter(group -> group.id().equals(step.modelGroupId()))
            .findFirst().orElseThrow(Failure::unavailable).deploymentIds();
        var model = workflowModels.stream().filter(stepModels::contains).findFirst().orElseThrow(ManagementAccess::denied);
        return config.modelDeployments().stream().filter(deployment -> deployment.id().equals(model))
            .findFirst().orElseThrow(Failure::unavailable);
    }
}
