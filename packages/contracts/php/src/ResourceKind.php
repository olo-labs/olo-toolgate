<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical ResourceKind wire values. */
enum ResourceKind: string {
    case FILE = 'FILE';
    case URL = 'URL';
    case DATABASE = 'DATABASE';
    case DEVICE = 'DEVICE';
    case CUSTOM = 'CUSTOM';
}
