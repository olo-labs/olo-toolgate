<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical RemoteToolState wire values. */
enum RemoteToolState: string {
    case RECEIVED = 'RECEIVED';
    case WAITING_FOR_POLL = 'WAITING_FOR_POLL';
    case SUBMITTED = 'SUBMITTED';
    case RESPONSE_RECEIVED = 'RESPONSE_RECEIVED';
    case DONE = 'DONE';
    case FAILED = 'FAILED';
    case EXPIRED = 'EXPIRED';
}
