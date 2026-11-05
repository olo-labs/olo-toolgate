// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// The package itself: the ESM and CommonJS entry points are one module instance (authoring/typescript.md, "Errors"),
// the export map, the error hierarchy of errors.md, and the versions it carries.
import { test } from "node:test";
import assert from "node:assert/strict";
import { createRequire } from "node:module";
import * as fs from "node:fs";
import * as path from "node:path";
import * as esm from "@olo-labs/toolgate-sdk";
import { REPO, SDK } from "./helpers.mjs";

const require = createRequire(import.meta.url);
const cjs = require("@olo-labs/toolgate-sdk");

test("instanceofWorksAcrossTheEsmAndCommonJsBuilds", () => {
  const e = new esm.ToolBusinessError("ORDER_NOT_FOUND", "No such order", { id: "1" });
  assert.ok(e instanceof cjs.ToolBusinessError);
  assert.ok(e instanceof cjs.ToolError);
  assert.ok(new cjs.UpstreamError("UPSTREAM_5XX", "orders.get", 503, undefined, false) instanceof esm.ToolError);
  assert.equal(esm.ToolResult, cjs.ToolResult);
  assert.equal(esm.serverTool, cjs.serverTool);
  assert.equal(esm.z, cjs.z);
});

test("theErrorHierarchyCarriesErrorsMdCodes", () => {
  assert.equal(new esm.ToolInputError("bad").code, "INVALID_ARGUMENT");
  assert.equal(new esm.ToolConfigurationError("bad").code, "CONFIGURATION");
  const b = new esm.ToolBusinessError("ALREADY_REFUNDED", "done", { at: 1 });
  assert.deepEqual([b.code, b.message, b.data, b.name], ["ALREADY_REFUNDED", "done", { at: 1 }, "ToolBusinessError"]);
  const r = new esm.ToolRetryableError("PSP_BUSY", "later", 500);
  assert.deepEqual([r.code, r.retryAfterMs], ["PSP_BUSY", 500]);
  const u = new esm.UpstreamError("UPSTREAM_TIMEOUT", "orders.get", undefined, "req-1", true);
  assert.deepEqual([u.code, u.route, u.status, u.upstreamRequestId, u.effectUncertain], ["UPSTREAM_TIMEOUT", "orders.get", undefined, "req-1", true]);
  assert.equal(u.message, "Upstream call on route orders.get failed: UPSTREAM_TIMEOUT");
  assert.equal(new esm.PlatformError("CANCELLED", "x").platformCode, "CANCELLED");
  for (const E of [esm.ToolInputError, esm.ToolConfigurationError]) assert.ok(new E("x") instanceof Error);
});

test("theExportMapHasImportAndRequireForEveryEntry", () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(SDK, "package.json"), "utf8"));
  for (const [entry, target] of Object.entries(manifest.exports)) {
    if (entry === "./package.json") continue;
    for (const condition of ["import", "require"]) {
      assert.ok(fs.existsSync(path.join(SDK, target[condition].default)), entry + " " + condition);
      assert.ok(fs.existsSync(path.join(SDK, target[condition].types)), entry + " " + condition + " types");
    }
  }
  assert.equal(manifest.dependencies.zod, "3.25.76", "zod is pinned exactly");
});

test("versionsMatchTheRepository", () => {
  const internal = require("../dist/cjs/version.js");
  const manifest = JSON.parse(fs.readFileSync(path.join(SDK, "package.json"), "utf8"));
  assert.equal(internal.SDK_VERSION, manifest.version);
  assert.equal(internal.SDK_VERSION, fs.readFileSync(path.join(REPO, "VERSION"), "utf8").trim());
  assert.equal(internal.CONTRACTS_VERSION, fs.readFileSync(path.join(REPO, "packages", "contracts", "VERSION"), "utf8").trim());
});

test("declarationsAreFrozenAndBranded", () => {
  const t = esm.serverTool({ name: "a.b", title: "T", description: "D", run: () => ({}) });
  assert.ok(Object.isFrozen(t));
  assert.ok(esm.isDeclaration(t, "serverTool"));
  assert.ok(!esm.isDeclaration({ name: "a.b" }));
  assert.throws(() => esm.serverTool("nope"), /takes an object literal/);
  assert.ok(esm.ToolResult.isToolResult(esm.ToolResult.text("x")));
  assert.deepEqual(esm.ToolResult.text("x").content, [{ type: "text", text: "x" }]);
});
