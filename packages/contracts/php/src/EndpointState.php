<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EndpointState wire values. */
enum EndpointState: string {
    case UNENROLLED = 'UNENROLLED';
    case PENDING = 'PENDING';
    case ACTIVE = 'ACTIVE';
    case OFFLINE = 'OFFLINE';
    case REVOKED = 'REVOKED';
}
