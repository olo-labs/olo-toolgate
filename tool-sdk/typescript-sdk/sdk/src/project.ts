// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * The registered project: the module namespaces the bundle's entry imports, described once per process. The runtime,
 * the examples runner and ToolHarness all read it from here, so a test bundle and the tool bundle see the same tools.
 */

import { describeProject, type DescribedPackage, type ProjectInput } from "./descriptor/describe";

const KEY = Symbol.for("io.ololabs.toolgate/project");

export interface RegisteredProject extends ProjectInput {
  readonly projectName?: string;
  readonly projectVersion?: string;
}

interface Slot {
  input: RegisteredProject;
  described?: DescribedPackage;
}

/** Registers the project of this process; the entry module calls it before anything else runs. */
export function registerProject(input: RegisteredProject): void {
  (globalThis as Record<symbol, unknown>)[KEY] = { input } satisfies Slot;
}

/** The registered project, or undefined. */
export function registeredProject(): RegisteredProject | undefined {
  return ((globalThis as Record<symbol, unknown>)[KEY] as Slot | undefined)?.input;
}

/** The registered project, described (once). Throws Problems on declaration errors. */
export function describedProject(): DescribedPackage | undefined {
  const slot = (globalThis as Record<symbol, unknown>)[KEY] as Slot | undefined;
  if (slot === undefined) return undefined;
  if (slot.described === undefined) slot.described = describeProject(slot.input);
  return slot.described;
}

/** The JSON the packager reads from `--toolgate-describe`. */
export function describeJson(described: DescribedPackage): Record<string, unknown> {
  return {
    id: described.id,
    name: described.name,
    publisher: described.publisher,
    ...(described.description !== undefined ? { description: described.description } : {}),
    descriptorSetDigest: described.descriptorSetDigest,
    routes: described.routes,
    tools: described.tools.map((t) => ({
      name: t.name,
      at: t.at,
      toolDescriptorDigest: t.digest,
      descriptor: t.descriptor,
      facts: t.facts,
      examples: t.examples,
    })),
  };
}
