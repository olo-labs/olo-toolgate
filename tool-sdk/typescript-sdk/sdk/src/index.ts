// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/** @olo-labs/toolgate-sdk: the authoring API of tool-sdk/spec/authoring/typescript.md. */

export {
  toolPackage,
  serverTool,
  clientTool,
  destination,
  httpAuth,
  httpRoute,
  tcpDestination,
  global,
  secret,
  jwtClaim,
  errorDecl,
  example,
  variable,
  idempotency,
  reconcile,
  isDeclaration,
} from "./declarations";
export type * from "./declarations";
export {
  ToolError,
  ToolInputError,
  ToolBusinessError,
  ToolRetryableError,
  UpstreamError,
  ToolConfigurationError,
  PlatformError,
  PLATFORM_CODES,
} from "./errors";
export type { UpstreamCode, PlatformCode } from "./errors";
export { ToolResult, inputRequest } from "./context";
export type * from "./context";
export { z, binary, sensitive, ZodBinary } from "./zod";
export type { InputMap, ValueTypeOf, ZodObjectLike } from "./zod";
