// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * The error hierarchy of tool-sdk/spec/authoring/errors.md. Handlers throw these (or return rejected promises with
 * them); the runtime maps each to the result shape of errors.md §3. The CommonJS and ESM entry points resolve to this
 * one module, so `instanceof` works whichever one a tool imports.
 */

/** Base of every error a handler raises. */
export abstract class ToolError extends Error {
  readonly code: string;

  protected constructor(code: string, message: string) {
    super(message);
    this.code = code;
    this.name = new.target.name;
  }
}

/** The arguments are invalid; code INVALID_ARGUMENT. */
export class ToolInputError extends ToolError {
  constructor(message: string) {
    super("INVALID_ARGUMENT", message);
  }
}

/** A business outcome with a code declared in the tool's errors. */
export class ToolBusinessError extends ToolError {
  readonly data: Record<string, unknown>;

  constructor(code: string, message: string, data: Record<string, unknown> = {}) {
    super(code, message);
    this.data = { ...data };
  }
}

/** A transient failure with a declared code; retryable only for READ_ONLY and IDEMPOTENT tools. */
export class ToolRetryableError extends ToolError {
  /** Milliseconds the caller should wait before retrying, when known. */
  readonly retryAfterMs: number | undefined;

  constructor(code: string, message: string, retryAfterMs?: number) {
    super(code, message);
    this.retryAfterMs = retryAfterMs;
  }
}

/** The upstream codes a route call can fail with. */
export type UpstreamCode = "UPSTREAM_4XX" | "UPSTREAM_5XX" | "UPSTREAM_TIMEOUT" | "UPSTREAM_CONNECTION";

/** A route call failed: UPSTREAM_4XX, UPSTREAM_5XX, UPSTREAM_TIMEOUT or UPSTREAM_CONNECTION. */
export class UpstreamError extends ToolError {
  readonly route: string;
  readonly status: number | undefined;
  readonly upstreamRequestId: string | undefined;
  /** True when the request was fully sent and no complete response arrived. */
  readonly effectUncertain: boolean;
  readonly body: string | undefined;

  constructor(
    code: UpstreamCode,
    route: string,
    status: number | undefined,
    upstreamRequestId: string | undefined,
    effectUncertain: boolean,
    body?: string,
  ) {
    super(code, "Upstream call on route " + route + " failed: " + code);
    this.route = route;
    this.status = status;
    this.upstreamRequestId = upstreamRequestId;
    this.effectUncertain = effectUncertain;
    this.body = body;
  }
}

/** A declaration or configuration problem; code CONFIGURATION. */
export class ToolConfigurationError extends ToolError {
  constructor(message: string) {
    super("CONFIGURATION", message);
  }
}

/** The platform codes of errors.md §2. */
export type PlatformCode =
  | "CANCELLED"
  | "DEADLINE_EXCEEDED"
  | "ROUTE_REFUSED"
  | "AUDIT_UNAVAILABLE"
  | "CLAIMS_UNAVAILABLE"
  | "CLIENT_TOOLS_UNAVAILABLE"
  | "UNSUPPORTED_TIER"
  | "CONCURRENCY_LIMIT";

export const PLATFORM_CODES: readonly PlatformCode[] = [
  "CANCELLED",
  "DEADLINE_EXCEEDED",
  "ROUTE_REFUSED",
  "AUDIT_UNAVAILABLE",
  "CLAIMS_UNAVAILABLE",
  "CLIENT_TOOLS_UNAVAILABLE",
  "UNSUPPORTED_TIER",
  "CONCURRENCY_LIMIT",
];

/** Raised by the SDK only. */
export class PlatformError extends ToolError {
  readonly platformCode: PlatformCode;

  constructor(code: PlatformCode, message: string) {
    super(code, message);
    this.platformCode = code;
  }
}
