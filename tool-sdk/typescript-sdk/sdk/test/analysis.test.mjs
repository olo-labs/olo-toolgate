// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// The packager's static checks over TypeScript and JavaScript sources: module-level literal declarations, type errors
// (the SDK's generics narrow getters to declared names), output inference, and the route and value-name analyser,
// which mirrors the Java processor's AnalysisTest.
import { test } from "node:test";
import assert from "node:assert/strict";
import * as fs from "node:fs";
import * as path from "node:path";
import { PACKAGE, describe, failure, project } from "./helpers.mjs";
import build from "../dist/cjs/build.js";

const ROUTES = `
import { httpRoute } from "@olo-labs/toolgate-sdk";
export const itemsList = httpRoute({ id: "items.list", destination: "api", method: "GET", path: "/items" });
export const adminGet = httpRoute({ id: "admin.get", destination: "api", method: "GET", path: "/admin" });
`;

/** A server tool listing items.get and items.list, whose body is `statement`, plus `decls` in its object literal. */
function handler(statement, decls = "", extra = {}, ext = "ts") {
  return {
    "src/package.ts": PACKAGE,
    "src/routes.ts": ROUTES,
    ...extra,
    ["src/tools." + ext]: `
import { serverTool, global, secret, jwtClaim, z } from "@olo-labs/toolgate-sdk";
export const get = serverTool({
  name: "items.get", title: "T", description: "D", routes: ["items.get", "items.list"],
  ${decls}
  input: z.object({ id: z.string().describe("Id") }),
  run: async (args, ctx) => {
    ${statement}
    return {};
  },
});
`,
  };
}

const fails = async (files, ...fragments) => {
  const message = await failure(files);
  for (const f of fragments) assert.ok(message.includes(f), 'missing "' + f + '" in:\n' + message);
  return message;
};

test("aLiteralCallMatchingOneListedRoutePackages", async () => {
  await describe(handler('await ctx.http.get("api", "/items/{id}").pathVar("id", args.id).send();'));
  await describe(handler('await ctx.http.get("api", "/items/{id:[0-9]+}").pathVar("id", args.id).send();'));
  await describe(handler('const http = ctx.http; await http.get("api", "/items").send();'));
});

test("aConcatenatedTemplateFailsAtTheCallSite", async () => {
  const m = await fails(handler('await ctx.http.get("api", "/items/" + args.id).send();'), "string literals");
  assert.match(m, /src\/tools\.ts:\d+:\d+: /);
  await fails(handler("await ctx.http.get(\"api\", `/items/${args.id}`).send();"), "string literals");
});

test("aTemplateOrDestinationFromAGlobalFails", async () => {
  await fails(
    handler('await ctx.http.get("api", ctx.global.getString("Path")).send();', 'globals: [global({ name: "Path", type: "STRING" })],'),
    "string literals",
  );
  await fails(
    handler('await ctx.http.get(ctx.global.getString("Dest"), "/items").send();', 'globals: [global({ name: "Dest", type: "STRING" })],'),
    "string literals",
  );
});

test("aTemplateMatchingTwoRoutesFailsNamingBoth", async () => {
  await fails(
    handler('await ctx.http.get("api", "/items/{id}").send();', "", {
      "src/also.ts": 'import { httpRoute } from "@olo-labs/toolgate-sdk";\nexport const also = httpRoute({ id: "items.also", destination: "api", method: "GET", path: "/items/{id:[a-z]+}" });\n',
    }),
    "more than one route",
    "items.get",
    "items.also",
  );
});

test("aRouteTheToolDoesNotListFails", async () => {
  await fails(handler('await ctx.http.get("api", "/admin").send();'), "route admin.get", "does not list");
  await fails(handler('await ctx.http.post("api", "/items").send();'), "matches no declared route", "candidates");
});

test("callsOutsideAHandlerFail", async () => {
  await fails(
    {
      "src/package.ts": PACKAGE,
      "src/tools.ts": `
import { serverTool, type HttpClient } from "@olo-labs/toolgate-sdk";
export const get = serverTool({ name: "items.get", title: "T", description: "D", routes: ["items.get"], run: async () => ({}) });
export function helper(http: HttpClient) { return http.get("api", "/items/{id}").send(); }
`,
    },
    "outside a tool handler",
  );
});

test("computedAccessToTheNetworkApiFails", async () => {
  await fails(handler('await (ctx.http as any)["get"]("api", "/items").send(); await ctx.http["route"]("items.get").send();'), "computed access to HttpClient");
});

test("literalValueNamesMustBeDeclaredAndReadWithTheirType", async () => {
  // TypeScript: the SDK's generic types make these type errors.
  await describe(handler('ctx.global.getInt("Limit"); ctx.global.getLong("Limit");', 'globals: [global({ name: "Limit", type: "INT" })],'));
  await fails(handler('ctx.global.getInt("Limt");', 'globals: [global({ name: "Limit", type: "INT" })],'), "TS2345", '"Limt"');
  await fails(handler('ctx.global.getString("Limit");', 'globals: [global({ name: "Limit", type: "INT" })],'), "TS2345");
  await fails(handler('ctx.secret.getString("Tokn");', 'secrets: [secret({ name: "Token" })],'), "TS2345");
  await fails(handler('ctx.jwt.getInt("tenant");', 'jwt: [jwtClaim({ name: "tenant", claim: "tid" })],'), "TS2345");
  await describe(handler('ctx.jwt.getString("tenant"); ctx.jwt.userName;', 'jwt: [jwtClaim({ name: "tenant", claim: "tid" })],'));
  await fails(handler('ctx.getString("nope");'), "TS2345");
  await describe(handler('ctx.getString("id");'));
  // JavaScript: the analyser reports them with Java's messages.
  const js = (statement, decls) => handler(statement, decls, {}, "js");
  await describe(js('ctx.global.getInt("Limit");', 'globals: [global({ name: "Limit", type: "INT" })],'));
  await fails(js('ctx.global.getInt("Limt");', 'globals: [global({ name: "Limit", type: "INT" })],'), "declares no global Limt", "declared: Limit");
  await fails(js('ctx.global.getString("Limit");', 'globals: [global({ name: "Limit", type: "INT" })],'), "global Limit of tool items.get is INT; getString does not read it");
  await fails(js('ctx.secret.getString("Tokn");', 'secrets: [secret({ name: "Token" })],'), "declares no secret Tokn");
  await fails(js('ctx.jwt.getInt("tenant");', 'jwt: [jwtClaim({ name: "tenant", claim: "tid" })],'), "claim tenant", "is STRING");
  await fails(js('ctx.getString("nope");'), "declares no input nope");
  // A name the build cannot see is left to the runtime.
  await describe(js('const n = "no" + "pe"; ctx.global.getString(n);'));
});

test("declarationsMustBeExportedModuleConstants", async () => {
  const tool = (prefix, suffix = "") => ({
    "src/package.ts": PACKAGE,
    "src/tools.ts": `
import { serverTool, global, z } from "@olo-labs/toolgate-sdk";
${prefix} serverTool({ name: "items.list", title: "T", description: "D", run: async () => ({}) })${suffix};
`,
  });
  await describe(tool("export const list ="));
  await fails(tool("const list ="), "must be an exported module-level constant");
  await fails(tool("export let list ="), "must be an exported module-level constant");
  await fails(tool("export const all = [1].map(() =>", ")"), "must be an exported module-level constant");
  await fails(tool("export const maybe = Math.random() > 0.5 ? null :"), "must be an exported module-level constant");
  await fails(
    { "src/package.ts": PACKAGE, "src/tools.ts": 'import { serverTool } from "@olo-labs/toolgate-sdk";\nfor (const n of ["a.b"]) { serverTool({ name: n, title: "T", description: "D", run: async () => ({}) }); }\n' },
    "must be an exported module-level constant",
  );
  const member = (text) => ({
    "src/package.ts": PACKAGE,
    "src/tools.ts": `
import { serverTool, global, z } from "@olo-labs/toolgate-sdk";
const base = { title: "T" } as const;
const name = "items.list";
export const shared = global({ name: "Limit", type: "INT" });
export const list = serverTool({ name: "items.list", description: "D", ${text} run: async () => ({}) });
`,
  });
  await describe(member('title: "T", globals: [shared],'));
  await fails(member("...base,"), "cannot spread members");
  await fails(member('title: "T" + "x",'), "serverTool title must be a literal");
  await fails(member("title: base.title,"), "serverTool title must be a literal");
  await fails(member('title: "T", globals: [global({ name: name, type: "INT" })],'), "global name must be a literal");
});

test("everyZodPropertyCallsDescribe", async () => {
  await fails(
    {
      "src/package.ts": PACKAGE,
      "src/tools.ts": `
import { serverTool, z } from "@olo-labs/toolgate-sdk";
export const list = serverTool({ name: "items.list", title: "T", description: "D", input: z.object({ limit: z.number().int() }), run: async () => ({}) });
`,
    },
    "input limit needs a description",
  );
});

test("aZodMajorOtherThan3FailsPackaging", async () => {
  const dir = project({
    "src/package.ts": PACKAGE,
    "src/tools.ts": 'import { serverTool } from "@olo-labs/toolgate-sdk";\nexport const list = serverTool({ name: "items.list", title: "T", description: "D", run: async () => ({}) });\n',
    "node_modules/zod/package.json": JSON.stringify({ name: "zod", version: "4.1.0" }),
  });
  await assert.rejects(build.prepare(dir), /resolves zod 4\.1\.0; the SDK converts Zod 3 schemas only/);
});

test("outputSchemasAreInferredFromReturnTypes", async () => {
  const d = await describe({
    "src/package.ts": PACKAGE,
    "src/tools.ts": `
import { serverTool, ToolResult } from "@olo-labs/toolgate-sdk";
interface Item { sku: string; quantity: number; note?: string; kind: "A" | "B"; tags: string[]; meta: Record<string, boolean>; bytes: Uint8Array }
export const one = serverTool({ name: "items.one", title: "T", description: "D", run: async (): Promise<Item> => ({} as Item) });
export const raw = serverTool({ name: "items.raw", title: "T", description: "D", run: () => ToolResult.text("x") });
`,
  });
  assert.deepEqual(d.tools.find((t) => t.name === "items.one").descriptor.outputSchema, {
    type: "object",
    properties: {
      sku: { type: "string" },
      quantity: { type: "number" },
      note: { type: "string" },
      kind: { type: "string", enum: ["A", "B"] },
      tags: { type: "array", items: { type: "string" } },
      meta: { type: "object", additionalProperties: { type: "boolean" } },
      bytes: { type: "string", contentEncoding: "base64" },
    },
    required: ["bytes", "kind", "meta", "quantity", "sku", "tags"],
    additionalProperties: false,
  });
  assert.equal(d.tools.find((t) => t.name === "items.raw").descriptor.outputSchema, undefined);
  await fails(
    { "src/package.ts": PACKAGE, "src/tools.ts": 'import { serverTool } from "@olo-labs/toolgate-sdk";\nexport const any = serverTool({ name: "items.any", title: "T", description: "D", run: async () => JSON.parse("{}") });\n' },
    "cannot infer the output schema of tool items.any",
  );
  await fails(
    { "src/package.ts": PACKAGE, "src/tools.ts": 'import { serverTool } from "@olo-labs/toolgate-sdk";\nexport const u = serverTool({ name: "items.u", title: "T", description: "D", run: async () => ({ v: Math.random() > 1 ? "a" : 1 }) });\n' },
    "v is a union",
  );
});

test("typedNarrowingCoversGroupedFormAndClientTools", async () => {
  const files = (body) => ({
    "src/package.ts": PACKAGE,
    "src/tools.ts": `
import { serverTool, clientTool, variable, global, secret } from "@olo-labs/toolgate-sdk";
export const g = serverTool({
  name: "items.g", title: "T", description: "D",
  variables: { inputs: [variable({ name: "count", description: "C", type: "INT" })], globals: [global({ name: "Mode", type: "STRING" })] },
  run: async (ctx) => { ${body} return {}; },
});
`,
  });
  await describe(files('ctx.getInt("count"); ctx.global.getString("Mode");'));
  await fails(files('ctx.getString("count");'), "TS2345");
  await fails(files('ctx.global.getString("Other");'), "TS2345");
  await fails(
    {
      "src/package.ts": PACKAGE,
      "src/tools.ts": 'import { clientTool, secret } from "@olo-labs/toolgate-sdk";\nexport const c = clientTool({ name: "items.c", title: "T", description: "D", secrets: [secret({ name: "S" })], run: async () => ({}) });\n',
    },
    "TS2769",
  );
});

test("theBundleIsOneModuleThatRunsAlone", async () => {
  const dir = project({ "src/package.ts": PACKAGE, "src/tools.ts": 'import { serverTool } from "@olo-labs/toolgate-sdk";\nexport const list = serverTool({ name: "items.list", title: "T", description: "D", run: async () => ({ n: 1 }) });\n' });
  const p = await build.prepare(dir);
  const text = fs.readFileSync(p.bundle, "utf8");
  assert.ok(!/\bfrom\s+"(?!node:)/.test(text), "imports only node: built-ins");
  assert.ok(!text.includes(path.resolve(dir)), "no absolute paths");
  assert.equal(p.description.tools[0].descriptor.outputSchema.properties.n.type, "number");
});
