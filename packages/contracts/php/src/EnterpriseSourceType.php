<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseSourceType wire values. */
enum EnterpriseSourceType: string {
    case TEAM = 'TEAM';
    case AGENT_GROUP = 'AGENT_GROUP';
    case ROLE = 'ROLE';
}
