<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical ApprovalChoice wire values. */
enum ApprovalChoice: string {
    case APPROVE_ONCE = 'APPROVE_ONCE';
    case APPROVE_TEMPORARY = 'APPROVE_TEMPORARY';
    case DENY = 'DENY';
}
