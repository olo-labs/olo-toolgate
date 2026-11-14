// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/** The runtime entry point of a bundle. */
export async function main(_args: readonly string[]): Promise<void> {
  process.stderr.write("toolgate: the runtime is not built yet\n");
  process.exitCode = 78;
}
