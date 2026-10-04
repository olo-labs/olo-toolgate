<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical UserRole wire values. */
enum UserRole: string {
    case BASIC = 'BASIC';
    case ADMINISTRATOR = 'ADMINISTRATOR';
    case SUPER_ADMIN = 'SUPER_ADMIN';
}
