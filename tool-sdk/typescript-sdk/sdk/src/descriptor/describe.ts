// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * Turns a project's exported declarations into generation-1 semantic descriptors (tool-sdk/spec/descriptor), a port of
 * the Java DescriptorProcessor with its checks, messages and their order. The packager runs it over the bundle at
 * build time and the runtime runs the same code at startup, so `toolgate dev` and the package carry the same digests.
 */

import type { ValueType } from "../context";
import {
  DECLARATION,
  declarationKind,
  type DeclarationKind,
  type DestinationDeclaration,
  type ErrorDeclaration,
  type ExampleDeclaration,
  type GlobalDeclaration,
  type HttpAuthDeclaration,
  type HttpRouteDeclaration,
  type JwtClaimDeclaration,
  type SecretDeclaration,
  type TcpDestinationDeclaration,
  type ToolDeclaration,
  type ToolPackageDeclaration,
  type VariableDeclaration,
} from "../declarations";
import { bytes, nfc, sha256, sorted } from "./jcs";
import { Problem, Problems } from "./problem";
import { constraints, isZodLike, isZodObject, specOf, specSchema, valueType, zodOutput, zodProperty, type Schema, type Spec } from "./schema";

export const GENERATION = 1;

export const PACKAGE_ID = /^[a-z][a-z0-9-]{1,30}(\.[a-z][a-z0-9-]{1,30}){1,3}$/;
export const LOCAL_NAME = /^[a-z][a-z0-9_-]{0,47}(\.[a-z][a-z0-9_-]{0,47}){0,2}$/;
export const TOOL_ID = /^[a-zA-Z0-9][a-zA-Z0-9._:/-]{0,127}$/;
export const MCP_NAME = /^[A-Za-z0-9_.-]{1,128}$/;
export const VARIABLE = /^[A-Za-z][A-Za-z0-9_]{0,63}$/;
export const DECLARATION_ID = /^[a-z][a-z0-9_.-]{0,63}$/;
export const ERROR_CODE = /^[A-Z][A-Z0-9_]{2,63}$/;
export const HEADER = /^[A-Za-z0-9-]{1,128}$/;
const RESERVED = ["toolgate.", "hotfolder.", "builtin."];
const FORBIDDEN_HEADERS = new Set(["authorization", "proxy-authorization", "cookie", "host"]);
const VALUE_TYPES = new Set<ValueType>([
  "STRING", "INT", "LONG", "NUMBER", "BOOLEAN", "DATE", "DATE_TIME", "DURATION", "ENUM", "URI", "OBJECT", "ARRAY", "BINARY",
]);

/** The modules of a project, by path relative to the project directory, as the bundle's entry imports them. */
export interface ProjectInput {
  /** The `toolgate.descriptorGeneration` of package.json, as given (undefined when missing). */
  readonly descriptorGeneration: unknown;
  readonly modules: Readonly<Record<string, Readonly<Record<string, unknown>>>>;
  /**
   * Output schemas the packager inferred from handler return types (TypeScript checker), by tool name; null for a
   * tool that returns ToolResult. A tool with an `output` schema ignores it.
   */
  readonly inferredOutputs?: Readonly<Record<string, Schema | null>>;
}

/** What the analyser checks one handler's code against. Value maps are name to value type. */
export interface Facts {
  readonly tool: string;
  readonly routes: readonly string[];
  readonly inputs: Readonly<Record<string, ValueType>>;
  readonly globals: Readonly<Record<string, ValueType>>;
  readonly secrets: Readonly<Record<string, ValueType>>;
  readonly claims: Readonly<Record<string, ValueType>>;
}

export interface ExampleEntry {
  readonly arguments: Record<string, unknown>;
  readonly expectedOutput?: unknown;
  readonly expectedError?: string;
  readonly exchanges?: string;
}

/** One described tool. */
export interface DescribedTool {
  readonly name: string;
  /** `<module>#<export>`. */
  readonly at: string;
  readonly descriptor: Record<string, unknown>;
  readonly bytes: Buffer;
  readonly digest: string;
  readonly declaration: ToolDeclaration;
  /** true: `run(args, ctx)`; false: `run(ctx)`. */
  readonly typed: boolean;
  readonly errorDataSchemas: Readonly<Record<string, Schema>>;
  readonly examples: readonly ExampleEntry[];
  readonly facts: Facts;
}

export interface DeclaredRoute {
  readonly id: string;
  readonly method: string;
  readonly destination: string;
  /** The path template without its patterns. */
  readonly template: string;
}

/** A described package: its descriptors, digests and the index the runtime and harness read. */
export interface DescribedPackage {
  readonly id: string;
  readonly name: string;
  readonly publisher: string;
  readonly description: string | undefined;
  readonly tools: readonly DescribedTool[];
  readonly routes: readonly DeclaredRoute[];
  readonly descriptorSetDigest: string;
}

interface Declared<T> {
  readonly value: T;
  readonly at: string;
}

/** Describes a project; throws Problems listing every declaration error. */
export function describeProject(input: ProjectInput): DescribedPackage {
  return new Build(input).run();
}

/** `[{toolDescriptorDigest, toolId}]` sorted by toolId, digested (descriptor §3). */
export function descriptorSetDigest(entries: ReadonlyArray<{ toolId: string; toolDescriptorDigest: string }>): string {
  const list = [...entries]
    .map((e) => ({ toolDescriptorDigest: e.toolDescriptorDigest, toolId: e.toolId }))
    .sort((a, b) => (nfc(a.toolId) < nfc(b.toolId) ? -1 : nfc(a.toolId) > nfc(b.toolId) ? 1 : 0));
  return sha256(bytes(list));
}

const PATTERN_VARIABLE = /\{([A-Za-z][A-Za-z0-9_]*)(:[^{}]*(\{[^{}]*\}[^{}]*)*)?\}/g;

/** A route template with its `{name:regex}` patterns removed. */
export function withoutPatterns(template: string): string {
  return template.replace(PATTERN_VARIABLE, (_m, name: string) => "{" + name + "}");
}

const MEMBERS: Record<string, readonly string[]> = {
  toolPackage: ["id", "name", "publisher", "description", "globals", "secrets", "jwt"],
  destination: ["id", "hostSetting", "defaultHost", "port", "spkiPins", "caBundle"],
  httpAuth: ["id", "type", "header", "secret", "connection", "scopes", "audience"],
  httpRoute: [
    "id", "destination", "method", "path", "query", "headers", "requestContentTypes", "maxRequestBytes",
    "maxResponseBytes", "streaming", "timeoutMs", "auth",
  ],
  tcpDestination: ["id", "hostSetting", "defaultHost", "port"],
  global: [
    "name", "type", "defaultValue", "required", "description", "min", "max", "minLength", "maxLength", "pattern",
    "enumValues", "format", "sensitive",
  ],
  secret: ["name", "type", "scope", "description"],
  jwtClaim: ["name", "claim", "type", "required"],
  errorDecl: ["code", "description", "retryable", "dataSchema"],
  example: ["arguments", "expectedOutput", "expectedError", "exchanges"],
  variable: [
    "name", "description", "type", "required", "defaultValue", "min", "max", "minLength", "maxLength", "pattern",
    "enumValues", "format", "sensitive",
  ],
  idempotency: ["header", "scope"],
  reconcile: ["tool"],
  serverTool: [
    "name", "title", "text", "description", "useWhen", "effect", "destructive", "openWorld", "async", "maxDurationMs",
    "stateless", "maxConcurrency", "routes", "tcp", "requiresBusinessKey", "businessKeyNamespace", "businessKeyArgument",
    "cancelIfAbandoned", "errors", "toolsets", "examples", "idempotency", "reconcile", "globals", "secrets", "jwt",
    "input", "variables", "output", "run", "tiers",
  ],
};
MEMBERS["clientTool"] = MEMBERS["serverTool"]!;

const ENUMS: Record<string, Record<string, readonly string[]>> = {
  serverTool: {
    effect: ["READ_ONLY", "IDEMPOTENT", "NON_IDEMPOTENT", "UNKNOWN"],
    businessKeyNamespace: ["AGENT", "DELEGATED_USER", "TENANT"],
  },
  httpAuth: { type: ["API_KEY", "BASIC", "OAUTH2_CLIENT_CREDENTIALS", "TOKEN_EXCHANGE", "CONNECTION", "CUSTOM"] },
  httpRoute: { method: ["GET", "HEAD", "POST", "PUT", "PATCH", "DELETE"], streaming: ["NONE", "RESPONSE", "BIDIRECTIONAL"] },
  secret: { scope: ["GROUP", "USER"] },
  idempotency: { scope: ["BUSINESS_KEY", "PER_INVOCATION"] },
};
ENUMS["clientTool"] = ENUMS["serverTool"]!;

class Build {
  private readonly problems: Problem[] = [];
  private readonly destinations = new Map<string, Declared<DestinationDeclaration>>();
  private readonly auths = new Map<string, Declared<HttpAuthDeclaration>>();
  private readonly routes = new Map<string, Declared<HttpRouteDeclaration>>();
  private readonly tcps = new Map<string, Declared<TcpDestinationDeclaration>>();
  private readonly tools = new Map<string, Declared<ToolDeclaration>>();
  private readonly reconcilers = new Map<string, string>();
  private pkg!: Declared<ToolPackageDeclaration>;

  constructor(private readonly input: ProjectInput) {}

  run(): DescribedPackage {
    const anchor = this.firstDeclaration();
    const generation = this.input.descriptorGeneration;
    if (generation === undefined || generation === null) {
      throw new Problems([
        new Problem(anchor, 'the build must set descriptorGeneration (the "toolgate" key in package.json); it has no default'),
      ]);
    }
    if (String(generation).trim() !== String(GENERATION)) {
      throw new Problems([
        new Problem(anchor, "descriptorGeneration=" + String(generation) + " is not supported; this SDK emits generation " + GENERATION),
      ]);
    }
    const collected = this.collect();
    this.fail();
    this.packageDeclaration(collected.packages);
    this.network(collected);
    for (const t of collected.tools) this.guard(() => this.register(t));
    for (const t of collected.tools) this.guard(() => this.reconciler(t));
    this.fail();
    const described: DescribedTool[] = [];
    for (const name of [...this.tools.keys()].sort()) {
      this.guard(() => described.push(this.descriptor(name, this.tools.get(name)!)));
    }
    this.fail();
    const routes: DeclaredRoute[] = [...this.routes.values()].map((r) => ({
      id: r.value.id,
      method: r.value.method,
      destination: r.value.destination,
      template: withoutPatterns(r.value.path),
    }));
    return {
      id: this.pkg.value.id,
      name: this.pkg.value.name,
      publisher: this.pkg.value.publisher,
      description: this.pkg.value.description || undefined,
      tools: described,
      routes,
      descriptorSetDigest: descriptorSetDigest(
        described.map((t) => ({ toolId: t.descriptor["toolId"] as string, toolDescriptorDigest: t.digest })),
      ),
    };
  }

  private firstDeclaration(): string | undefined {
    for (const path of Object.keys(this.input.modules).sort()) {
      for (const name of Object.keys(this.input.modules[path]!).sort()) {
        if (declarationKind(this.input.modules[path]![name]) !== undefined) return path + "#" + name;
      }
    }
    return undefined;
  }

  private fail(): void {
    if (this.problems.length) throw new Problems(this.problems);
  }

  private guard(step: () => void): void {
    try {
      step();
    } catch (e) {
      if (e instanceof Problem) this.problems.push(e);
      else throw e;
    }
  }

  /** Exported declarations, each object once (a re-export is the same declaration), in module and export order. */
  private collect(): {
    packages: Declared<ToolPackageDeclaration>[];
    tools: Declared<ToolDeclaration>[];
    destinations: Declared<DestinationDeclaration>[];
    auths: Declared<HttpAuthDeclaration>[];
    routes: Declared<HttpRouteDeclaration>[];
    tcps: Declared<TcpDestinationDeclaration>[];
  } {
    const seen = new Set<unknown>();
    const out = {
      packages: [] as Declared<ToolPackageDeclaration>[],
      tools: [] as Declared<ToolDeclaration>[],
      destinations: [] as Declared<DestinationDeclaration>[],
      auths: [] as Declared<HttpAuthDeclaration>[],
      routes: [] as Declared<HttpRouteDeclaration>[],
      tcps: [] as Declared<TcpDestinationDeclaration>[],
    };
    for (const path of Object.keys(this.input.modules).sort()) {
      const mod = this.input.modules[path]!;
      for (const name of Object.keys(mod).sort((a, b) => (a === "default" ? -1 : b === "default" ? 1 : a < b ? -1 : a > b ? 1 : 0))) {
        const value = mod[name];
        const kind = declarationKind(value);
        if (kind === undefined || seen.has(value)) continue;
        seen.add(value);
        const at = path + "#" + name;
        this.guard(() => this.shape(kind, value as Record<string, unknown>, at, kind));
        switch (kind) {
          case "toolPackage":
            out.packages.push({ value: value as ToolPackageDeclaration, at });
            break;
          case "serverTool":
          case "clientTool":
            out.tools.push({ value: value as ToolDeclaration, at });
            break;
          case "destination":
            out.destinations.push({ value: value as DestinationDeclaration, at });
            break;
          case "httpAuth":
            out.auths.push({ value: value as HttpAuthDeclaration, at });
            break;
          case "httpRoute":
            out.routes.push({ value: value as HttpRouteDeclaration, at });
            break;
          case "tcpDestination":
            out.tcps.push({ value: value as TcpDestinationDeclaration, at });
            break;
          default:
            break;
        }
      }
    }
    return out;
  }

  /** Member names, enum values and member types, recursively; TypeScript checks these too, JavaScript does not. */
  private shape(kind: DeclarationKind, d: Record<string, unknown>, at: string, what: string): void {
    const allowed = MEMBERS[kind]!;
    for (const key of Object.keys(d)) {
      if (!allowed.includes(key)) throw new Problem(at, what + " has no member " + key);
    }
    for (const [key, values] of Object.entries(ENUMS[kind] ?? {})) {
      const v = d[key];
      if (v !== undefined && !values.includes(v as string)) {
        throw new Problem(at, what + " " + key + " " + String(v) + " is not one of " + values.join(", "));
      }
    }
    if ((kind === "global" || kind === "variable" || kind === "secret" || kind === "jwtClaim") && d["type"] !== undefined) {
      if (!VALUE_TYPES.has(d["type"] as ValueType)) throw new Problem(at, what + " type " + String(d["type"]) + " is not a value type");
    }
    const nested: Record<string, DeclarationKind> = {
      globals: "global",
      secrets: "secret",
      jwt: "jwtClaim",
      errors: "errorDecl",
      examples: "example",
    };
    for (const [key, k] of Object.entries(nested)) {
      const list = d[key];
      if (list === undefined) continue;
      if (!Array.isArray(list)) throw new Problem(at, what + " " + key + " must be a list");
      for (const item of list) {
        if (declarationKind(item) !== k) throw new Problem(at, what + " " + key + " must hold " + k + "(...) declarations");
        this.shape(k, item as Record<string, unknown>, at, k + " " + String((item as Record<string, unknown>)["name"] ?? (item as Record<string, unknown>)["code"] ?? ""));
      }
    }
    for (const [key, k] of [["idempotency", "idempotency"], ["reconcile", "reconcile"]] as const) {
      if (d[key] === undefined) continue;
      if (declarationKind(d[key]) !== k) throw new Problem(at, what + " " + key + " must be a " + k + "(...) declaration");
      this.shape(k, d[key] as Record<string, unknown>, at, k);
    }
    if (d["variables"] !== undefined) {
      const v = d["variables"] as Record<string, unknown>;
      if (v === null || typeof v !== "object") throw new Problem(at, what + " variables must be an object");
      const groups: Record<string, DeclarationKind> = { inputs: "variable", globals: "global", secrets: "secret", jwt: "jwtClaim" };
      for (const key of Object.keys(v)) {
        if (!(key in groups)) throw new Problem(at, what + " variables has no member " + key);
        const list = v[key];
        if (!Array.isArray(list)) throw new Problem(at, what + " variables." + key + " must be a list");
        for (const item of list) {
          if (declarationKind(item) !== groups[key]) {
            throw new Problem(at, what + " variables." + key + " must hold " + groups[key] + "(...) declarations");
          }
          this.shape(groups[key]!, item as Record<string, unknown>, at, groups[key] + " " + String((item as Record<string, unknown>)["name"]));
        }
      }
    }
  }

  private packageDeclaration(declared: Declared<ToolPackageDeclaration>[]): void {
    if (declared.length !== 1) {
      throw new Problems([
        new Problem(declared[0]?.at ?? this.firstDeclaration(), "exactly one toolPackage is required per package; found " + declared.length),
      ]);
    }
    this.pkg = declared[0]!;
    const p = this.pkg.value;
    const at = this.pkg.at;
    const fail = (m: string): never => {
      throw new Problems([new Problem(at, m)]);
    };
    if (typeof p.id !== "string" || !PACKAGE_ID.test(p.id)) fail("package id " + String(p.id) + " does not match " + PACKAGE_ID.source);
    try {
      reserved(p.id, "package id", at);
    } catch (e) {
      fail((e as Problem).message);
    }
    if (typeof p.name !== "string" || p.name.trim() === "") fail("package name is required");
    if (typeof p.publisher !== "string" || p.publisher.trim() === "") fail("package publisher is required");
  }

  private network(c: ReturnType<Build["collect"]>): void {
    for (const d of c.destinations) this.put(this.destinations, d.value.id, d, "destination");
    for (const a of c.auths) this.put(this.auths, a.value.id, a, "auth");
    for (const r of c.routes) this.put(this.routes, r.value.id, r, "route");
    for (const t of c.tcps) this.put(this.tcps, t.value.id, t, "TCP destination");
    for (const id of [...this.routes.keys()].sort()) this.guard(() => this.route(this.routes.get(id)!));
    for (const id of [...this.destinations.keys()].sort()) {
      const d = this.destinations.get(id)!;
      this.guard(() => {
        variable(d.value.hostSetting, "hostSetting of destination " + d.value.id, d.at);
        port(d.value.port ?? 443, d.at);
        strings(d.value.spkiPins, "spkiPins of destination " + d.value.id, d.at);
      });
    }
    for (const id of [...this.tcps.keys()].sort()) {
      const d = this.tcps.get(id)!;
      this.guard(() => {
        variable(d.value.hostSetting, "hostSetting of TCP destination " + d.value.id, d.at);
        port(d.value.port, d.at);
      });
    }
    for (const id of [...this.auths.keys()].sort()) {
      const a = this.auths.get(id)!;
      this.guard(() => {
        if (a.value.header && !HEADER.test(a.value.header)) throw new Problem(a.at, "auth " + a.value.id + " header is not a header name");
        if (a.value.secret) variable(a.value.secret, "secret of auth " + a.value.id, a.at);
        if (a.value.connection) declarationId(a.value.connection, "connection", a.at);
        strings(a.value.scopes, "scopes of auth " + a.value.id, a.at);
      });
    }
  }

  private put<T extends { id: string }>(map: Map<string, Declared<T>>, id: string, d: Declared<T>, kind: string): void {
    this.guard(() => {
      declarationId(id, kind + " id", d.at);
      if (map.has(id)) throw new Problem(d.at, kind + " id " + id + " is declared twice in the package");
      map.set(id, d);
    });
  }

  private route(d: Declared<HttpRouteDeclaration>): void {
    const r = d.value;
    if (!this.destinations.has(r.destination)) {
      throw new Problem(d.at, "route " + r.id + " names undeclared destination " + r.destination);
    }
    if (r.auth && !this.auths.has(r.auth)) throw new Problem(d.at, "route " + r.id + " names undeclared auth " + r.auth);
    pathTemplate(r, d.at);
    const authHeader = r.auth ? (this.auths.get(r.auth)!.value.header ?? "").toLowerCase() : "";
    for (const h of r.headers ?? []) {
      const lower = h.toLowerCase();
      if (!HEADER.test(h)) throw new Problem(d.at, "route " + r.id + " header " + h + " is not a header name");
      if (FORBIDDEN_HEADERS.has(lower) || lower.startsWith("x-forwarded-") || lower === authHeader) {
        throw new Problem(d.at, "route " + r.id + " may not allow header " + h);
      }
    }
    unique(strings(r.query, "query of route " + r.id, d.at), "query name", d.at);
    unique(strings(r.headers, "headers of route " + r.id, d.at), "header", d.at);
    unique(strings(r.requestContentTypes ?? ["application/json"], "requestContentTypes of route " + r.id, d.at), "content type", d.at);
    range(r.maxRequestBytes ?? 1_048_576, 0, 2 ** 30, "maxRequestBytes", d.at);
    range(r.maxResponseBytes ?? 4_194_304, 0, 2 ** 30, "maxResponseBytes", d.at);
    range(r.timeoutMs ?? 10_000, 1, 900_000, "timeoutMs", d.at);
  }

  private register(t: Declared<ToolDeclaration>): void {
    const name = t.value.name;
    if (typeof name !== "string" || !LOCAL_NAME.test(name)) {
      throw new Problem(t.at, "tool name " + String(name) + " does not match " + LOCAL_NAME.source);
    }
    reserved(name, "tool name", t.at);
    if (this.tools.has(name)) throw new Problem(t.at, "tool " + name + " is declared twice");
    if (typeof t.value.run !== "function") throw new Problem(t.at, "tool " + name + " needs a run function");
    this.tools.set(name, t);
  }

  private reconciler(t: Declared<ToolDeclaration>): void {
    const r = t.value.reconcile;
    if (r === undefined) return;
    const self = t.value.name;
    if (!this.tools.has(r.tool)) throw new Problem(t.at, "reconcile names unknown tool " + r.tool);
    if (r.tool === self) throw new Problem(t.at, "a tool cannot reconcile itself");
    if (this.reconcilers.has(r.tool)) throw new Problem(t.at, "tool " + r.tool + " has two reconcile handlers");
    this.reconcilers.set(r.tool, self);
  }

  private descriptor(name: string, declared: Declared<ToolDeclaration>): DescribedTool {
    const t = declared.value;
    const at = declared.at;
    const isClient = t[DECLARATION] === "clientTool";
    const pkg = this.pkg.value;

    const toolId = pkg.id + "/" + name;
    if (toolId.length > 128 || !TOOL_ID.test(toolId)) {
      throw new Problem(at, "toolId " + toolId + " is longer than 128 characters or not a valid id");
    }
    const alias = pkg.id.substring(pkg.id.lastIndexOf(".") + 1);
    const mcpName = alias + "." + name;
    if (!MCP_NAME.test(mcpName)) throw new Problem(at, "mcpName " + mcpName + " is longer than 128 characters");

    const titleSet = t.title !== undefined && t.title !== "";
    const textSet = t.text !== undefined && t.text !== "";
    if (titleSet === textSet) throw new Problem(at, "tool " + name + " must set exactly one of title and text");
    const title = (titleSet ? t.title : t.text)!;
    length(title, 1, 100, "title", at);
    length(t.description ?? "", 1, 2048, "description", at);
    if ((t.useWhen ?? "").length > 512) throw new Problem(at, "useWhen is longer than 512 characters");

    if (isClient) {
      for (const k of ["routes", "tcp"] as const) {
        if (t[k] !== undefined && t[k]!.length > 0) throw new Problem(at, "client tool " + name + " cannot declare " + k);
      }
    } else if (t.tiers !== undefined) {
      throw new Problem(at, "tiers apply to client tools only");
    }

    const properties: Schema = {};
    const required: string[] = [];
    const typed = this.inputs(name, t, at, properties, required);

    const d: Record<string, unknown> = {};
    d["descriptorGeneration"] = GENERATION;
    d["toolId"] = toolId;
    d["mcpName"] = mcpName;
    d["kind"] = isClient ? "CLIENT" : "SERVER";
    d["title"] = title;
    d["description"] = t.description;
    if (t.useWhen) d["useWhen"] = t.useWhen;
    d["inputSchema"] = { type: "object", properties, required: sorted(required), additionalProperties: false };
    const output = this.output(name, t, at);
    if (output !== undefined) d["outputSchema"] = output;
    const errorDataSchemas: Record<string, Schema> = {};
    d["errors"] = errors(t.errors ?? [], at, errorDataSchemas);

    const routes = strings(t.routes, "routes", at);
    const tcp = strings(t.tcp, "tcp", at);
    const effect = t.effect ?? "UNKNOWN";
    const destructive = bool(t.destructive, "destructive", at);
    d["effect"] = {
      safety: effect,
      destructive,
      openWorld: t.openWorld === undefined ? !isClient && routes.length > 0 : bool(t.openWorld, "openWorld", at),
    };
    d["businessKey"] = this.businessKey(t, effect, destructive, at, Object.keys(properties));
    d["execution"] = execution(t, at);
    if (this.reconcilers.has(name)) d["reconcileTool"] = this.reconcilers.get(name);
    if (isClient) d["tiers"] = tiers(t.tiers ?? ["OCI_SANDBOX"], at);

    const routeIds = [...new Set(unique(routes, "route", at))].sort();
    const tcpIds = [...new Set(unique(tcp, "TCP destination", at))].sort();
    const routeList: unknown[] = [];
    const destinationIds = new Set<string>();
    const authIds = new Set<string>();
    for (const id of routeIds) {
      const r = this.routes.get(id);
      if (r === undefined) throw new Problem(at, "tool " + name + " lists undeclared route " + id);
      routeList.push(routeJson(r.value));
      destinationIds.add(r.value.destination);
      if (r.value.auth) authIds.add(r.value.auth);
    }
    d["routes"] = routeList;
    d["destinations"] = [...destinationIds].sort().map((id) => destinationJson(this.destinations.get(id)!.value));
    d["httpAuth"] = [...authIds].sort().map((id) => authJson(this.auths.get(id)!.value));
    const tcpList: unknown[] = [];
    for (const id of tcpIds) {
      const x = this.tcps.get(id);
      if (x === undefined) throw new Problem(at, "tool " + name + " lists undeclared TCP destination " + id);
      tcpList.push({ id, hostSetting: x.value.hostSetting, port: x.value.port });
    }
    d["tcp"] = tcpList;

    const vars = this.variables(t, at, Object.keys(properties), isClient);
    for (const authId of [...authIds].sort()) {
      const a = this.auths.get(authId)!.value;
      if (a.secret && !vars.secrets.some((s) => s.name === a.secret)) {
        throw new Problem(at, "auth " + authId + " uses secret " + a.secret + ", which tool " + name + " does not declare");
      }
    }
    d["variables"] = vars.toJson(at);

    const permissions = new Set<string>();
    if (routeIds.length) permissions.add("HTTP_ROUTE");
    if (tcpIds.length) {
      permissions.add("TCP");
      permissions.add("CREDENTIALS_IN_SANDBOX");
    }
    for (const authId of authIds) {
      const type = this.auths.get(authId)!.value.type;
      if (type === "CUSTOM") permissions.add("CREDENTIALS_IN_SANDBOX");
      if (type === "TOKEN_EXCHANGE") permissions.add("TOKEN_EXCHANGE");
      if (type === "CONNECTION") permissions.add("CONNECTION");
    }
    if (vars.secrets.some((s) => s.scope === "USER")) permissions.add("USER_SECRET");
    d["permissions"] = sorted(permissions);

    const toolsets = strings(t.toolsets, "toolsets", at);
    for (const set of toolsets) length(set, 1, 64, "toolset", at);
    d["toolsets"] = sorted(unique(toolsets, "toolset", at));

    const examples = t.examples ?? [];
    if (examples.length > 8) throw new Problem(at, "tool " + name + " declares " + examples.length + " examples; at most 8 are allowed");
    const exampleEntries = examples.map((e) => exampleEntry(e, at));

    const inputTypes: Record<string, ValueType> = {};
    for (const [k, v] of Object.entries(properties)) inputTypes[k] = valueType(v as Schema);
    const facts: Facts = {
      tool: name,
      routes: routeIds,
      inputs: inputTypes,
      globals: Object.fromEntries(vars.globals.map((g) => [g.name, g.type])),
      secrets: Object.fromEntries(vars.secrets.map((s) => [s.name, s.type ?? "STRING"])),
      claims: Object.fromEntries(vars.claims.map((c) => [c.name, c.type ?? "STRING"])),
    };
    let canonical: Buffer;
    try {
      canonical = bytes(d);
    } catch (e) {
      throw new Problem(at, "cannot write the descriptor: " + (e as Error).message);
    }
    return {
      name,
      at,
      descriptor: JSON.parse(canonical.toString("utf8")) as Record<string, unknown>,
      bytes: canonical,
      digest: sha256(canonical),
      declaration: t,
      typed,
      errorDataSchemas,
      examples: exampleEntries,
      facts,
    };
  }

  /** Fills the input properties; returns whether the handler takes typed arguments. */
  private inputs(name: string, t: ToolDeclaration, at: string, properties: Schema, required: string[]): boolean {
    const groupedInputs = t.variables?.inputs ?? [];
    if (t.input !== undefined) {
      if (groupedInputs.length > 0) throw new Problem(at, "with variables.inputs the handler takes only ToolContext; drop input");
      if (!isZodObject(t.input)) {
        throw new Problem(
          at,
          isZodLike(t.input) ? "input must be a Zod 3 object schema, z.object({...})" : "input must be a Zod object schema, z.object({...})",
        );
      }
      const def = (t.input as unknown as { _def: Record<string, unknown> })._def;
      if (def["unknownKeys"] === "passthrough") throw new Problem(at, "input: .passthrough() is not allowed in generation 1 (descriptor §5)");
      const shape = (def["shape"] as () => Record<string, unknown>)();
      for (const [input, zod] of Object.entries(shape)) {
        variable(input, "input name", at);
        const p = zodProperty(zod, "input " + input, at);
        if (p.description === "") throw new Problem(at, "input " + input + " needs a description");
        if (p.description.length > 1024) throw new Problem(at, "input " + input + " description is longer than 1024 characters");
        properties[input] = p.schema;
        if (!p.optional) required.push(input);
      }
      return true;
    }
    for (const v of groupedInputs as readonly VariableDeclaration[]) {
      if (v.type === "OBJECT" || v.type === "ARRAY") {
        throw new Problem(at, "grouped input " + v.name + " needs a scalar type; use the typed form for objects and arrays");
      }
      const spec = specOf(v as unknown as Record<string, unknown>, at, "variable " + v.name);
      variable(spec.name, "input name", at);
      if (spec.description === "") throw new Problem(at, "input " + spec.name + " needs a description");
      if (spec.description.length > 1024) throw new Problem(at, "input " + spec.name + " description is longer than 1024 characters");
      if (spec.name in properties) throw new Problem(at, "input " + spec.name + " is declared twice");
      properties[spec.name] = specSchema(v.type === "ENUM" ? "ENUM" : v.type, spec, at, true);
      if (spec.required && spec.defaultValue === "") required.push(spec.name);
    }
    void name;
    return false;
  }

  private output(name: string, t: ToolDeclaration, at: string): Schema | undefined {
    if (t.output !== undefined) return zodOutput(t.output, at);
    const inferred = this.input.inferredOutputs?.[name];
    return inferred === null || inferred === undefined ? undefined : inferred;
  }

  private businessKey(t: ToolDeclaration, effect: string, destructive: boolean, at: string, inputs: string[]): Record<string, unknown> {
    const m: Record<string, unknown> = {};
    m["required"] = t.requiresBusinessKey === undefined ? effect === "NON_IDEMPOTENT" && destructive : bool(t.requiresBusinessKey, "requiresBusinessKey", at);
    m["namespace"] = t.businessKeyNamespace ?? "AGENT";
    if (t.businessKeyArgument) {
      if (!inputs.includes(t.businessKeyArgument)) {
        throw new Problem(at, "businessKeyArgument " + t.businessKeyArgument + " is not an input of the tool");
      }
      m["argument"] = t.businessKeyArgument;
    }
    if (t.idempotency !== undefined) {
      if (typeof t.idempotency.header !== "string" || !HEADER.test(t.idempotency.header)) {
        throw new Problem(at, "idempotency header is not a header name");
      }
      m["idempotencyHeader"] = t.idempotency.header;
      m["idempotencyScope"] = t.idempotency.scope ?? "BUSINESS_KEY";
    }
    return m;
  }

  /** Globals, secrets and claims in scope: the package, then the tool (its own lists and the grouped form). */
  private variables(t: ToolDeclaration, at: string, inputs: string[], isClient: boolean): Variables {
    const pkg = this.pkg.value;
    const scope = new Variables();
    scope.add(this.pkg.at, pkg.globals ?? [], pkg.secrets ?? [], pkg.jwt ?? []);
    const tool = new Variables();
    tool.add(at, t.globals ?? [], t.secrets ?? [], t.jwt ?? []);
    tool.add(at, t.variables?.globals ?? [], t.variables?.secrets ?? [], t.variables?.jwt ?? []);
    for (const [n, what] of tool.names) {
      if (scope.names.has(n)) {
        throw new Problem(at, "tool-level " + what + " cannot override the declaration " + scope.names.get(n) + " of its package");
      }
    }
    const all = new Variables();
    all.merge(scope);
    all.merge(tool);
    const seen = new Map(all.names);
    for (const input of inputs) {
      const key = input.toLowerCase();
      const prior = seen.get(key);
      seen.set(key, "input " + input);
      if (prior !== undefined) throw new Problem(at, "input " + input + " and " + prior + " have the same name, ignoring case");
    }
    if (isClient && all.secrets.length > 0) throw new Problem(at, "client tool cannot use secrets; found " + all.secrets[0]!.name);
    for (const g of all.globals) globalConstraints(g, at);
    return all;
  }
}

/** Scope-level or tool-level globals, secrets and claims, with case-insensitive name tracking. */
class Variables {
  readonly globals: GlobalDeclaration[] = [];
  readonly secrets: SecretDeclaration[] = [];
  readonly claims: JwtClaimDeclaration[] = [];
  readonly names = new Map<string, string>();

  add(at: string, gs: readonly GlobalDeclaration[], ss: readonly SecretDeclaration[], cs: readonly JwtClaimDeclaration[]): void {
    for (const g of gs) {
      this.name(g.name, "global " + g.name, at);
      this.globals.push(g);
    }
    for (const s of ss) {
      this.name(s.name, "secret " + s.name, at);
      this.secrets.push(s);
    }
    for (const c of cs) {
      this.name(c.name, "claim " + c.name, at);
      if (typeof c.claim !== "string" || c.claim.length === 0 || c.claim.length > 256) {
        throw new Problem(at, "claim " + c.name + " needs a claim of 1-256 characters");
      }
      this.claims.push(c);
    }
  }

  merge(other: Variables): void {
    for (const [k, v] of other.names) if (!this.names.has(k)) this.names.set(k, v);
    this.globals.push(...other.globals);
    this.secrets.push(...other.secrets);
    this.claims.push(...other.claims);
  }

  private name(name: string, what: string, at: string): void {
    variable(name, what, at);
    const key = name.toLowerCase();
    const prior = this.names.get(key);
    this.names.set(key, what);
    if (prior !== undefined) throw new Problem(at, what + " and " + prior + " have the same name, ignoring case");
  }

  toJson(at: string): Record<string, unknown> {
    const byName = <T>(list: T[], key: (t: T) => string): T[] =>
      [...new Map(list.map((x) => [nfc(key(x)), x])).entries()].sort((a, b) => (a[0] < b[0] ? -1 : a[0] > b[0] ? 1 : 0)).map((e) => e[1]);
    const globals = byName(this.globals, (g) => g.name).map((x) => {
      const v: Record<string, unknown> = {
        name: x.name,
        type: x.type,
        required: x.required === undefined ? true : x.required === true,
        sensitive: x.sensitive === true,
      };
      if (x.description) v["description"] = x.description;
      Object.assign(v, globalConstraints(x, at));
      return v;
    });
    const secrets = byName(this.secrets, (s) => s.name).map((x) => ({ name: x.name, type: x.type ?? "STRING", scope: x.scope ?? "GROUP" }));
    const claims = byName(this.claims, (c) => c.name).map((x) => ({
      name: x.name,
      claim: x.claim,
      type: x.type ?? "STRING",
      required: x.required === undefined ? true : x.required === true,
    }));
    return { globals, secrets, claims };
  }
}

/** The constraint members of a global; INT and LONG bounds are not materialised. */
export function globalConstraints(g: GlobalDeclaration, at: string | undefined): Schema {
  if (g.type === undefined) throw new Problem(at, "global " + g.name + " needs a type");
  if (g.type === "ENUM" && (g.enumValues ?? []).length === 0) throw new Problem(at, "global " + g.name + " of type ENUM needs enumValues");
  if ((g.description ?? "").length > 1024) throw new Problem(at, "global " + g.name + " description is longer than 1024 characters");
  const spec: Spec = specOf(g as unknown as Record<string, unknown>, at, "global " + g.name);
  const out: Schema = {};
  if (g.type === "ENUM") out["enum"] = [...(g.enumValues ?? [])];
  constraints(out, g.type, spec, at, false);
  delete out["x-toolgate-sensitive"];
  return out;
}

function errors(list: readonly ErrorDeclaration[], at: string, dataSchemas: Record<string, Schema>): unknown[] {
  const byCode = new Map<string, unknown>();
  for (const e of list) {
    if (typeof e.code !== "string" || !ERROR_CODE.test(e.code)) {
      throw new Problem(at, "error code " + String(e.code) + " does not match " + ERROR_CODE.source);
    }
    length(e.description ?? "", 1, 1024, "error description", at);
    if (byCode.has(e.code)) throw new Problem(at, "error code " + e.code + " is declared twice");
    byCode.set(e.code, { code: e.code, description: e.description, retryable: e.retryable === true });
    if (e.dataSchema !== undefined && e.dataSchema !== "") {
      let schema: unknown = e.dataSchema;
      if (typeof schema === "string") {
        try {
          schema = JSON.parse(schema);
        } catch {
          throw new Problem(at, "error " + e.code + " dataSchema is not JSON");
        }
      }
      if (schema === null || typeof schema !== "object" || Array.isArray(schema)) {
        throw new Problem(at, "error " + e.code + " dataSchema must be a JSON object");
      }
      dataSchemas[e.code] = schema as Schema;
    }
  }
  return [...byCode.keys()].sort().map((k) => byCode.get(k));
}

function execution(t: ToolDeclaration, at: string): Record<string, unknown> {
  const isAsync = bool(t.async, "async", at);
  const ceiling = isAsync ? 900_000 : 60_000;
  const duration = t.maxDurationMs === undefined ? (isAsync ? 900_000 : 30_000) : t.maxDurationMs;
  if (!Number.isInteger(duration) || duration < 1 || duration > ceiling) {
    throw new Problem(at, "maxDurationMs must be 1.." + ceiling + (isAsync ? " for async tools" : " for sync tools; make the tool async for longer work"));
  }
  const cancelIfAbandoned = bool(t.cancelIfAbandoned, "cancelIfAbandoned", at);
  if (cancelIfAbandoned && !isAsync) throw new Problem(at, "cancelIfAbandoned applies to async tools only");
  const m: Record<string, unknown> = { async: isAsync, maxDurationMs: duration, stateless: bool(t.stateless, "stateless", at) };
  if (t.maxConcurrency !== undefined) {
    range(t.maxConcurrency, 1, 1_048_576, "maxConcurrency", at);
    m["maxConcurrency"] = t.maxConcurrency;
  }
  m["cancelIfAbandoned"] = cancelIfAbandoned;
  return m;
}

function tiers(list: readonly string[], at: string): string[] {
  if (list.length === 0) throw new Problem(at, "a client tool needs at least one tier");
  for (const tier of list) {
    if (!["OCI_SANDBOX", "WASI", "HOST_NATIVE"].includes(tier)) throw new Problem(at, "tier " + tier + " is not one of OCI_SANDBOX, WASI, HOST_NATIVE");
  }
  return sorted(unique([...list], "tier", at));
}

function routeJson(r: HttpRouteDeclaration): Record<string, unknown> {
  const m: Record<string, unknown> = {
    id: r.id,
    destination: r.destination,
    method: r.method,
    path: r.path,
    query: sorted(r.query ?? []),
    headers: sorted(r.headers ?? []),
    requestContentTypes: sorted(r.requestContentTypes ?? ["application/json"]),
    maxRequestBytes: r.maxRequestBytes ?? 1_048_576,
    maxResponseBytes: r.maxResponseBytes ?? 4_194_304,
    streaming: r.streaming ?? "NONE",
    timeoutMs: r.timeoutMs ?? 10_000,
  };
  if (r.auth) m["auth"] = r.auth;
  return m;
}

function destinationJson(d: DestinationDeclaration): Record<string, unknown> {
  const m: Record<string, unknown> = { id: d.id, hostSetting: d.hostSetting, port: d.port ?? 443, spkiPins: [...(d.spkiPins ?? [])] };
  if (d.caBundle) m["caBundle"] = d.caBundle;
  return m;
}

function authJson(a: HttpAuthDeclaration): Record<string, unknown> {
  const m: Record<string, unknown> = { id: a.id, type: a.type };
  if (a.header) m["header"] = a.header;
  if (a.secret) m["secret"] = a.secret;
  if (a.connection) m["connection"] = a.connection;
  m["scopes"] = [...(a.scopes ?? [])];
  if (a.audience) m["audience"] = a.audience;
  return m;
}

function exampleEntry(e: ExampleDeclaration, at: string): ExampleEntry {
  let args: unknown = e.arguments;
  if (typeof args === "string") {
    if (args.trim() === "") throw new Problem(at, "an example needs arguments");
    try {
      args = JSON.parse(args);
    } catch {
      throw new Problem(at, "example arguments are not JSON");
    }
  }
  if (args === undefined || args === null || typeof args !== "object" || Array.isArray(args)) {
    throw new Problem(at, "an example needs arguments, a JSON object");
  }
  const hasOutput = e.expectedOutput !== undefined && e.expectedOutput !== "";
  const hasError = e.expectedError !== undefined && e.expectedError !== "";
  if (hasOutput && hasError) throw new Problem(at, "an example expects either an output or an error");
  const entry: { -readonly [K in keyof ExampleEntry]: ExampleEntry[K] } = { arguments: args as Record<string, unknown> };
  if (hasOutput) {
    let out: unknown = e.expectedOutput;
    if (typeof out === "string") {
      try {
        out = JSON.parse(out);
      } catch {
        throw new Problem(at, "example expectedOutput is not JSON");
      }
    }
    entry.expectedOutput = out;
  }
  if (hasError) entry.expectedError = e.expectedError;
  if (e.exchanges) entry.exchanges = e.exchanges;
  return entry;
}

/** RFC 6570 level 1 with {name:regex}: every variable needs a pattern, and the patterns .* and .+ are refused. */
function pathTemplate(r: HttpRouteDeclaration, at: string): void {
  const path = r.path;
  if (typeof path !== "string" || !path.startsWith("/")) throw new Problem(at, "route " + r.id + " path must start with /");
  const names = new Set<string>();
  let i = 0;
  while (i < path.length) {
    const c = path[i];
    if (c === "}") throw new Problem(at, "route " + r.id + " path has an unmatched }");
    if (c !== "{") {
      i++;
      continue;
    }
    let depth = 1;
    let j = i + 1;
    while (j < path.length && depth > 0) {
      if (path[j] === "{") depth++;
      if (path[j] === "}") depth--;
      j++;
    }
    if (depth !== 0) throw new Problem(at, "route " + r.id + " path has an unmatched {");
    const body = path.substring(i + 1, j - 1);
    const colon = body.indexOf(":");
    const name = colon < 0 ? body : body.substring(0, colon);
    variable(name, "path variable of route " + r.id, at);
    if (names.has(name)) throw new Problem(at, "route " + r.id + " repeats path variable " + name);
    names.add(name);
    if (colon < 0) throw new Problem(at, "route " + r.id + " path variable " + name + " needs a pattern, {" + name + ":regex}");
    const regex = body.substring(colon + 1);
    if (regex === ".*" || regex === ".+") throw new Problem(at, "route " + r.id + " path variable " + name + " may not use " + regex);
    try {
      new RegExp(regex);
    } catch {
      throw new Problem(at, "route " + r.id + " path variable " + name + " pattern does not compile");
    }
    i = j;
  }
}

function reserved(value: string, what: string, at: string): void {
  for (const prefix of RESERVED) {
    if (value.startsWith(prefix) || value === prefix.substring(0, prefix.length - 1)) {
      throw new Problem(at, what + " " + value + " uses the reserved prefix " + prefix);
    }
  }
}

export function variable(name: unknown, what: string, at: string | undefined): void {
  if (typeof name !== "string" || !VARIABLE.test(name)) {
    throw new Problem(at, what + " " + String(name) + " does not match " + VARIABLE.source);
  }
}

function declarationId(id: unknown, what: string, at: string): void {
  if (typeof id !== "string" || !DECLARATION_ID.test(id)) {
    throw new Problem(at, what + " " + String(id) + " does not match " + DECLARATION_ID.source);
  }
}

function length(value: string, min: number, max: number, what: string, at: string): void {
  if (typeof value !== "string" || value.length < min || value.length > max) {
    throw new Problem(at, what + " must be " + min + "-" + max + " characters");
  }
}

function range(value: unknown, min: number, max: number, what: string, at: string): void {
  if (typeof value !== "number" || !Number.isInteger(value) || value < min || value > max) {
    throw new Problem(at, what + " must be " + min + ".." + max);
  }
}

function port(p: unknown, at: string): void {
  range(p, 1, 65535, "port", at);
}

function bool(value: unknown, what: string, at: string): boolean {
  if (value === undefined) return false;
  if (typeof value !== "boolean") throw new Problem(at, what + " must be true or false");
  return value;
}

function strings(value: readonly string[] | undefined, what: string, at: string): string[] {
  if (value === undefined) return [];
  if (!Array.isArray(value) || value.some((v) => typeof v !== "string")) throw new Problem(at, what + " must be a list of strings");
  return [...value];
}

function unique<T>(values: T[], what: string, at: string): T[] {
  const seen = new Set<T>();
  for (const v of values) {
    if (seen.has(v)) throw new Problem(at, what + " " + String(v) + " is listed twice");
    seen.add(v);
  }
  return values;
}
