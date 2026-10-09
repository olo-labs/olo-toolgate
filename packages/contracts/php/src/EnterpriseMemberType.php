<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseMemberType wire values. */
enum EnterpriseMemberType: string {
    case USER = 'USER';
    case AGENT = 'AGENT';
    case TOOL = 'TOOL';
    case DEVICE = 'DEVICE';
}
