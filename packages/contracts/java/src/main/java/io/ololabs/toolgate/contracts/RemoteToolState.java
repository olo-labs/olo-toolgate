// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Canonical RemoteToolState wire values; unknown values must be rejected. */
public enum RemoteToolState {
    /** Canonical RECEIVED value. */
    RECEIVED,
    /** Canonical WAITING_FOR_POLL value. */
    WAITING_FOR_POLL,
    /** Canonical SUBMITTED value. */
    SUBMITTED,
    /** Canonical RESPONSE_RECEIVED value. */
    RESPONSE_RECEIVED,
    /** Canonical DONE value. */
    DONE,
    /** Canonical FAILED value. */
    FAILED,
    /** Canonical EXPIRED value. */
    EXPIRED
}
