<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseConfigurationState wire values. */
enum EnterpriseConfigurationState: string {
    case DRAFT = 'DRAFT';
    case PENDING = 'PENDING';
    case APPROVED = 'APPROVED';
    case DENIED = 'DENIED';
    case CANCELLED = 'CANCELLED';
    case EXPIRED = 'EXPIRED';
    case REVOKED = 'REVOKED';
    case APPLIED = 'APPLIED';
    case STALE = 'STALE';
}
