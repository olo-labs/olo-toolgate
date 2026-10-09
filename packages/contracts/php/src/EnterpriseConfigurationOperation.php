<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseConfigurationOperation wire values. */
enum EnterpriseConfigurationOperation: string {
    case CREATE = 'CREATE';
    case UPDATE = 'UPDATE';
    case DELETE = 'DELETE';
    case MEMBERSHIPS = 'MEMBERSHIPS';
    case IMPORT = 'IMPORT';
}
