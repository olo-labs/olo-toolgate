// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// The descriptor vectors of tool-sdk/spec/descriptor/vectors, reproduced byte for byte by the TypeScript fixtures that
// mirror the Java processor's (test/fixtures), through the whole packager: analyse, bundle, describe.
import { test } from "node:test";
import assert from "node:assert/strict";
import * as fs from "node:fs";
import * as path from "node:path";
import build from "../dist/cjs/build.js";
import internal from "../dist/cjs/internal.js";
import { SDK, VECTORS, project } from "./helpers.mjs";

const expected = JSON.parse(fs.readFileSync(path.join(VECTORS, "expected.json"), "utf8"));
const fixture = (name) => path.join(SDK, "test", "fixtures", name);
const describedOnce = new Map();
async function described(name) {
  if (!describedOnce.has(name)) describedOnce.set(name, build.prepare(fixture(name)).then((p) => p.description));
  return describedOnce.get(name);
}

async function assertVector(fixtureName, tool, vector) {
  const d = await described(fixtureName);
  const t = d.tools.find((x) => x.name === tool);
  const bytes = internal.jcs.bytes(t.descriptor);
  const want = fs.readFileSync(path.join(VECTORS, vector.replace(".json", ".jcs")));
  assert.equal(bytes.toString("utf8"), want.toString("utf8"));
  assert.deepEqual(bytes, want);
  assert.equal(internal.jcs.sha256(bytes), expected.digests[vector]);
  assert.equal(t.toolDescriptorDigest, expected.digests[vector]);
}

test("clientToolMatchesTool01", () => assertVector("files", "files.checksum", "tool-01-checksum.json"));

test("serverToolMatchesTool02", () => assertVector("orders", "orders.lookup", "tool-02-orders-lookup.json"));

test("taskToolWithNonAsciiNamesMatchesTool03", () => assertVector("orders", "orders.refund", "tool-03-refund.json"));

test("setDigestMatchesSet01", async () => {
  const d = await described("orders");
  const entries = ["orders.lookup", "orders.refund"].map((name) => ({
    toolId: "acme.orders/" + name,
    toolDescriptorDigest: d.tools.find((t) => t.name === name).toolDescriptorDigest,
  }));
  assert.equal(internal.descriptorSetDigest(entries), expected.sets["set-01-orders.json"]);
  assert.equal(internal.jcs.sha256(internal.jcs.bytes(entries)), expected.sets["set-01-orders.json"]);
});

test("theSetDigestCoversEveryTool", async () => {
  const d = await described("orders");
  assert.equal(
    d.descriptorSetDigest,
    internal.descriptorSetDigest(d.tools.map((t) => ({ toolId: t.descriptor.toolId, toolDescriptorDigest: t.toolDescriptorDigest }))),
  );
});

test("sameSourceGivesIdenticalBytes", async () => {
  const a = await build.prepare(fixture("orders"));
  const bundleA = fs.readFileSync(a.bundle);
  const b = await build.prepare(fixture("orders"));
  assert.deepEqual(b.description, a.description);
  assert.deepEqual(fs.readFileSync(b.bundle), bundleA);
});

test("deploymentValuesAndExamplesDoNotChangeTheDigest", async () => {
  const dir = fixture("orders");
  const files = {};
  for (const f of fs.readdirSync(path.join(dir, "src"))) files["src/" + f] = fs.readFileSync(path.join(dir, "src", f), "utf8");
  files["src/package.ts"] = files["src/package.ts"].replace('defaultHost: "api.acme.com"', 'defaultHost: "staging.acme.example"');
  files["src/lookup.ts"] = files["src/lookup.ts"]
    .replace('import { errorDecl,', 'import { example, errorDecl,')
    .replace('  effect: "READ_ONLY",', '  effect: "READ_ONLY",\n  examples: [example({ arguments: { orderId: "123456" } })],');
  const changed = (await build.prepare(project(files))).description;
  const original = await described("orders");
  for (const name of ["orders.lookup", "orders.refund"]) {
    assert.equal(
      changed.tools.find((t) => t.name === name).toolDescriptorDigest,
      original.tools.find((t) => t.name === name).toolDescriptorDigest,
    );
  }
  assert.equal(changed.tools.find((t) => t.name === "orders.lookup").examples.length, 1);
});

test("anInferredMapOutputMatchesJavasMapOfObject", async () => {
  // orders.refund-status returns Record<string, unknown>, as the Java fixture returns Map<String, Object>.
  const d = await described("orders");
  assert.deepEqual(d.tools.find((t) => t.name === "orders.refund-status").descriptor.outputSchema, {
    type: "object",
    additionalProperties: {},
  });
});
