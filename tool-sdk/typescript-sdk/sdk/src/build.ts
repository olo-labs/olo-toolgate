// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/** @olo-labs/toolgate-sdk/build: the packager behind `toolgate-ts dev | check | package`. Needs typescript and esbuild. */

export {
  prepare,
  describeBundle,
  checkCalls,
  describeCommand,
  devCommand,
  checkCommand,
  packageCommand,
  stage,
  pack,
  findCli,
  tracked,
  MANIFEST,
  RUNTIME_ID,
  ARTIFACT,
  ENTRY_POINT,
} from "./build/commands";
export type { Description, Prepared } from "./build/commands";
export { readProject, BuildError } from "./build/project";
export type { Project } from "./build/project";
export { analyse } from "./build/analysis";
export { entrySource, bundle, ESBUILD_VERSION, TYPESCRIPT_VERSION } from "./build/bundle";
