# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
import json
from email.message import Message
from io import BytesIO
import unittest

from toolgate_runtime.run_transport import ControlRuns, RunTransportError
from toolgate_runtime.runner import RunOutcome


class Response(BytesIO):
    def __init__(self, status, value):
        super().__init__(json.dumps(value).encode())
        self.status = status
        self.headers = Message()
        self.headers["Content-Type"] = "application/json"


class RunTransportTests(unittest.TestCase):
    def setUp(self):
        self.control = ControlRuns("https://control.example", lambda: "workload.credential")
        self.control._opener = self
        self.requests = []
        self.snapshot = {
            "runId": "run-1", "leaseId": "lease-1", "workflowId": "workflow-1", "workflowVersionId": "v1",
            "profileDigest": "sha256:" + "1" * 64, "leaseToken": "x" * 43, "message": "test",
            "supervisorPrompt": "Delegate", "supervisorModelGroupId": "models", "contextSources": [],
            "limits": {"maxSteps": 4, "maxHandoffs": 1, "maxDurationSeconds": 20},
            "agents": [{"agentId": "agent-1", "modelGroupId": "models", "systemPrompt": "Handle",
                        "contextSources": [], "entryEligible": True, "delegationAllowed": True}],
            "remediationMode": "PREPARE", "expiresAtUnixMs": 1,
        }
        self.responses = []

    def open(self, request, timeout):
        self.requests.append((request.full_url, request.get_header("Authorization"), json.loads(request.data), timeout))
        return self.responses.pop(0)

    def test_claim_and_finish_use_the_workload_credential_and_lease(self):
        self.responses.extend([Response(200, self.snapshot), Response(200, {"runId": "run-1", "leaseId": "lease-1", "state": "SUCCEEDED"})])
        lease = self.control.claim()
        self.assertEqual("agent-1", lease.agents[0].agent_id)
        ack = self.control.finish(lease, RunOutcome("SUCCEEDED"), timeout_seconds=5)
        self.assertEqual("SUCCEEDED", ack.state)
        self.assertEqual([
            ("https://control.example/internal/v1/runtime/claim", "Bearer workload.credential", {}, 10),
            ("https://control.example/internal/v1/runtime/runs/run-1/finish", "Bearer workload.credential",
             {"leaseId": "lease-1", "leaseToken": "x" * 43, "state": "SUCCEEDED"}, 5),
        ], self.requests)

    def test_empty_queue_and_unexpected_snapshot(self):
        self.responses.extend([Response(204, None), Response(200, dict(self.snapshot, modelOrigin="https://model.example"))])
        self.assertIsNone(self.control.claim())
        with self.assertRaisesRegex(RunTransportError, "Invalid Control run snapshot"):
            self.control.claim()

    def test_step_authorization_is_bound_to_the_claimed_run(self):
        self.responses.extend([Response(200, self.snapshot), Response(200, {"runId": "run-1", "leaseId": "lease-1",
            "subjectKind": "AGENT", "subjectId": "agent-1", "modelGroupId": "models", "token": "y" * 43})])
        lease = self.control.claim()
        grant = self.control.authorize_step(lease, "AGENT", "agent-1", "models", timeout_seconds=4)
        self.assertEqual("y" * 43, grant.token)
        self.assertEqual(("https://control.example/internal/v1/runtime/runs/run-1/steps:authorize", "Bearer workload.credential",
                          {"leaseId": "lease-1", "leaseToken": "x" * 43, "subjectKind": "AGENT", "subjectId": "agent-1",
                           "modelGroupId": "models"}, 4), self.requests[-1])
