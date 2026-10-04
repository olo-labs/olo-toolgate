<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical ControlEntityKind wire values. */
enum ControlEntityKind: string {
    case USER = 'USER';
    case TEAM = 'TEAM';
    case AGENT = 'AGENT';
    case TOOL = 'TOOL';
    case POLICY = 'POLICY';
    case DEVICE = 'DEVICE';
    case ROLE = 'ROLE';
}
