<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical EnterpriseExtractorKind wire values. */
enum EnterpriseExtractorKind: string {
    case FIXED = 'FIXED';
    case FIELDS = 'FIELDS';
    case FILESYSTEM = 'FILESYSTEM';
    case NETWORK = 'NETWORK';
    case SQL = 'SQL';
    case SHELL = 'SHELL';
}
