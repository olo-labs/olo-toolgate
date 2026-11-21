#!/usr/bin/env node
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// toolgate-ts dev | check | package | describe [<dir>]: the build commands a TypeScript tool project's npm scripts run.
// Exit codes: 0 success, 1 a check or build failed, 2 usage.
"use strict";

const USAGE = "usage: toolgate-ts dev | check | package [--no-pack] | describe [<project dir>]";

async function run(argv) {
  const [command, ...rest] = argv;
  const flags = rest.filter((a) => a.startsWith("--"));
  const dirs = rest.filter((a) => !a.startsWith("--"));
  if (dirs.length > 1) return usage();
  const dir = dirs[0] ?? process.cwd();
  const build = require("../dist/cjs/build.js");
  try {
    switch (command) {
      case "dev":
        if (flags.length) return usage();
        return await build.devCommand(dir);
      case "check":
        if (flags.length) return usage();
        return await build.checkCommand(dir);
      case "package":
        if (flags.some((f) => f !== "--no-pack")) return usage();
        return await build.packageCommand(dir, { pack: !flags.includes("--no-pack") });
      case "describe":
        if (flags.length) return usage();
        return await build.describeCommand(dir);
      default:
        return usage();
    }
  } catch (e) {
    if (e instanceof build.BuildError) {
      process.stderr.write(e.message + "\n");
      return 1;
    }
    throw e;
  }
}

function usage() {
  process.stderr.write(USAGE + "\n");
  return 2;
}

run(process.argv.slice(2)).then(
  (code) => {
    process.exitCode = code;
  },
  (e) => {
    process.stderr.write("toolgate: " + (e && e.stack ? e.stack : String(e)) + "\n");
    process.exitCode = 1;
  },
);
