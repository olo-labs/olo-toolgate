<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical BuilderTestState wire values. */
enum BuilderTestState: string {
    case QUEUED = 'QUEUED';
    case RUNNING = 'RUNNING';
    case PASSED = 'PASSED';
    case FAILED = 'FAILED';
    case EXPIRED = 'EXPIRED';
}
