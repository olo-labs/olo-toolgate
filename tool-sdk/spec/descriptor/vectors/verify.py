#!/usr/bin/env python3
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
"""Recompute the descriptor test vectors and check them.

Reference implementation of the canonical form and digests in ../README.md.
Integers only: generation 1 test vectors contain no non-integral numbers.
Usage: python3 verify.py            check every vector
       python3 verify.py --write    rewrite the expected .jcs files and expected.json
"""
import hashlib
import json
import pathlib
import sys
import unicodedata

HERE = pathlib.Path(__file__).resolve().parent


def _utf16_key(s):
    return s.encode("utf-16-be")


def jcs(value):
    """RFC 8785 canonical JSON for strings, integers, booleans, null, arrays and objects."""
    if isinstance(value, bool) or value is None:
        return json.dumps(value)
    if isinstance(value, int):
        if abs(value) > 2**53 - 1:
            raise ValueError("integer outside the IEEE 754 safe range")
        return str(value)
    if isinstance(value, float):
        raise ValueError("non-integral numbers are not used in generation 1 vectors")
    if isinstance(value, str):
        return json.dumps(value, ensure_ascii=False)
    if isinstance(value, list):
        return "[" + ",".join(jcs(v) for v in value) + "]"
    if isinstance(value, dict):
        items = sorted(value.items(), key=lambda kv: _utf16_key(kv[0]))
        return "{" + ",".join(jcs(k) + ":" + jcs(v) for k, v in items) + "}"
    raise TypeError(type(value))


def nfc(value):
    if isinstance(value, str):
        return unicodedata.normalize("NFC", value)
    if isinstance(value, list):
        return [nfc(v) for v in value]
    if isinstance(value, dict):
        return {nfc(k): nfc(v) for k, v in value.items()}
    return value


def digest(value):
    return "sha256:" + hashlib.sha256(jcs(nfc(value)).encode("utf-8")).hexdigest()


def descriptor_set_digest(descriptors):
    entries = sorted(({"toolId": d["toolId"], "toolDescriptorDigest": digest(d)} for d in descriptors),
                     key=lambda e: _utf16_key(e["toolId"]))
    return digest(entries)


# Equivalence rewrites, list version 1 (README section 4).
DEFAULT_VALUED = {"additionalProperties": True, "uniqueItems": False, "minItems": 0, "minLength": 0,
                  "deprecated": False, "readOnly": False, "writeOnly": False}


def rewrite(schema, defs=None, stack=()):
    if isinstance(schema, list):
        return [rewrite(s, defs, stack) for s in schema]
    if not isinstance(schema, dict):
        return schema
    if defs is None:
        defs = schema.get("$defs", {})
    ref = schema.get("$ref")
    if isinstance(ref, str) and ref.startswith("#/$defs/") and len(schema) == 1:
        name = ref[len("#/$defs/"):]
        if name not in stack and name in defs:
            return rewrite(defs[name], defs, stack + (name,))
    out = {}
    for k, v in schema.items():
        if k in DEFAULT_VALUED and v == DEFAULT_VALUED[k]:
            continue
        if k == "type" and isinstance(v, list) and len(v) == 1:
            v = v[0]
        elif k == "required" and isinstance(v, list):
            v = sorted(v, key=_utf16_key)
        else:
            v = rewrite(v, defs, stack)
        out[k] = v
    if "$defs" in out:
        still_used = jcs({k: v for k, v in out.items() if k != "$defs"})
        out["$defs"] = {n: d for n, d in out["$defs"].items() if f'"#/$defs/{n}"' in still_used}
        if not out["$defs"]:
            del out["$defs"]
    return out


def semantic_form(descriptor):
    d = dict(descriptor)
    for key in ("inputSchema", "outputSchema"):
        if key in d:
            d[key] = rewrite(d[key])
    return d


def equivalent(a, b):
    return jcs(nfc(semantic_form(a))) == jcs(nfc(semantic_form(b)))


def main():
    write = "--write" in sys.argv
    expected_path = HERE / "expected.json"
    expected = json.loads(expected_path.read_text(encoding="utf-8")) if expected_path.exists() else {}
    actual = {"digests": {}, "sets": {}, "equivalence": {}}
    failures = 0
    for path in sorted(HERE.glob("tool-*.json")):
        descriptor = json.loads(path.read_text(encoding="utf-8"))
        canonical = jcs(nfc(descriptor))
        canonical_path = path.with_suffix(".jcs")
        if write:
            canonical_path.write_bytes(canonical.encode("utf-8"))
        elif canonical_path.read_bytes() != canonical.encode("utf-8"):
            print(f"FAIL canonical bytes: {path.name}")
            failures += 1
        actual["digests"][path.name] = digest(descriptor)
    for path in sorted(HERE.glob("set-*.json")):
        names = json.loads(path.read_text(encoding="utf-8"))["tools"]
        descriptors = [json.loads((HERE / n).read_text(encoding="utf-8")) for n in names]
        actual["sets"][path.name] = descriptor_set_digest(descriptors)
    for path in sorted(HERE.glob("equiv-*.json")):
        case = json.loads(path.read_text(encoding="utf-8"))
        actual["equivalence"][path.name] = equivalent(case["a"], case["b"])
    if write:
        expected_path.write_text(json.dumps(actual, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print("wrote expected.json")
        return 0
    for section, values in actual.items():
        for name, value in values.items():
            want = expected.get(section, {}).get(name)
            if want != value:
                print(f"FAIL {section} {name}: expected {want}, got {value}")
                failures += 1
    for section, values in expected.items():
        for name in values:
            if name not in actual.get(section, {}):
                print(f"FAIL {section} {name}: vector file missing")
                failures += 1
    print("descriptor vectors:", "FAILED" if failures else "ok")
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
