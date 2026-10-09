<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseGrantPurpose wire values. */
enum EnterpriseGrantPurpose: string {
    case HUMAN = 'HUMAN';
    case CAPABILITY = 'CAPABILITY';
    case SERVICE = 'SERVICE';
    case SECRET = 'SECRET';
}
