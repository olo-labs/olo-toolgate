// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * The runtime surface a handler sees (authoring/typescript.md, "ToolContext"). The type parameters narrow the typed
 * getters to the names a tool declares, so an undeclared name or a getter that does not read the declared type is a
 * compile error; the packager's analyser repeats the check for JavaScript sources, and the runtime refuses it with
 * ToolConfigurationError.
 */

import type { Socket } from "node:net";

/** Value types of authoring/README.md §7. */
export type ValueType =
  | "STRING"
  | "INT"
  | "LONG"
  | "NUMBER"
  | "BOOLEAN"
  | "DATE"
  | "DATE_TIME"
  | "DURATION"
  | "ENUM"
  | "URI"
  | "OBJECT"
  | "ARRAY"
  | "BINARY";

/** Declared names by value type. */
export type ValueMap = Record<string, ValueType>;

/** The value types each getter reads, exactly as the runtime accepts them. */
export type TextTypes = "STRING" | "ENUM" | "URI" | "DATE" | "DATE_TIME" | "DURATION";

/** The names in `M` whose type overlaps `T`; any name when `M` is not narrowed. */
export type NamesOf<M extends ValueMap, T extends ValueType> = string extends keyof M
  ? string
  : { [K in keyof M]: [Extract<M[K], T>] extends [never] ? never : K }[keyof M] & string;

/** Typed reads over declared values. */
export interface Values<M extends ValueMap = ValueMap> {
  getString(name: NamesOf<M, TextTypes>): string;
  getInt(name: NamesOf<M, "INT">): number;
  getLong(name: NamesOf<M, "INT" | "LONG">): number;
  getNumber(name: NamesOf<M, "INT" | "LONG" | "NUMBER">): number;
  getBoolean(name: NamesOf<M, "BOOLEAN">): boolean;
  getDate(name: NamesOf<M, "DATE">): string;
  getDateTime(name: NamesOf<M, "DATE_TIME">): string;
  getDuration(name: NamesOf<M, "DURATION">): string;
  getObject<T = unknown>(name: NamesOf<M, "OBJECT">): T;
  getList<T = unknown>(name: NamesOf<M, "ARRAY">): T[];
  getBinary(name: NamesOf<M, "BINARY">): Uint8Array;
}

/** Declared claims, plus the standard ones of the verified identity. */
export interface Jwt<M extends ValueMap = ValueMap> extends Values<M> {
  readonly userName: string;
  readonly subject: string;
  readonly issuer: string;
}

/** A response with its bytes and headers. */
export interface RawResponse {
  readonly status: number;
  /** Header names in lower case. */
  readonly headers: Record<string, string[]>;
  readonly body: Uint8Array;
}

/** One call on a declared route (http.md §2). */
export interface RouteCall {
  pathVar(name: string, value: unknown): RouteCall;
  /** A null or undefined value is omitted; an array sends the parameter once per item. */
  query(name: string, value: unknown): RouteCall;
  header(name: string, value: string): RouteCall;
  body(value: unknown): RouteCall;
  /** May only shorten the route timeout. */
  timeout(ms: number): RouteCall;
  /** The parsed JSON body of a 2xx response (undefined when empty); any other status raises UpstreamError. */
  send<T = unknown>(): Promise<T>;
  sendRaw(): Promise<RawResponse>;
}

/** The routes HTTP client (http.md). There is no free-form URL. */
export interface HttpClient {
  route(routeId: string): RouteCall;
  get(destinationId: string, pathTemplate: string): RouteCall;
  post(destinationId: string, pathTemplate: string): RouteCall;
  put(destinationId: string, pathTemplate: string): RouteCall;
  patch(destinationId: string, pathTemplate: string): RouteCall;
  delete(destinationId: string, pathTemplate: string): RouteCall;
  head(destinationId: string, pathTemplate: string): RouteCall;
}

/** TCP to the destinations a tool lists (http.md §6). */
export interface TcpConnector {
  connect(destinationId: string): Promise<Socket>;
}

export type InvocationMode = "STANDALONE" | "GOVERNED";

/** The current invocation. `mode` is for diagnostics only; tool code must not branch on it. */
export interface Invocation {
  readonly invocationId: string;
  readonly toolId: string;
  readonly agentId: string | undefined;
  readonly userId: string | undefined;
  readonly catalogRevision: number | undefined;
  readonly deadline: Date;
  readonly businessKey: string | undefined;
  readonly attempt: number;
  readonly mode: InvocationMode;
}

/** A question for the caller: an MRTR round in sync calls, `input_required` in tasks. */
export interface InputRequest<T = unknown> {
  readonly message: string;
  /** A JSON Schema for the answer, as MCP form elicitation takes it. */
  readonly requestedSchema: Record<string, unknown>;
  /** Type marker only. */
  readonly __answer?: T;
}

/** Builds an input request whose answer is typed `T`. */
export function inputRequest<T = Record<string, unknown>>(message: string, requestedSchema: Record<string, unknown>): InputRequest<T> {
  return Object.freeze({ message, requestedSchema });
}

/** The only supported log channel; entries are redacted and size-capped. */
export interface ToolLogger {
  debug(message: string): void;
  info(message: string): void;
  warn(message: string): void;
  error(message: string): void;
}

/** Values declared for the whole package (toolPackage globals, secrets and jwt); augment it to type them. */
// eslint-disable-next-line @typescript-eslint/no-empty-interface
export interface PackageScope {}

type ScopeOf<K extends string> = PackageScope extends { [P in K]: infer M } ? (M extends ValueMap ? M : {}) : {};

/** The context of one invocation, narrowed to the tool's declared inputs, globals, secrets and claims. */
export interface ToolContext<
  I extends ValueMap = ValueMap,
  G extends ValueMap = ValueMap,
  S extends ValueMap = ValueMap,
  C extends ValueMap = ValueMap,
> extends Values<I> {
  readonly global: Values<G & ScopeOf<"globals">>;
  readonly secret: Values<S & ScopeOf<"secrets">>;
  readonly jwt: Jwt<C & ScopeOf<"claims">>;
  readonly http: HttpClient;
  readonly tcp: TcpConnector;
  readonly invocation: Invocation;
  /** Fraction 0..1, monotonic; ignored for sync calls without a progress token. */
  progress(fraction: number, message?: string): void;
  isCancelled(): boolean;
  /** Aborted when the invocation is cancelled or its deadline passes. */
  readonly signal: AbortSignal;
  requestInput<T>(request: InputRequest<T>): Promise<T>;
  readonly log: ToolLogger;
}

const RESULT = Symbol.for("io.ololabs.toolgate/toolResult");

/** An explicit result: MCP content plus optional structured content. A tool returning it has no outputSchema. */
export class ToolResult {
  readonly content: ReadonlyArray<Record<string, unknown>>;
  readonly structuredContent: Record<string, unknown> | undefined;
  readonly [RESULT] = true;

  private constructor(content: ReadonlyArray<Record<string, unknown>>, structuredContent?: Record<string, unknown>) {
    this.content = content.map((c) => ({ ...c }));
    this.structuredContent = structuredContent === undefined ? undefined : { ...structuredContent };
  }

  static of(content: ReadonlyArray<Record<string, unknown>>, structuredContent?: Record<string, unknown>): ToolResult {
    return new ToolResult(content, structuredContent);
  }

  static text(text: string): ToolResult {
    return new ToolResult([{ type: "text", text }]);
  }

  /** Whether `value` is a ToolResult, from this or any other copy of the SDK. */
  static isToolResult(value: unknown): value is ToolResult {
    return typeof value === "object" && value !== null && (value as Record<symbol, unknown>)[RESULT] === true;
  }
}
