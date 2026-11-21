// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * Canonical form of descriptor §3: every string NFC, then RFC 8785 (keys by UTF-16 code units, ECMAScript numbers
 * and strings), UTF-8. A port of the Java processor's Jcs.
 */

import { createHash } from "node:crypto";

export type Json = null | boolean | number | string | Json[] | { [key: string]: Json };

const SAFE = 9007199254740991;

export function nfc(s: string): string {
  return s.normalize("NFC");
}

/** The canonical text of a JSON value. */
export function canonical(value: unknown): string {
  const out: string[] = [];
  write(out, value);
  return out.join("");
}

/** The canonical UTF-8 bytes of a JSON value. */
export function bytes(value: unknown): Buffer {
  return Buffer.from(canonical(value), "utf8");
}

/** `sha256:` plus the lower-case hex digest. */
export function sha256(data: Uint8Array | string): string {
  return "sha256:" + createHash("sha256").update(data).digest("hex");
}

/** The digest of a value's canonical bytes. */
export function digest(value: unknown): string {
  return sha256(bytes(value));
}

/** Sorts strings by UTF-16 code units after NFC, the RFC 8785 order. */
export function sorted(values: Iterable<string>): string[] {
  return [...values].map(nfc).sort(compare);
}

export function compare(a: string, b: string): number {
  return a < b ? -1 : a > b ? 1 : 0;
}

function write(out: string[], value: unknown): void {
  if (value === null) {
    out.push("null");
  } else if (Array.isArray(value)) {
    out.push("[");
    value.forEach((v, i) => {
      if (i > 0) out.push(",");
      write(out, v === undefined ? null : v);
    });
    out.push("]");
  } else if (typeof value === "string") {
    out.push(string(nfc(value)));
  } else if (typeof value === "boolean") {
    out.push(value ? "true" : "false");
  } else if (typeof value === "number") {
    if (!Number.isFinite(value)) throw new RangeError("not a finite number");
    out.push(Object.is(value, -0) ? "0" : String(value));
  } else if (typeof value === "bigint") {
    if (value > BigInt(SAFE) || value < -BigInt(SAFE)) throw new RangeError("integer beyond 2^53-1: " + value);
    out.push(value.toString());
  } else if (typeof value === "object") {
    const entries = new Map<string, unknown>();
    for (const [k, v] of Object.entries(value as Record<string, unknown>)) {
      if (v === undefined) continue;
      const key = nfc(k);
      if (entries.has(key)) throw new RangeError("keys are equal after NFC: " + key);
      entries.set(key, v);
    }
    out.push("{");
    let first = true;
    for (const key of [...entries.keys()].sort(compare)) {
      if (!first) out.push(",");
      first = false;
      out.push(string(key), ":");
      write(out, entries.get(key));
    }
    out.push("}");
  } else {
    throw new TypeError("not a JSON value: " + String(value));
  }
}

function string(s: string): string {
  let out = '"';
  for (let i = 0; i < s.length; i++) {
    const c = s.charCodeAt(i);
    switch (c) {
      case 0x22:
        out += '\\"';
        break;
      case 0x5c:
        out += "\\\\";
        break;
      case 0x08:
        out += "\\b";
        break;
      case 0x0c:
        out += "\\f";
        break;
      case 0x0a:
        out += "\\n";
        break;
      case 0x0d:
        out += "\\r";
        break;
      case 0x09:
        out += "\\t";
        break;
      default:
        out += c < 0x20 ? "\\u" + c.toString(16).padStart(4, "0") : s[i];
    }
  }
  return out + '"';
}
