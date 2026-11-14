# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Claim and execute ToolGate workflows using a mounted workload credential."""
import argparse
import json
from pathlib import Path
import time

from .run_transport import ControlRuns, GatewayRuntime, RunTransportError, WorkflowGateway
from .runner import CompletionUnconfirmed, OwnershipLost, Runner


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
        runner = Runner(WorkflowGateway(control, gateway))
        while True:
            lease = control.claim()
            if lease is not None:
                outcome = runner.execute(lease)
                print(json.dumps({"runId": lease.run_id, "workflowId": lease.workflow_id,
                                  "state": outcome.state, "code": outcome.code}), flush=True)
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
