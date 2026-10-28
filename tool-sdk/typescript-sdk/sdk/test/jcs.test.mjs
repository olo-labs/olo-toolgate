// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// The canonical form of descriptor §3; mirrors the Java processor's JcsTest.
import { test } from "node:test";
import assert from "node:assert/strict";
import internal from "../dist/cjs/internal.js";

const { canonical, sha256 } = internal.jcs;

test("numbersUseEcmaScriptFormatting", () => {
  assert.equal(canonical(1.0), "1");
  assert.equal(canonical(0.1), "0.1");
  assert.equal(canonical(-2.5), "-2.5");
  assert.equal(canonical(-0), "0");
  assert.equal(canonical(1e21), "1e+21");
  assert.equal(canonical(123456789012345680000.0), "123456789012345680000");
  assert.equal(canonical(0.000001), "0.000001");
  assert.equal(canonical(1e-7), "1e-7");
  assert.equal(canonical(Number.MIN_VALUE), "5e-324");
  assert.equal(canonical(Number.MAX_VALUE), "1.7976931348623157e+308");
  assert.equal(canonical(333333333.33333329), "333333333.3333333");
  assert.throws(() => canonical(NaN));
  assert.throws(() => canonical(2n ** 53n));
});

test("stringsEscapeOnlyWhatJsonRequires", () => {
  assert.equal(canonical("\u0001\n\"\\/ é"), '"\\u0001\\n\\"\\\\/ é"');
});

test("keysAreNfcAndSortedByUtf16", () => {
  const m = { Ａlpha: 1, "😀note": 2, état: [] };
  assert.equal(canonical(m), '{"état":[],"😀note":2,"Ａlpha":1}');
  assert.throws(() => canonical({ "état": 1, état: 2 }), /equal after NFC/);
  assert.equal(canonical("é"), '"é"');
});

test("digestsAreLowerCaseHex", () => {
  assert.match(sha256("x"), /^sha256:[0-9a-f]{64}$/);
});
