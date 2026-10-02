// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.ErrorCode;

/** Stable application failure. Details deliberately exclude database and request payloads. */
public final class Failure extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final ErrorCode code;
    private final int status;
    public Failure(ErrorCode code, int status, String message) {
        super(message); this.code = code; this.status = status;
    }
    public ErrorCode code() { return code; }
    public int status() { return status; }
    public static Failure validation() { return new Failure(ErrorCode.VALIDATION, 400, "Invalid request"); }
    public static Failure conflict() { return new Failure(ErrorCode.CONFLICT, 409, "Revision or configuration conflict"); }
    public static Failure unavailable() { return new Failure(ErrorCode.DEPENDENCY_UNAVAILABLE, 503, "Persistence unavailable"); }
}
