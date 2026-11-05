// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * The declaration functions of authoring/typescript.md. Each returns a frozen, branded object; the packager reads the
 * exported module-level constants that hold them, checks that their arguments are literals, and turns them into
 * descriptors. The const type parameters keep declared names and types as literal types, so `ToolContext` narrows its
 * getters to what a tool declares.
 */

import type { z } from "zod";
import type { InputMap, ZodObjectLike } from "./zod";
import type { ToolContext, ValueType } from "./context";
import type { ToolResult } from "./context";

export const DECLARATION = Symbol.for("io.ololabs.toolgate/declaration");

export type EffectSafety = "READ_ONLY" | "IDEMPOTENT" | "NON_IDEMPOTENT" | "UNKNOWN";
export type KeyNamespace = "AGENT" | "DELEGATED_USER" | "TENANT";
export type RuntimeTier = "OCI_SANDBOX" | "WASI" | "HOST_NATIVE";
export type HttpMethod = "GET" | "HEAD" | "POST" | "PUT" | "PATCH" | "DELETE";
export type Streaming = "NONE" | "RESPONSE" | "BIDIRECTIONAL";
export type AuthType = "API_KEY" | "BASIC" | "OAUTH2_CLIENT_CREDENTIALS" | "TOKEN_EXCHANGE" | "CONNECTION" | "CUSTOM";
export type SecretScope = "GROUP" | "USER";
export type IdempotencyScope = "BUSINESS_KEY" | "PER_INVOCATION";

/** Which declaration a branded object is. */
export type DeclarationKind =
  | "toolPackage"
  | "serverTool"
  | "clientTool"
  | "destination"
  | "httpAuth"
  | "httpRoute"
  | "tcpDestination"
  | "global"
  | "secret"
  | "jwtClaim"
  | "errorDecl"
  | "example"
  | "variable"
  | "idempotency"
  | "reconcile";

export interface Branded<K extends DeclarationKind> {
  readonly [DECLARATION]: K;
}

/** Constraint attributes shared by Global and Variable (authoring README §4). */
export interface Constraints {
  /** Parsed as the value type (INT, LONG, NUMBER). */
  readonly min?: number | string;
  readonly max?: number | string;
  readonly minLength?: number;
  readonly maxLength?: number;
  /** ECMA-262. */
  readonly pattern?: string;
  /** Allowed values in declaration order; makes a STRING an ENUM. */
  readonly enumValues?: readonly string[];
  /** JSON Schema format, STRING only. */
  readonly format?: string;
  /** Redacted from logs, errors and audit. */
  readonly sensitive?: boolean;
}

export interface ToolPackageSpec {
  readonly id: string;
  readonly name: string;
  readonly publisher: string;
  readonly description?: string;
  /** Package-scope declarations; they apply to every tool. */
  readonly globals?: readonly GlobalDeclaration[];
  readonly secrets?: readonly SecretDeclaration[];
  readonly jwt?: readonly JwtClaimDeclaration[];
}
export type ToolPackageDeclaration = ToolPackageSpec & Branded<"toolPackage">;

export interface DestinationSpec {
  readonly id: string;
  readonly hostSetting: string;
  /** Proposed value only; never part of the descriptor. */
  readonly defaultHost?: string;
  /** Default 443. */
  readonly port?: number;
  readonly spkiPins?: readonly string[];
  readonly caBundle?: string;
}
export type DestinationDeclaration = DestinationSpec & Branded<"destination">;

export interface HttpAuthSpec {
  readonly id: string;
  readonly type: AuthType;
  readonly header?: string;
  readonly secret?: string;
  readonly connection?: string;
  readonly scopes?: readonly string[];
  readonly audience?: string;
}
export type HttpAuthDeclaration = HttpAuthSpec & Branded<"httpAuth">;

export interface HttpRouteSpec {
  readonly id: string;
  readonly destination: string;
  readonly method: HttpMethod;
  /** RFC 6570 level 1; every variable carries a pattern, {name:regex}. */
  readonly path: string;
  readonly query?: readonly string[];
  readonly headers?: readonly string[];
  /** Default ["application/json"]. */
  readonly requestContentTypes?: readonly string[];
  /** Default 1048576. */
  readonly maxRequestBytes?: number;
  /** Default 4194304. */
  readonly maxResponseBytes?: number;
  readonly streaming?: Streaming;
  /** Default 10000. */
  readonly timeoutMs?: number;
  readonly auth?: string;
}
export type HttpRouteDeclaration = HttpRouteSpec & Branded<"httpRoute">;

export interface TcpDestinationSpec {
  readonly id: string;
  readonly hostSetting: string;
  readonly defaultHost?: string;
  readonly port: number;
}
export type TcpDestinationDeclaration = TcpDestinationSpec & Branded<"tcpDestination">;

export interface GlobalSpec<N extends string = string, T extends ValueType = ValueType> extends Constraints {
  readonly name: N;
  readonly type: T;
  readonly defaultValue?: string;
  /** Default true. */
  readonly required?: boolean;
  readonly description?: string;
}
export type GlobalDeclaration<N extends string = string, T extends ValueType = ValueType> = GlobalSpec<N, T> & Branded<"global">;

export interface SecretSpec<N extends string = string, T extends ValueType = ValueType> {
  readonly name: N;
  /** Default STRING. */
  readonly type?: T;
  /** Default GROUP. */
  readonly scope?: SecretScope;
  readonly description?: string;
}
export type SecretDeclaration<N extends string = string, T extends ValueType = ValueType> = SecretSpec<N, T> & Branded<"secret">;

export interface JwtClaimSpec<N extends string = string, T extends ValueType = ValueType> {
  readonly name: N;
  /** A JSON pointer or a top-level claim name. */
  readonly claim: string;
  /** Default STRING. */
  readonly type?: T;
  /** Default true. */
  readonly required?: boolean;
}
export type JwtClaimDeclaration<N extends string = string, T extends ValueType = ValueType> = JwtClaimSpec<N, T> &
  Branded<"jwtClaim">;

export interface ErrorDeclSpec {
  readonly code: string;
  readonly description: string;
  readonly retryable?: boolean;
  /** JSON Schema for the error data, as a JSON string or an object literal. */
  readonly dataSchema?: string | Readonly<Record<string, unknown>>;
}
export type ErrorDeclaration = ErrorDeclSpec & Branded<"errorDecl">;

export interface ExampleSpec {
  /** A JSON object, as a JSON string or an object literal. */
  readonly arguments: string | Readonly<Record<string, unknown>>;
  readonly expectedOutput?: string | Readonly<Record<string, unknown>>;
  /** An error code. */
  readonly expectedError?: string;
  /** Recorded exchanges file (testing.md), relative to the project directory. */
  readonly exchanges?: string;
}
export type ExampleDeclaration = ExampleSpec & Branded<"example">;

export interface VariableSpec<N extends string = string, T extends ValueType = ValueType> extends Constraints {
  readonly name: N;
  readonly description: string;
  readonly type: T;
  /** Default true. */
  readonly required?: boolean;
  readonly defaultValue?: string;
}
export type VariableDeclaration<N extends string = string, T extends ValueType = ValueType> = VariableSpec<N, T> &
  Branded<"variable">;

export interface IdempotencySpec {
  readonly header: string;
  /** Default BUSINESS_KEY. */
  readonly scope?: IdempotencyScope;
}
export type IdempotencyDeclaration = IdempotencySpec & Branded<"idempotency">;

export interface ReconcileSpec {
  /** Local name of the tool this handler reconciles. */
  readonly tool: string;
}
export type ReconcileDeclaration = ReconcileSpec & Branded<"reconcile">;

type Named = { readonly name: string; readonly type?: ValueType };

/** Declared name to value type, from a list of globals, secrets, claims or variables. */
export type MapOf<D extends readonly Named[] | undefined, Default extends ValueType = "STRING"> = D extends readonly Named[]
  ? { [E in D[number] as E["name"]]: E["type"] extends ValueType ? E["type"] : Default }
  : {};

/** The grouped form (authoring README §4). */
export interface ToolVariables<
  VI extends readonly VariableDeclaration[] = readonly VariableDeclaration[],
  VG extends readonly GlobalDeclaration[] = readonly GlobalDeclaration[],
  VC extends readonly JwtClaimDeclaration[] = readonly JwtClaimDeclaration[],
  VS extends readonly SecretDeclaration[] = readonly SecretDeclaration[],
> {
  readonly inputs?: VI;
  readonly globals?: VG;
  readonly jwt?: VC;
  readonly secrets?: VS;
}

/** The members ServerTool and ClientTool share. */
export interface ToolCommon {
  readonly name: string;
  /** Exactly one of title and text. */
  readonly title?: string;
  readonly text?: string;
  readonly description: string;
  readonly useWhen?: string;
  /** Default UNKNOWN. */
  readonly effect?: EffectSafety;
  readonly destructive?: boolean;
  /** Default: true for server tools with routes. */
  readonly openWorld?: boolean;
  readonly async?: boolean;
  /** Default 30000 sync, 900000 async. */
  readonly maxDurationMs?: number;
  readonly stateless?: boolean;
  readonly maxConcurrency?: number;
  /** Default: true when NON_IDEMPOTENT and destructive. */
  readonly requiresBusinessKey?: boolean;
  /** Default AGENT. */
  readonly businessKeyNamespace?: KeyNamespace;
  readonly businessKeyArgument?: string;
  readonly cancelIfAbandoned?: boolean;
  readonly errors?: readonly ErrorDeclaration[];
  readonly toolsets?: readonly string[];
  readonly examples?: readonly ExampleDeclaration[];
  readonly idempotency?: IdempotencyDeclaration;
  readonly reconcile?: ReconcileDeclaration;
  /** Optional Zod schema of the result; inferred from the handler's return type otherwise. */
  readonly output?: z.ZodTypeAny;
}

type Out<O> = O extends z.ZodTypeAny ? z.input<O> | ToolResult : unknown;
type Awaitable<T> = T | Promise<T>;

/** A handler's context type, from its declarations. */
export type ContextOf<
  IM,
  G extends readonly GlobalDeclaration[] | undefined,
  S extends readonly SecretDeclaration[] | undefined,
  C extends readonly JwtClaimDeclaration[] | undefined,
> = ToolContext<IM & Record<never, ValueType>, MapOf<G, never>, MapOf<S>, MapOf<C>>;

/** Typed form: `input` is a Zod object and `run(args, ctx)`. */
export interface TypedServerToolSpec<
  I extends ZodObjectLike,
  G extends readonly GlobalDeclaration[] | undefined,
  S extends readonly SecretDeclaration[] | undefined,
  C extends readonly JwtClaimDeclaration[] | undefined,
  O extends z.ZodTypeAny | undefined,
> extends ToolCommon {
  readonly routes?: readonly string[];
  readonly tcp?: readonly string[];
  readonly globals?: G;
  readonly secrets?: S;
  readonly jwt?: C;
  readonly input: I;
  readonly variables?: never;
  readonly output?: O;
  readonly run: (args: z.output<I>, ctx: ContextOf<InputMap<I>, G, S, C>) => Awaitable<Out<O>>;
}

/** Grouped form, or no inputs: `run(ctx)`. */
export interface GroupedServerToolSpec<
  VI extends readonly VariableDeclaration[] | undefined,
  G extends readonly GlobalDeclaration[] | undefined,
  S extends readonly SecretDeclaration[] | undefined,
  C extends readonly JwtClaimDeclaration[] | undefined,
  VG extends readonly GlobalDeclaration[] | undefined,
  VS extends readonly SecretDeclaration[] | undefined,
  VC extends readonly JwtClaimDeclaration[] | undefined,
  O extends z.ZodTypeAny | undefined,
> extends ToolCommon {
  readonly routes?: readonly string[];
  readonly tcp?: readonly string[];
  readonly globals?: G;
  readonly secrets?: S;
  readonly jwt?: C;
  readonly input?: never;
  readonly variables?: { readonly inputs?: VI; readonly globals?: VG; readonly secrets?: VS; readonly jwt?: VC };
  readonly output?: O;
  readonly run: (
    ctx: ToolContext<
      MapOf<VI> & Record<never, ValueType>,
      MapOf<G, never> & MapOf<VG, never>,
      MapOf<S> & MapOf<VS>,
      MapOf<C> & MapOf<VC>
    >,
  ) => Awaitable<Out<O>>;
}

/** The runtime view of a tool declaration, as the descriptor builder and the runtime read it. */
export interface ToolDeclaration extends ToolCommon {
  readonly [DECLARATION]: "serverTool" | "clientTool";
  readonly routes?: readonly string[];
  readonly tcp?: readonly string[];
  readonly tiers?: readonly RuntimeTier[];
  readonly globals?: readonly GlobalDeclaration[];
  readonly secrets?: readonly SecretDeclaration[];
  readonly jwt?: readonly JwtClaimDeclaration[];
  readonly input?: ZodObjectLike;
  readonly variables?: ToolVariables;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  readonly run: (...args: any[]) => unknown;
}

/** Client-only members; routes, tcp, secrets and HTTP auth are refused. */
interface ClientOnly {
  /** Default [OCI_SANDBOX]. */
  readonly tiers?: readonly RuntimeTier[];
  readonly routes?: never;
  readonly tcp?: never;
  readonly secrets?: never;
}

function brand<K extends DeclarationKind, T extends object>(kind: K, spec: T, what: string): T & Branded<K> {
  if (spec === null || typeof spec !== "object" || Array.isArray(spec)) {
    throw new TypeError(what + "() takes an object literal");
  }
  const copy: Record<string | symbol, unknown> = {};
  for (const [k, v] of Object.entries(spec)) copy[k] = Array.isArray(v) ? Object.freeze([...v]) : v;
  Object.defineProperty(copy, DECLARATION, { value: kind, enumerable: false });
  return Object.freeze(copy) as T & Branded<K>;
}

/** Whether `value` is a declaration of the given kind, from any copy of the SDK. */
export function isDeclaration<K extends DeclarationKind>(value: unknown, kind?: K): value is Branded<K> {
  if (value === null || typeof value !== "object") return false;
  const k = (value as Record<symbol, unknown>)[DECLARATION];
  return typeof k === "string" && (kind === undefined || k === kind);
}

/** The kind of a declaration, or undefined. */
export function declarationKind(value: unknown): DeclarationKind | undefined {
  return isDeclaration(value) ? ((value as unknown as Record<symbol, unknown>)[DECLARATION] as DeclarationKind) : undefined;
}

export function toolPackage(spec: ToolPackageSpec): ToolPackageDeclaration {
  return brand("toolPackage", spec, "toolPackage");
}

export function serverTool<
  I extends ZodObjectLike,
  const G extends readonly GlobalDeclaration[] | undefined = undefined,
  const S extends readonly SecretDeclaration[] | undefined = undefined,
  const C extends readonly JwtClaimDeclaration[] | undefined = undefined,
  O extends z.ZodTypeAny | undefined = undefined,
>(spec: TypedServerToolSpec<I, G, S, C, O>): ToolDeclaration;
export function serverTool<
  const VI extends readonly VariableDeclaration[] | undefined = undefined,
  const G extends readonly GlobalDeclaration[] | undefined = undefined,
  const S extends readonly SecretDeclaration[] | undefined = undefined,
  const C extends readonly JwtClaimDeclaration[] | undefined = undefined,
  const VG extends readonly GlobalDeclaration[] | undefined = undefined,
  const VS extends readonly SecretDeclaration[] | undefined = undefined,
  const VC extends readonly JwtClaimDeclaration[] | undefined = undefined,
  O extends z.ZodTypeAny | undefined = undefined,
>(spec: GroupedServerToolSpec<VI, G, S, C, VG, VS, VC, O>): ToolDeclaration;
export function serverTool(spec: object): ToolDeclaration {
  return brand("serverTool", spec, "serverTool") as unknown as ToolDeclaration;
}

export function clientTool<
  I extends ZodObjectLike,
  const G extends readonly GlobalDeclaration[] | undefined = undefined,
  const C extends readonly JwtClaimDeclaration[] | undefined = undefined,
  O extends z.ZodTypeAny | undefined = undefined,
>(spec: Omit<TypedServerToolSpec<I, G, undefined, C, O>, "routes" | "tcp" | "secrets"> & ClientOnly): ToolDeclaration;
export function clientTool<
  const VI extends readonly VariableDeclaration[] | undefined = undefined,
  const G extends readonly GlobalDeclaration[] | undefined = undefined,
  const C extends readonly JwtClaimDeclaration[] | undefined = undefined,
  const VG extends readonly GlobalDeclaration[] | undefined = undefined,
  const VC extends readonly JwtClaimDeclaration[] | undefined = undefined,
  O extends z.ZodTypeAny | undefined = undefined,
>(
  spec: Omit<GroupedServerToolSpec<VI, G, undefined, C, VG, undefined, VC, O>, "routes" | "tcp" | "secrets" | "variables"> &
    ClientOnly & { readonly variables?: { readonly inputs?: VI; readonly globals?: VG; readonly jwt?: VC; readonly secrets?: never } },
): ToolDeclaration;
export function clientTool(spec: object): ToolDeclaration {
  return brand("clientTool", spec, "clientTool") as unknown as ToolDeclaration;
}

export function destination(spec: DestinationSpec): DestinationDeclaration {
  return brand("destination", spec, "destination");
}

export function httpAuth(spec: HttpAuthSpec): HttpAuthDeclaration {
  return brand("httpAuth", spec, "httpAuth");
}

export function httpRoute(spec: HttpRouteSpec): HttpRouteDeclaration {
  return brand("httpRoute", spec, "httpRoute");
}

export function tcpDestination(spec: TcpDestinationSpec): TcpDestinationDeclaration {
  return brand("tcpDestination", spec, "tcpDestination");
}

export function global<const N extends string, const T extends ValueType>(spec: GlobalSpec<N, T>): GlobalDeclaration<N, T> {
  return brand("global", spec, "global");
}

export function secret<const N extends string, const T extends ValueType = "STRING">(spec: SecretSpec<N, T>): SecretDeclaration<N, T> {
  return brand("secret", spec, "secret");
}

export function jwtClaim<const N extends string, const T extends ValueType = "STRING">(
  spec: JwtClaimSpec<N, T>,
): JwtClaimDeclaration<N, T> {
  return brand("jwtClaim", spec, "jwtClaim");
}

export function errorDecl(spec: ErrorDeclSpec): ErrorDeclaration {
  return brand("errorDecl", spec, "errorDecl");
}

export function example(spec: ExampleSpec): ExampleDeclaration {
  return brand("example", spec, "example");
}

export function variable<const N extends string, const T extends ValueType>(spec: VariableSpec<N, T>): VariableDeclaration<N, T> {
  return brand("variable", spec, "variable");
}

export function idempotency(spec: IdempotencySpec): IdempotencyDeclaration {
  return brand("idempotency", spec, "idempotency");
}

export function reconcile(spec: ReconcileSpec): ReconcileDeclaration {
  return brand("reconcile", spec, "reconcile");
}

/** The function names the packager's analyser treats as declarations. */
export const DECLARATION_FUNCTIONS: readonly DeclarationKind[] = [
  "toolPackage",
  "serverTool",
  "clientTool",
  "destination",
  "httpAuth",
  "httpRoute",
  "tcpDestination",
  "global",
  "secret",
  "jwtClaim",
  "errorDecl",
  "example",
  "variable",
  "idempotency",
  "reconcile",
];
