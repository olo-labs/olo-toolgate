<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseGroupType wire values. */
enum EnterpriseGroupType: string {
    case TEAM = 'TEAM';
    case AGENT_GROUP = 'AGENT_GROUP';
    case TOOL_GROUP = 'TOOL_GROUP';
    case DEVICE_GROUP = 'DEVICE_GROUP';
}
