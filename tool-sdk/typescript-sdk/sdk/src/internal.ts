// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * @olo-labs/toolgate-sdk/internal: what the bundle's entry module, the testing package and the conformance tests use.
 * Not part of the authoring API.
 */

import { Problems } from "./descriptor/problem";
import { describeJson, describedProject, registerProject, type RegisteredProject } from "./project";
import { main } from "./runtime/main";

export { registerProject, registeredProject, describedProject, describeJson } from "./project";
export type { RegisteredProject } from "./project";
export { describeProject, descriptorSetDigest, withoutPatterns } from "./descriptor/describe";
export type { DescribedPackage, DescribedTool, Facts, ExampleEntry, ProjectInput } from "./descriptor/describe";
export { Problem, Problems } from "./descriptor/problem";
export * as jcs from "./descriptor/jcs";

/**
 * The bundle's entry: registers the project, then describes it (`--toolgate-describe`, for the packager) or runs it
 * (the standalone or governed runtime).
 */
export async function runProject(input: RegisteredProject): Promise<void> {
  registerProject(input);
  if (process.argv.includes("--toolgate-describe")) {
    try {
      const described = describedProject()!;
      process.stdout.write(JSON.stringify(describeJson(described)) + "\n");
    } catch (e) {
      if (e instanceof Problems) {
        process.stderr.write(e.message + "\n");
        process.exitCode = 1;
        return;
      }
      throw e;
    }
    return;
  }
  await main(process.argv.slice(2));
}
