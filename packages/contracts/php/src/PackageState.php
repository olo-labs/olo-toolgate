<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical PackageState wire values. */
enum PackageState: string {
    case ABSENT = 'ABSENT';
    case STAGING = 'STAGING';
    case READY = 'READY';
    case FAILED = 'FAILED';
    case REVOKED = 'REVOKED';
}
