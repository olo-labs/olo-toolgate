<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseInvocationState wire values. */
enum EnterpriseInvocationState: string {
    case PENDING_APPROVAL = 'PENDING_APPROVAL';
    case QUEUED = 'QUEUED';
    case RESERVED = 'RESERVED';
    case DISPATCHED = 'DISPATCHED';
    case EXECUTING = 'EXECUTING';
    case SUCCEEDED = 'SUCCEEDED';
    case FAILED = 'FAILED';
    case CANCELLED = 'CANCELLED';
    case EXPIRED = 'EXPIRED';
    case PARTIAL = 'PARTIAL';
    case OUTCOME_UNKNOWN = 'OUTCOME_UNKNOWN';
}
