<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical DecisionReason wire values. */
enum DecisionReason: string {
    case MATCHED = 'MATCHED';
    case NO_MATCH = 'NO_MATCH';
    case POLICY_UNAVAILABLE = 'POLICY_UNAVAILABLE';
    case INVALID_INPUT = 'INVALID_INPUT';
    case APPROVAL_REQUIRED = 'APPROVAL_REQUIRED';
}
