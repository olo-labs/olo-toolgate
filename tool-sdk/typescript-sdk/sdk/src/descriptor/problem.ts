// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/** A declaration problem: the packaging error of one declaration, with where it was declared. */
export class Problem extends Error {
  /** `<module>#<export>` of the declaration, when known. */
  readonly at: string | undefined;

  constructor(at: string | undefined, message: string) {
    super(message);
    this.at = at;
    this.name = "Problem";
  }

  /** The message with its location, as the packager prints it. */
  describe(): string {
    return "toolgate: " + (this.at ? this.at + ": " : "") + this.message;
  }
}

/** A set of problems found in one pass; thrown when the pass ends. */
export class Problems extends Error {
  readonly problems: readonly Problem[];

  constructor(problems: readonly Problem[]) {
    super(problems.map((p) => p.describe()).join("\n"));
    this.problems = problems;
    this.name = "Problems";
  }
}
