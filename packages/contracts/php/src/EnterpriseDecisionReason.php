<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseDecisionReason wire values. */
enum EnterpriseDecisionReason: string {
    case MATCHED = 'MATCHED';
    case NO_GRANT = 'NO_GRANT';
    case IDENTITY_DISABLED = 'IDENTITY_DISABLED';
    case INVALID_CONTEXT = 'INVALID_CONTEXT';
    case GROUP_UNAVAILABLE = 'GROUP_UNAVAILABLE';
    case NO_BINDING = 'NO_BINDING';
    case DEVICE_UNTRUSTED = 'DEVICE_UNTRUSTED';
    case RESOURCE_REJECTED = 'RESOURCE_REJECTED';
    case BLOCKED = 'BLOCKED';
    case APPROVAL_REQUIRED = 'APPROVAL_REQUIRED';
    case EVIDENCE_MISSING = 'EVIDENCE_MISSING';
    case EXPIRED = 'EXPIRED';
    case STALE_AUTHORITY = 'STALE_AUTHORITY';
    case QUOTA_EXCEEDED = 'QUOTA_EXCEEDED';
    case VERSION_MISMATCH = 'VERSION_MISMATCH';
    case MANAGEMENT_DENIED = 'MANAGEMENT_DENIED';
}
