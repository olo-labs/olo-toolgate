# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Workload-authenticated transport for the Control run ledger."""
from __future__ import annotations

import json
import re
from urllib.error import HTTPError
from urllib.request import Request

from .runner import Agent, CompletionAck, ContextSource, ID, Limits, OwnershipLost, RunLease, StepGrant
from .trigger_bridge import _constant, _object
from .trigger_transport import ControlTriggers


class RunTransportError(Exception):
    """A bounded error that carries no response body or credential."""


def _record(value, fields):
    if not isinstance(value, dict) or set(value) != set(fields.split()):
        raise ValueError("Invalid runtime response")
    return value


def _contexts(values):
    if not isinstance(values, list) or len(values) > 16:
        raise ValueError("Invalid context sources")
    contexts = []
    for value in values:
        source = _record(value, "toolId arguments")
        contexts.append(ContextSource(source["toolId"], source["arguments"]))
    return tuple(contexts)


def _agent(value):
    agent = _record(value, "agentId modelGroupId systemPrompt contextSources entryEligible delegationAllowed")
    return Agent(agent["agentId"], agent["modelGroupId"], agent["systemPrompt"], _contexts(agent["contextSources"]),
                 agent["entryEligible"], agent["delegationAllowed"])


def _lease(value):
    data = _record(value, "runId leaseId workflowId workflowVersionId profileDigest leaseToken message supervisorPrompt "
                         "supervisorModelGroupId limits agents contextSources remediationMode expiresAtUnixMs")
    if (not isinstance(data["leaseToken"], str) or not re.fullmatch(r"[A-Za-z0-9_-]{43}", data["leaseToken"])
            or type(data["expiresAtUnixMs"]) is not int or data["expiresAtUnixMs"] <= 0
            or not isinstance(data["agents"], list) or not 1 <= len(data["agents"]) <= 32):
        raise ValueError("Invalid run lease")
    limits = _record(data["limits"], "maxSteps maxHandoffs maxDurationSeconds")
    return RunLease(data["runId"], data["leaseId"], data["workflowId"], data["workflowVersionId"], data["profileDigest"],
                    data["leaseToken"], data["message"], data["supervisorPrompt"], data["supervisorModelGroupId"],
                    Limits(limits["maxSteps"], limits["maxHandoffs"], limits["maxDurationSeconds"]),
                    tuple(map(_agent, data["agents"])), _contexts(data["contextSources"]), data["remediationMode"])


class ControlRuns(ControlTriggers):
    """Reuse the Control TLS policy with a separate workload credential."""

    def _runtime_request(self, path, payload, *, timeout_seconds, allow_empty=False):
        try:
            token = self._token()
            if not isinstance(token, str) or not re.fullmatch(r"[A-Za-z0-9_.-]{1,8192}", token):
                raise ValueError("Invalid workload credential")
            timeout = min(self.timeout, timeout_seconds)
            if not 0 < timeout <= 30:
                raise ValueError("Invalid runtime timeout")
            request = Request(self.origin + "/internal/v1/runtime/" + path,
                              json.dumps(payload, allow_nan=False).encode(),
                              {"Authorization": "Bearer " + token, "Content-Type": "application/json",
                               "Accept": "application/json"}, method="POST")
            with self._opener.open(request, timeout=timeout) as response:
                if allow_empty and response.status == 204:
                    return None
                if response.status != 200 or response.headers.get_content_type() != "application/json":
                    raise ValueError("Invalid runtime response")
                raw = response.read(262145)
                if len(raw) > 262144:
                    raise ValueError("Runtime response exceeds limit")
                return json.loads(raw, object_pairs_hook=_object, parse_constant=_constant)
        except HTTPError as failure:
            with failure:
                if failure.code in (401, 403, 409) and not allow_empty:
                    raise OwnershipLost("Run lease is unavailable") from None
            raise RunTransportError("Control rejected the runtime request") from None
        except Exception:
            raise RunTransportError("Control runtime request failed") from None

    def claim(self):
        value = self._runtime_request("claim", {}, timeout_seconds=self.timeout, allow_empty=True)
        if value is None:
            return None
        try:
            return _lease(value)
        except (ValueError, TypeError):
            raise RunTransportError("Invalid Control run snapshot") from None

    @staticmethod
    def _run_path(lease, operation):
        if not isinstance(lease.run_id, str) or not ID.fullmatch(lease.run_id) or any(c in lease.run_id for c in ":/"):
            raise RunTransportError("Invalid run identity")
        return f"runs/{lease.run_id}/{operation}"

    def authorize_step(self, lease, subject_kind, subject_id, model_group_id, *, timeout_seconds):
        value = self._runtime_request(self._run_path(lease, "steps:authorize"),
                                      {"leaseId": lease.lease_id, "leaseToken": lease.lease_token, "subjectKind": subject_kind,
                                       "subjectId": subject_id, "modelGroupId": model_group_id}, timeout_seconds=timeout_seconds)
        try:
            grant = _record(value, "runId leaseId subjectKind subjectId modelGroupId token")
            if not isinstance(grant["token"], str) or not re.fullmatch(r"[A-Za-z0-9_-]{43}", grant["token"]):
                raise ValueError("Invalid step token")
            return StepGrant(grant["runId"], grant["leaseId"], grant["subjectKind"], grant["subjectId"],
                             grant["modelGroupId"], grant["token"])
        except ValueError:
            raise RunTransportError("Invalid Control step grant") from None

    def finish(self, lease, outcome, *, timeout_seconds):
        value = self._runtime_request(self._run_path(lease, "finish"),
                                      {"leaseId": lease.lease_id, "leaseToken": lease.lease_token, "state": outcome.state},
                                      timeout_seconds=timeout_seconds)
        try:
            ack = _record(value, "runId leaseId state")
            return CompletionAck(ack["runId"], ack["leaseId"], ack["state"])
        except ValueError:
            raise RunTransportError("Invalid Control completion acknowledgement") from None
