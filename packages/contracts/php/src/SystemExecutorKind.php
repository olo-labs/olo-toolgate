<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical SystemExecutorKind wire values. */
enum SystemExecutorKind: string {
    case BUILTINS = 'BUILTINS';
    case HOTFOLDER = 'HOTFOLDER';
    case REST_FORWARDING = 'REST_FORWARDING';
}
