# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Claim and execute ToolGate workflows using a mounted workload credential."""
import argparse
import json
from pathlib import Path
import time

from .run_transport import ControlRuns, GatewayRuntime, RunTransportError, WorkflowGateway
from .runner import CompletionUnconfirmed, Denied, OwnershipLost, Runner


class ExecutionEvidence:
    """Bounded adapter observations, without prompts, arguments, results or tokens.

    These observations describe this worker's calls, not independent server attestation.
    """

    def __init__(self, adapter):
        self.adapter = adapter
        self.authorize_step = adapter.authorize_step
        self.discover_agents = adapter.discover_agents
        self.discover_tools = adapter.discover_tools
        self.finish = adapter.finish
        self.inferences = []
        self.tools = []

    def infer(self, lease, grant, **kwargs):
        decision = self.adapter.infer(lease, grant, **kwargs)
        self.inferences.append({"subjectKind": grant.subject_kind, "subjectId": grant.subject_id})
        return decision

    def call_tool(self, lease, grant, tool_id, arguments, **kwargs):
        observation = {"subjectKind": grant.subject_kind, "subjectId": grant.subject_id,
                       "toolId": tool_id, "state": "OUTCOME_UNKNOWN"}
        self.tools.append(observation)
        try:
            outcome = self.adapter.call_tool(lease, grant, tool_id, arguments, **kwargs)
        except Denied:
            observation["state"] = "DENIED"
            raise
        observation["state"] = outcome.state
        return outcome

    def summary(self):
        return {"inferences": self.inferences, "tools": self.tools}


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--control-origin", required=True)
    parser.add_argument("--gateway-origin", required=True)
    parser.add_argument("--token-file", type=Path, required=True)
    parser.add_argument("--control-ca-file")
    parser.add_argument("--gateway-ca-file")
    parser.add_argument("--once", action="store_true")
    args = parser.parse_args(argv)

    def credential():
        with args.token_file.open(encoding="ascii") as stream:
            return stream.read(8193).strip()

    try:
        control = ControlRuns(args.control_origin, credential, ca_file=args.control_ca_file)
        gateway = GatewayRuntime(args.gateway_origin, ca_file=args.gateway_ca_file)
        while True:
            lease = control.claim()
            if lease is not None:
                evidence = ExecutionEvidence(WorkflowGateway(control, gateway))
                runner = Runner(evidence)
                outcome = runner.execute(lease)
                print(json.dumps({"runId": lease.run_id, "workflowId": lease.workflow_id,
                                  "state": outcome.state, "code": outcome.code,
                                  "execution": evidence.summary()}), flush=True)
            if args.once:
                return 0
            if lease is None:
                time.sleep(2)
    except KeyboardInterrupt:
        return 130
    except (RunTransportError, CompletionUnconfirmed, OwnershipLost, OSError, ValueError):
        print(json.dumps({"error": "WORKFLOW_WORKER_STOPPED"}), flush=True)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
