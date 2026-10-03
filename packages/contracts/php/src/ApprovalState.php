<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical ApprovalState wire values. */
enum ApprovalState: string {
    case PENDING = 'PENDING';
    case APPROVED_ONCE = 'APPROVED_ONCE';
    case APPROVED_TEMPORARY = 'APPROVED_TEMPORARY';
    case DENIED = 'DENIED';
    case EXPIRED = 'EXPIRED';
    case CONSUMED = 'CONSUMED';
}
