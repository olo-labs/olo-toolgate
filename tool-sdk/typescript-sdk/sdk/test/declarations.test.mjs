// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// The declaration checks of the descriptor builder; mirrors the Java processor's DeclarationTest. These run the
// builder over declaration objects directly; analysis.test.mjs runs the whole packager over TypeScript sources.
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  toolPackage, serverTool, clientTool, destination, httpAuth, httpRoute, tcpDestination, global, secret, jwtClaim,
  errorDecl, example, variable, idempotency, reconcile, z, binary, sensitive,
} from "../dist/esm/index.mjs";
import internal from "../dist/cjs/internal.js";

const pkg = toolPackage({ id: "acme.demo", name: "Demo", publisher: "Acme" });
const api = destination({ id: "api", hostSetting: "ApiHost" });
const itemsGet = httpRoute({ id: "items.get", destination: "api", method: "GET", path: "/items/{id:[0-9]+}" });

/** The Java fixture's TOOL: a server tool with one input, id. */
function tool(name, extra = {}) {
  return serverTool({ name, title: "T", description: "D", input: z.object({ id: z.string().describe("Id") }), run: () => ({}), ...extra });
}

function describe(exports, { generation = 1, packageDecl = pkg, network = { api, itemsGet } } = {}) {
  return internal.describeProject({
    descriptorGeneration: generation,
    modules: { "src/package.ts": { default: packageDecl, ...network }, "src/tools.ts": exports },
  });
}

function descriptor(d, name) {
  return internal.jcs.canonical(d.tools.find((t) => t.name === name).descriptor);
}

function fails(run, ...fragments) {
  let message;
  try {
    run();
  } catch (e) {
    message = e.message;
  }
  assert.ok(message !== undefined, "described");
  for (const f of fragments) assert.ok(message.includes(f), 'missing "' + f + '" in:\n' + message);
}

test("generationIsRequiredAndHasNoDefault", () => {
  fails(() => describe({ t: tool("items.list") }, { generation: null }), "descriptorGeneration", "it has no default");
  fails(() => describe({ t: tool("items.list") }, { generation: 2 }), "is not supported");
  describe({ t: tool("items.list") });
});

test("identifiersFollowTheirGrammars", () => {
  fails(() => describe({ t: tool("Items.list") }), "tool name Items.list");
  fails(() => describe({ t: tool("a.b.c.d") }), "tool name a.b.c.d");
  describe({ t: tool("a.b.c") });
  fails(() => describe({ t: tool("items.list", { input: z.object({ "1id": z.string().describe("x") }) }) }), "input name 1id");
  fails(
    () => describe({ t: tool("items.list") }, { network: { api, bad: httpRoute({ id: "Bad", destination: "api", method: "GET", path: "/x" }) } }),
    "route id Bad",
  );
  fails(() => describe({ t: tool("items.list", { errors: [errorDecl({ code: "AB", description: "x" })] }) }), "error code AB");
  fails(() => describe({ t: tool("items.list") }, { packageDecl: toolPackage({ id: "Acme.demo", name: "D", publisher: "A" }) }), "package id Acme.demo");
  fails(() => describe({ t: tool("items.list") }, { packageDecl: toolPackage({ id: "acme", name: "D", publisher: "A" }) }), "package id acme");
});

test("toolIdIsAtMost128Characters", () => {
  // "acme.demo/" is 10 characters, so a 118-character local name gives a 128-character toolId.
  const seg = "a".repeat(38);
  describe({ t: tool(seg + "." + seg + "." + "c".repeat(40)) });
  fails(() => describe({ t: tool(seg + "." + seg + "." + "c".repeat(41)) }), "longer than 128");
});

test("reservedPrefixesAreRejected", () => {
  fails(() => describe({ t: tool("items.list") }, { packageDecl: toolPackage({ id: "toolgate.demo", name: "D", publisher: "A" }) }), "reserved prefix toolgate.");
  fails(() => describe({ t: tool("items.list") }, { packageDecl: toolPackage({ id: "hotfolder.demo", name: "D", publisher: "A" }) }), "reserved prefix hotfolder.");
  fails(() => describe({ t: tool("builtin.items") }), "reserved prefix builtin.");
});

test("variableNamesAreUniqueIgnoringCase", () => {
  fails(() => describe({ t: tool("items.list", { globals: [global({ name: "ID", type: "STRING" })] }) }), "input id", "global ID");
  fails(
    () => describe({ t: tool("items.list", { secrets: [secret({ name: "Token" })], jwt: [jwtClaim({ name: "token", claim: "sub" })] }) }),
    "claim token",
    "secret Token",
  );
});

test("toolLevelDeclarationsNeverOverrideScopeLevelOnes", () => {
  const scoped = toolPackage({ id: "acme.demo", name: "Demo", publisher: "Acme", globals: [global({ name: "Limit", type: "INT" })] });
  fails(
    () => describe({ t: tool("items.list", { globals: [global({ name: "Limit", type: "INT", defaultValue: "5" })] }) }, { packageDecl: scoped }),
    "cannot override",
    "global Limit",
  );
});

test("toolAttributesTakeTheSpecifiedDefaults", () => {
  const d = describe({
    s: tool("items.get", { routes: ["items.get"] }),
    c: clientTool({ name: "items.local", title: "T", description: "D", run: () => ({}) }),
  });
  const server = descriptor(d, "items.get");
  const client = descriptor(d, "items.local");
  assert.ok(server.includes('"effect":{"destructive":false,"openWorld":true,"safety":"UNKNOWN"}'), server);
  assert.ok(client.includes('"effect":{"destructive":false,"openWorld":false,"safety":"UNKNOWN"}'), client);
  for (const x of [server, client]) {
    assert.ok(x.includes('"execution":{"async":false,"cancelIfAbandoned":false,"maxDurationMs":30000,"stateless":false}'), x);
    assert.ok(x.includes('"businessKey":{"namespace":"AGENT","required":false}'), x);
  }
  assert.ok(client.includes('"tiers":["OCI_SANDBOX"]'), client);
  assert.ok(!server.includes('"tiers"'), server);
  assert.ok(server.includes('"permissions":["HTTP_ROUTE"]'), server);
  assert.ok(server.includes('"destinations":[{"hostSetting":"ApiHost","id":"api","port":443,"spkiPins":[]}]'), server);
});

test("nonIdempotentDestructiveToolsRequireABusinessKey", () => {
  const d = describe({
    a: tool("items.delete", { effect: "NON_IDEMPOTENT", destructive: true }),
    b: tool("items.create", { effect: "NON_IDEMPOTENT" }),
    c: tool("items.purge", { effect: "NON_IDEMPOTENT", destructive: true, requiresBusinessKey: false }),
  });
  assert.ok(descriptor(d, "items.delete").includes('"businessKey":{"namespace":"AGENT","required":true}'));
  assert.ok(descriptor(d, "items.create").includes('"businessKey":{"namespace":"AGENT","required":false}'));
  assert.ok(descriptor(d, "items.purge").includes('"businessKey":{"namespace":"AGENT","required":false}'));
});

test("asyncToolsDefaultTo900000Ms", () => {
  const d = describe({ a: tool("items.export", { async: true }) });
  assert.ok(descriptor(d, "items.export").includes('"maxDurationMs":900000'));
  fails(() => describe({ a: tool("items.slow", { maxDurationMs: 60001 }) }), "maxDurationMs must be 1..60000 for sync tools");
  fails(() => describe({ a: tool("items.slow", { async: true, maxDurationMs: 900001 }) }), "maxDurationMs must be 1..900000 for async tools");
  fails(() => describe({ a: tool("items.slow", { cancelIfAbandoned: true }) }), "cancelIfAbandoned applies to async tools only");
});

test("titleAndTextAreExclusiveAliases", () => {
  const d = describe({ a: tool("items.list", { title: undefined, text: "Listed" }) });
  assert.ok(descriptor(d, "items.list").includes('"title":"Listed"'));
  assert.ok(!descriptor(d, "items.list").includes('"text"'));
  fails(() => describe({ a: tool("items.list", { text: "Also" }) }), "exactly one of title and text");
  fails(() => describe({ a: tool("items.list", { title: undefined }) }), "exactly one of title and text");
});

test("clientToolsCannotDeclareEgressOrSecrets", () => {
  const client = (extra) => clientTool({ name: "items.local", title: "T", description: "D", run: () => ({}), ...extra });
  fails(() => describe({ c: client({ routes: ["items.get"] }) }), "client tool items.local cannot declare routes");
  fails(() => describe({ c: client({ tcp: ["db"] }) }), "client tool items.local cannot declare tcp");
  fails(() => describe({ c: client({ secrets: [secret({ name: "Token" })] }) }), "client tool cannot use secrets");
  const scoped = toolPackage({ id: "acme.demo", name: "Demo", publisher: "Acme", secrets: [secret({ name: "Token" })] });
  fails(() => describe({ c: client({}) }, { packageDecl: scoped }), "client tool cannot use secrets; found Token");
  fails(() => describe({ c: client({ auth: "key" }) }), "clientTool has no member auth");
  fails(() => describe({ s: tool("items.list", { tiers: ["WASI"] }) }), "tiers apply to client tools only");
});

test("typedAndGroupedFormsGiveIdenticalBytes", () => {
  const typed = describe({
    t: serverTool({
      name: "items.find",
      title: "T",
      description: "D",
      input: z.object({
        orderId: z.string().regex(/^[0-9]+$/).describe("Order number"),
        count: z.number().int().min(1).optional().describe("How many"),
        sort: z.enum(["ASC", "DESC"]).default("ASC").describe("Sort"),
        token: sensitive(z.string()).describe("Token"),
      }),
      run: () => ({}),
    }),
  });
  const grouped = describe({
    t: serverTool({
      name: "items.find",
      title: "T",
      description: "D",
      variables: {
        inputs: [
          variable({ name: "orderId", description: "Order number", type: "STRING", pattern: "^[0-9]+$" }),
          variable({ name: "count", description: "How many", type: "INT", min: 1, required: false }),
          variable({ name: "sort", description: "Sort", type: "ENUM", enumValues: ["ASC", "DESC"], defaultValue: "ASC" }),
          variable({ name: "token", description: "Token", type: "STRING", sensitive: true }),
        ],
      },
      run: () => ({}),
    }),
  });
  assert.equal(descriptor(typed, "items.find"), descriptor(grouped, "items.find"));
  assert.equal(typed.tools[0].digest, grouped.tools[0].digest);
  const d = descriptor(typed, "items.find");
  assert.ok(d.includes('"count":{"description":"How many","maximum":2147483647,"minimum":1,"type":"integer"}'), d);
  assert.ok(d.includes('"required":["orderId","token"]'), d);
  assert.ok(d.includes('"x-toolgate-sensitive":true'), d);
  assert.equal(typed.tools[0].typed, true);
  assert.equal(grouped.tools[0].typed, false);
});

test("objectsAreClosedUnlessExplicitMaps", () => {
  const d = descriptor(
    describe({
      t: serverTool({
        name: "items.put",
        title: "T",
        description: "D",
        input: z.object({
          address: z.object({ street: z.string(), zip: z.string().optional() }).describe("Where"),
          counts: z.record(z.string(), z.number().int()).describe("Counts"),
          ids: z.array(z.number().int().safe()).describe("Ids"),
          any: z.record(z.string(), z.unknown()).describe("Anything"),
          blob: binary().describe("Bytes"),
        }),
        run: () => ({}),
      }),
    }),
    "items.put",
  );
  assert.ok(
    d.includes('"address":{"additionalProperties":false,"description":"Where","properties":{"street":{"type":"string"},"zip":{"type":"string"}},"required":["street"],"type":"object"}'),
    d,
  );
  assert.ok(d.includes('"counts":{"additionalProperties":{"maximum":2147483647,"minimum":-2147483648,"type":"integer"},"description":"Counts","type":"object"}'), d);
  assert.ok(d.includes('"ids":{"description":"Ids","items":{"maximum":9007199254740991,"minimum":-9007199254740991,"type":"integer"},"type":"array"}'), d);
  assert.ok(d.includes('"any":{"additionalProperties":{},"description":"Anything","type":"object"}'), d);
  assert.ok(d.includes('"blob":{"contentEncoding":"base64","description":"Bytes","type":"string"}'), d);
});

test("atMostEightExamples", () => {
  const eight = Array.from({ length: 8 }, () => example({ arguments: "{}" }));
  describe({ t: tool("items.list", { examples: eight }) });
  fails(() => describe({ t: tool("items.list", { examples: [...eight, example({ arguments: {} })] }) }), "9 examples");
  fails(() => describe({ t: tool("items.list", { examples: [example({ arguments: {}, expectedOutput: {}, expectedError: "X" })] }) }), "either an output or an error");
});

test("routesAreChecked", () => {
  const r = (path, extra = {}) => ({ api, r: httpRoute({ id: "items.all", destination: "api", method: "GET", path, ...extra }) });
  fails(() => describe({ t: tool("items.list") }, { network: r("/items/{rest:.*}") }), "may not use .*");
  fails(() => describe({ t: tool("items.list") }, { network: r("/items/{rest:.+}") }), "may not use .+");
  fails(() => describe({ t: tool("items.list") }, { network: r("/items/{id}") }), "path variable id needs a pattern");
  describe({ t: tool("items.list") }, { network: r("/items/{id:[0-9]+}") });
  fails(() => describe({ t: tool("items.list") }, { network: r("/items", { headers: ["Cookie"] }) }), "may not allow header Cookie");
  fails(() => describe({ t: tool("items.list") }, { network: r("/items", { headers: ["X-Forwarded-For"] }) }), "may not allow header X-Forwarded-For");
  fails(
    () => describe({ t: tool("items.list") }, { network: { r: httpRoute({ id: "items.all", destination: "nowhere", method: "GET", path: "/items" }) } }),
    "undeclared destination nowhere",
  );
  fails(() => describe({ t: tool("items.list", { routes: ["items.missing"] }) }), "undeclared route items.missing");
  fails(() => describe({ t: tool("items.list") }, { network: r("items") }), "path must start with /");
  fails(() => describe({ t: tool("items.list") }, { network: r("/items/{a:[0-9]+}/{a:[0-9]+}") }), "repeats path variable a");
  const key = httpAuth({ id: "key", type: "API_KEY", header: "X-Key", secret: "Key" });
  fails(
    () => describe({ t: tool("items.list") }, { network: { api, key, r: httpRoute({ id: "items.all", destination: "api", method: "GET", path: "/items", headers: ["x-key"], auth: "key" }) } }),
    "may not allow header x-key",
  );
  fails(
    () => describe({ t: tool("items.list", { routes: ["items.all"] }) }, { network: { api, key, r: httpRoute({ id: "items.all", destination: "api", method: "GET", path: "/items", auth: "key" }) } }),
    "auth key uses secret Key, which tool items.list does not declare",
  );
  fails(() => describe({ t: tool("items.list") }, { network: { api, a: api, b: destination({ id: "api", hostSetting: "Other" }) } }), "destination id api is declared twice");
});

test("permissionsAreDerived", () => {
  const exchange = httpAuth({ id: "exchange", type: "TOKEN_EXCHANGE", audience: "api" });
  const mine = httpRoute({ id: "items.mine", destination: "api", method: "GET", path: "/me", auth: "exchange" });
  const db = tcpDestination({ id: "db", hostSetting: "DbHost", port: 5432 });
  const scoped = toolPackage({ id: "acme.demo", name: "Demo", publisher: "Acme", secrets: [secret({ name: "Personal", scope: "USER" })] });
  const d = describe({ t: tool("items.mine", { routes: ["items.mine"], tcp: ["db"] }) }, { packageDecl: scoped, network: { api, exchange, mine, db } });
  assert.ok(
    descriptor(d, "items.mine").includes('"permissions":["CREDENTIALS_IN_SANDBOX","HTTP_ROUTE","TCP","TOKEN_EXCHANGE","USER_SECRET"]'),
    descriptor(d, "items.mine"),
  );
});

test("constraintsFitTheValueType", () => {
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "STRING", min: 1 })] }) }), "X: min and max apply to INT, LONG and NUMBER only");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "INT", pattern: "a" })] }) }), "X: minLength, maxLength and pattern apply to STRING and URI only");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "URI", format: "email" })] }) }), "X: format applies to STRING only");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "STRING", enumValues: ["A"] })] }) }), "X: enumValues applies to a String typed as ENUM");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "INT", defaultValue: "ten" })] }) }), "X default: ten is not an integer");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "INT", max: 5, defaultValue: "6" })] }) }), "X default: 6 is outside -2147483648..5");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "OBJECT", defaultValue: "{}" })] }) }), "OBJECT values cannot have a default");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "STRING", pattern: "(" })] }) }), "X: pattern does not compile");
  fails(() => describe({ t: tool("a.b", { globals: [global({ name: "X", type: "ENUM" })] }) }), "global X of type ENUM needs enumValues");
  const d = descriptor(describe({ t: tool("a.b", { globals: [global({ name: "X", type: "INT", min: "1", defaultValue: "3", sensitive: true })] }) }), "a.b");
  assert.ok(d.includes('"globals":[{"default":3,"minimum":1,"name":"X","required":true,"sensitive":true,"type":"INT"}]'), d);
});

test("toolLevelChecks", () => {
  fails(() => describe({ t: tool("items.list", { businessKeyArgument: "nope" }) }), "businessKeyArgument nope is not an input of the tool");
  fails(() => describe({ t: tool("items.list", { maxConcurrency: 0 }) }), "maxConcurrency must be 1..1048576");
  fails(() => describe({ t: tool("items.list", { idempotency: idempotency({ header: "Bad Header" }) }) }), "idempotency header is not a header name");
  fails(() => describe({ t: tool("items.list", { reconcile: reconcile({ tool: "items.list" }) }) }), "a tool cannot reconcile itself");
  fails(() => describe({ t: tool("items.list", { reconcile: reconcile({ tool: "nope" }) }) }), "reconcile names unknown tool nope");
  fails(() => describe({ a: tool("items.list"), b: tool("items.list") }), "tool items.list is declared twice");
  fails(() => describe({ t: tool("items.list", { title: "x".repeat(101) }) }), "title must be 1-100 characters");
  fails(() => describe({ t: tool("items.list", { useWhen: "x".repeat(513) }) }), "useWhen is longer than 512 characters");
  fails(() => describe({ t: tool("items.list", { input: z.object({ id: z.string() }) }) }), "input id needs a description");
  fails(() => describe({ t: tool("items.list", { toolsets: ["a", "a"] }) }), "toolset a is listed twice");
  fails(() => describe({ t: tool("items.list", { effect: "SAFE" }) }), "serverTool effect SAFE is not one of");
  fails(() => describe({ t: tool("items.list", { colour: "red" }) }), "serverTool has no member colour");
});

test("exactlyOnePackage", () => {
  fails(() => internal.describeProject({ descriptorGeneration: 1, modules: { "a.ts": { t: tool("items.list") } } }), "exactly one toolPackage is required per package; found 0");
  fails(
    () => internal.describeProject({ descriptorGeneration: 1, modules: { "a.ts": { pkg, other: toolPackage({ id: "acme.other", name: "O", publisher: "A" }) } } }),
    "found 2",
  );
  // A re-export is the same declaration, not a second one.
  internal.describeProject({ descriptorGeneration: 1, modules: { "a.ts": { default: pkg, t: tool("items.list") }, "index.ts": { pkg } } });
});

test("zodConstructsConvertAsDescriptor5Lists", () => {
  const convert = (schema) => {
    const d = describe({ t: tool("a.b", { input: z.object({ v: schema.describe("V") }) }) });
    const p = d.tools[0].descriptor.inputSchema.properties.v;
    delete p.description;
    return p;
  };
  assert.deepEqual(convert(z.string().min(1).max(5)), { type: "string", minLength: 1, maxLength: 5 });
  assert.deepEqual(convert(z.string().length(3)), { type: "string", minLength: 3, maxLength: 3 });
  assert.deepEqual(convert(z.string().regex(/^a+$/)), { type: "string", pattern: "^a+$" });
  assert.deepEqual(convert(z.string().email()), { type: "string", format: "email" });
  assert.deepEqual(convert(z.string().url()), { type: "string", format: "uri" });
  assert.deepEqual(convert(z.string().uuid()), { type: "string", format: "uuid" });
  assert.deepEqual(convert(z.string().date()), { type: "string", format: "date" });
  assert.deepEqual(convert(z.string().datetime({ offset: true })), { type: "string", format: "date-time" });
  assert.deepEqual(convert(z.string().duration()), { type: "string", format: "duration" });
  assert.deepEqual(convert(z.number()), { type: "number" });
  assert.deepEqual(convert(z.number().min(0.5).max(2)), { type: "number", minimum: 0.5, maximum: 2 });
  assert.deepEqual(convert(z.number().int()), { type: "integer", minimum: -2147483648, maximum: 2147483647 });
  assert.deepEqual(convert(z.number().int().min(1)), { type: "integer", minimum: 1, maximum: 2147483647 });
  assert.deepEqual(convert(z.number().int().safe()), { type: "integer", minimum: -9007199254740991, maximum: 9007199254740991 });
  assert.deepEqual(convert(z.boolean()), { type: "boolean" });
  assert.deepEqual(convert(z.enum(["B", "A"])), { type: "string", enum: ["B", "A"] });
  assert.deepEqual(convert(z.array(z.string()).min(1).max(3)), { type: "array", items: { type: "string" }, minItems: 1, maxItems: 3 });
  assert.deepEqual(convert(z.object({ a: z.string() })), { type: "object", properties: { a: { type: "string" } }, required: ["a"], additionalProperties: false });
  assert.deepEqual(convert(z.record(z.string(), z.boolean())), { type: "object", additionalProperties: { type: "boolean" } });
  assert.deepEqual(convert(z.string().default("x")), { type: "string", default: "x" });
  assert.deepEqual(convert(binary()), { type: "string", contentEncoding: "base64" });
});

test("disallowedZodConstructsFailNamingTheConstruct", () => {
  const input = (schema) => () => describe({ t: tool("a.b", { input: z.object({ v: schema.describe("V") }) }) });
  fails(input(z.union([z.string(), z.number()])), "input v: unions is not allowed");
  fails(input(z.intersection(z.object({}), z.object({}))), "input v: intersections is not allowed");
  fails(input(z.string().nullable()), "input v: nullable is not allowed");
  fails(input(z.string().transform((s) => s)), "transform, refine and preprocess is not allowed");
  fails(input(z.string().refine(() => true)), "transform, refine and preprocess is not allowed");
  fails(input(z.preprocess((s) => s, z.string())), "transform, refine and preprocess is not allowed");
  fails(input(z.lazy(() => z.string())), "input v: lazy is not allowed");
  fails(input(z.any()), "input v: any is not allowed");
  fails(input(z.unknown()), "input v: unknown is not allowed");
  fails(input(z.bigint()), "input v: bigint is not allowed");
  fails(input(z.date()), "input v: z.date() is not allowed");
  fails(input(z.string().startsWith("a")), "input v: .startsWith() is not allowed");
  fails(input(z.string().datetime()), "only .datetime({ offset: true })");
  fails(input(z.number().positive()), "exclusive bounds");
  fails(input(z.number().multipleOf(2)), "input v: .multipleOf() is not allowed");
  fails(input(z.string().regex(/a/i)), "cannot carry flags");
  fails(input(z.object({}).passthrough()), "input v: .passthrough() is not allowed");
  fails(input(z.coerce.string()), "z.coerce is not allowed");
  fails(input(z.string().email().url()), "at most one format");
  fails(() => describe({ t: tool("a.b", { input: z.string() }) }), "input must be a Zod");
  fails(() => describe({ t: tool("a.b", { output: z.string() }) }), "output must be a Zod object or record schema");
});

test("longInputsAreSafeIntegers", () => {
  fails(() => describe({ t: tool("a.b", { input: z.object({ v: z.number().int().max(2 ** 53).describe("V") }) }) }), "is outside -9007199254740991..9007199254740991");
});
