// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// Test helpers: throwaway tool projects under test/.work (so they resolve this SDK through the workspace), and the
// packager run over them.
import * as fs from "node:fs";
import * as path from "node:path";
import { fileURLToPath } from "node:url";
import { randomBytes } from "node:crypto";
import build from "../dist/cjs/build.js";

export const SDK = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
export const REPO = path.resolve(SDK, "..", "..", "..");
export const VECTORS = path.join(REPO, "tool-sdk", "spec", "descriptor", "vectors");
export const CONFORMANCE = path.join(REPO, "tool-sdk", "conformance");
export const WORK = path.join(SDK, "test", ".work");

/** A throwaway project with `files` (relative path to text) and a package.json; returns its directory. */
export function project(files, { generation = 1, name = "demo-tools", version = "1.0.0", toolgate } = {}) {
  const dir = path.join(WORK, randomBytes(6).toString("hex"));
  fs.mkdirSync(dir, { recursive: true });
  const config = toolgate ?? (generation === undefined ? {} : { descriptorGeneration: generation });
  fs.writeFileSync(path.join(dir, "package.json"), JSON.stringify({ name, version, private: true, type: "module", toolgate: config }, null, 2));
  for (const [file, text] of Object.entries(files)) {
    fs.mkdirSync(path.dirname(path.join(dir, file)), { recursive: true });
    fs.writeFileSync(path.join(dir, file), text);
  }
  return dir;
}

/** Runs the packager's analyse, bundle and describe steps; returns the description. */
export async function describe(files, options) {
  const dir = project(files, options);
  return (await build.prepare(dir)).description;
}

/** Runs the packager and returns its error text; fails when the project packages. */
export async function failure(files, options) {
  try {
    await describe(files, options);
  } catch (e) {
    if (e instanceof build.BuildError) return e.message;
    throw e;
  }
  throw new Error("the project packaged");
}

/** The canonical descriptor text of one described tool. */
export function canonical(description, name) {
  const t = description.tools.find((x) => x.name === name);
  if (t === undefined) throw new Error("no tool " + name);
  return jcs(t.descriptor);
}

function jcs(value) {
  if (Array.isArray(value)) return "[" + value.map(jcs).join(",") + "]";
  if (value !== null && typeof value === "object") {
    return "{" + Object.keys(value).sort().map((k) => JSON.stringify(k) + ":" + jcs(value[k])).join(",") + "}";
  }
  return JSON.stringify(value);
}

/** The package declaration and a destination most test projects share. */
export const PACKAGE = `
import { toolPackage, destination, httpRoute } from "@olo-labs/toolgate-sdk";
export default toolPackage({ id: "acme.demo", name: "Demo", publisher: "Acme" });
export const api = destination({ id: "api", hostSetting: "ApiHost" });
export const itemsGet = httpRoute({ id: "items.get", destination: "api", method: "GET", path: "/items/{id:[0-9]+}" });
`;
