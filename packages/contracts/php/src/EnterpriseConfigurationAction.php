<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseConfigurationAction wire values. */
enum EnterpriseConfigurationAction: string {
    case SUBMIT = 'SUBMIT';
    case APPROVE = 'APPROVE';
    case DENY = 'DENY';
    case REVOKE = 'REVOKE';
    case CANCEL = 'CANCEL';
    case APPLY = 'APPLY';
}
