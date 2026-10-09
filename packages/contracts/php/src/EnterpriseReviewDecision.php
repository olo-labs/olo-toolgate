<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseReviewDecision wire values. */
enum EnterpriseReviewDecision: string {
    case APPROVE = 'APPROVE';
    case DENY = 'DENY';
    case REVOKE = 'REVOKE';
}
