<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical BuilderPermission wire values. */
enum BuilderPermission: string {
    case COMPUTE = 'COMPUTE';
    case FILE_READ = 'FILE_READ';
    case FILE_WRITE = 'FILE_WRITE';
    case NETWORK = 'NETWORK';
    case CREDENTIALS = 'CREDENTIALS';
}
