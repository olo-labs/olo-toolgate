// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * Schema generation rules of descriptor §5, generation 1: the SDK's own Zod converter (only the constructs §5 lists;
 * anything else fails packaging, naming the construct) and the Spec-driven schemas of grouped inputs and globals, a
 * port of the Java processor's Schemas. Zod is read through `_def` by type name, so any copy of Zod 3 converts alike.
 */

import type { ValueType } from "../context";
import { BINARY_TYPE_NAME } from "../zod";
import { sorted } from "./jcs";
import { Problem } from "./problem";

export const INT_MIN = -2147483648;
export const INT_MAX = 2147483647;
export const SAFE = 9007199254740991;

export type Schema = Record<string, unknown>;

/** The attributes of a Param, Variable or Global, as Java's Spec holds them. Empty strings and -1 mean unset. */
export interface Spec {
  readonly name: string;
  readonly description: string;
  readonly type: ValueType;
  readonly required: boolean;
  readonly defaultValue: string;
  readonly min: string;
  readonly max: string;
  readonly minLength: number;
  readonly maxLength: number;
  readonly pattern: string;
  readonly enumValues: readonly string[];
  readonly format: string;
  readonly sensitive: boolean;
}

/** A Spec from a declaration object's members. */
export function specOf(d: Record<string, unknown>, at: string | undefined, what: string): Spec {
  const str = (k: string): string => {
    const v = d[k];
    if (v === undefined) return "";
    if (typeof v !== "string") throw new Problem(at, what + ": " + k + " must be a string");
    return v;
  };
  const bound = (k: string): string => {
    const v = d[k];
    if (v === undefined) return "";
    if (typeof v === "number") return String(v);
    if (typeof v !== "string") throw new Problem(at, what + ": " + k + " must be a number or a string");
    return v;
  };
  const len = (k: string): number => {
    const v = d[k];
    if (v === undefined) return -1;
    if (typeof v !== "number" || !Number.isInteger(v)) throw new Problem(at, what + ": " + k + " must be an integer");
    if (v < 0) throw new Problem(at, what + ": lengths must be at least 0");
    return v;
  };
  const enumValues = d["enumValues"];
  if (enumValues !== undefined && (!Array.isArray(enumValues) || enumValues.some((e) => typeof e !== "string"))) {
    throw new Problem(at, what + ": enumValues must be a list of strings");
  }
  return {
    name: str("name"),
    description: str("description"),
    type: (d["type"] as ValueType | undefined) ?? "STRING",
    required: d["required"] === undefined ? true : d["required"] === true,
    defaultValue: str("defaultValue"),
    min: bound("min"),
    max: bound("max"),
    minLength: len("minLength"),
    maxLength: len("maxLength"),
    pattern: str("pattern"),
    enumValues: (enumValues as string[] | undefined) ?? [],
    format: str("format"),
    sensitive: d["sensitive"] === true,
  };
}

/** The closed object over the given properties. */
export function closed(properties: Schema, required: readonly string[]): Schema {
  return { type: "object", properties, required: sorted(required), additionalProperties: false };
}

/**
 * Adds the declared constraint keywords; with materialise, INT and LONG get their range bounds unless narrower ones
 * are declared.
 */
export function constraints(schema: Schema, vt: ValueType, spec: Spec, at: string | undefined, materialise: boolean): void {
  const name = spec.name;
  const numeric = vt === "INT" || vt === "LONG" || vt === "NUMBER";
  const text = vt === "STRING" || vt === "URI";
  if (!numeric && (spec.min !== "" || spec.max !== "")) {
    throw new Problem(at, name + ": min and max apply to INT, LONG and NUMBER only");
  }
  if (!text && (spec.minLength >= 0 || spec.maxLength >= 0 || spec.pattern !== "")) {
    throw new Problem(at, name + ": minLength, maxLength and pattern apply to STRING and URI only");
  }
  if (spec.format !== "" && vt !== "STRING") {
    throw new Problem(at, name + ": format applies to STRING only; " + vt + " has its own format");
  }
  if (spec.enumValues.length > 0 && vt !== "ENUM") {
    throw new Problem(at, name + ": enumValues applies to a String typed as ENUM");
  }
  if (vt === "INT" || vt === "LONG") {
    const lo = vt === "INT" ? INT_MIN : -SAFE;
    const hi = vt === "INT" ? INT_MAX : SAFE;
    const min = spec.min === "" ? undefined : integer(spec.min, lo, hi, name, at);
    const max = spec.max === "" ? undefined : integer(spec.max, lo, hi, name, at);
    if (min !== undefined && max !== undefined && min > max) throw new Problem(at, name + ": min is greater than max");
    if (min !== undefined || materialise) schema["minimum"] = min ?? lo;
    if (max !== undefined || materialise) schema["maximum"] = max ?? hi;
  } else if (vt === "NUMBER") {
    const min = spec.min === "" ? undefined : number(spec.min, name, at);
    const max = spec.max === "" ? undefined : number(spec.max, name, at);
    if (min !== undefined && max !== undefined && min > max) throw new Problem(at, name + ": min is greater than max");
    if (min !== undefined) schema["minimum"] = min;
    if (max !== undefined) schema["maximum"] = max;
  }
  if (spec.minLength < -1 || spec.maxLength < -1) throw new Problem(at, name + ": lengths must be at least 0");
  if (spec.minLength >= 0) schema["minLength"] = spec.minLength;
  if (spec.maxLength >= 0) schema["maxLength"] = spec.maxLength;
  if (spec.minLength >= 0 && spec.maxLength >= 0 && spec.minLength > spec.maxLength) {
    throw new Problem(at, name + ": minLength is greater than maxLength");
  }
  if (spec.pattern !== "") {
    const error = compileError(spec.pattern);
    if (error !== undefined) throw new Problem(at, name + ": pattern does not compile: " + error);
    schema["pattern"] = spec.pattern;
  }
  if (spec.format !== "") schema["format"] = spec.format;
  if (spec.defaultValue !== "") schema["default"] = typedDefault(vt, spec, schema, name, at);
  if (spec.sensitive) schema["x-toolgate-sensitive"] = true;
}

function typedDefault(vt: ValueType, spec: Spec, schema: Schema, name: string, at: string | undefined): unknown {
  const raw = spec.defaultValue;
  switch (vt) {
    case "INT":
    case "LONG": {
      const lo = typeof schema["minimum"] === "number" ? (schema["minimum"] as number) : vt === "INT" ? INT_MIN : -SAFE;
      const hi = typeof schema["maximum"] === "number" ? (schema["maximum"] as number) : vt === "INT" ? INT_MAX : SAFE;
      return integer(raw, lo, hi, name + " default", at);
    }
    case "NUMBER":
      return number(raw, name + " default", at);
    case "BOOLEAN":
      if (raw !== "true" && raw !== "false") throw new Problem(at, name + ": default must be true or false");
      return raw === "true";
    case "ENUM": {
      const values = schema["enum"] as string[] | undefined;
      if (values === undefined || !values.includes(raw)) throw new Problem(at, name + ": default " + raw + " is not an enum value");
      return raw;
    }
    case "STRING":
    case "DATE":
    case "DATE_TIME":
    case "DURATION":
    case "URI":
      return raw;
    default:
      throw new Problem(at, name + ": " + vt + " values cannot have a default");
  }
}

/** Java's Long.parseLong over the trimmed text, range-checked. */
function integer(raw: string, lo: number, hi: number, name: string, at: string | undefined): number {
  const t = raw.trim();
  if (!/^[+-]?[0-9]+$/.test(t)) throw new Problem(at, name + ": " + raw + " is not an integer");
  const big = BigInt(t);
  if (big < BigInt(lo) || big > BigInt(hi)) throw new Problem(at, name + ": " + raw + " is outside " + lo + ".." + hi);
  return Number(big);
}

/** Java's Double.parseDouble over the trimmed text, finite only. */
function number(raw: string, name: string, at: string | undefined): number {
  const t = raw.trim();
  if (!/^[+-]?(NaN|Infinity|((\d+\.?\d*|\.\d+)([eE][+-]?\d+)?)[fFdD]?)$/.test(t)) {
    throw new Problem(at, name + ": " + raw + " is not a number");
  }
  const value = Number(t.replace(/[fFdD]$/, ""));
  if (!Number.isFinite(value)) throw new Problem(at, name + ": " + raw + " is not finite");
  return value;
}

/** The compile error of an ECMA-262 pattern, or undefined. */
export function compileError(pattern: string): string | undefined {
  try {
    new RegExp(pattern);
    return undefined;
  } catch (e) {
    return (e as Error).message;
  }
}

/** The schema of a grouped input or a global from its Spec (Java Schemas.schema over a scalar type). */
export function specSchema(vt: ValueType, spec: Spec, at: string | undefined, materialise: boolean): Schema {
  const schema: Schema = {};
  switch (vt) {
    case "STRING":
      schema["type"] = "string";
      break;
    case "INT":
    case "LONG":
      schema["type"] = "integer";
      break;
    case "NUMBER":
      schema["type"] = "number";
      break;
    case "BOOLEAN":
      schema["type"] = "boolean";
      break;
    case "DATE":
      Object.assign(schema, { type: "string", format: "date" });
      break;
    case "DATE_TIME":
      Object.assign(schema, { type: "string", format: "date-time" });
      break;
    case "DURATION":
      Object.assign(schema, { type: "string", format: "duration" });
      break;
    case "URI":
      Object.assign(schema, { type: "string", format: "uri" });
      break;
    case "BINARY":
      Object.assign(schema, { type: "string", contentEncoding: "base64" });
      break;
    case "ENUM":
      if (spec.enumValues.length === 0) throw new Problem(at, "a String typed as ENUM needs enumValues");
      Object.assign(schema, { type: "string", enum: [...spec.enumValues] });
      break;
    default:
      throw new Problem(at, "unsupported type " + vt);
  }
  if (spec.description !== "") schema["description"] = spec.description;
  constraints(schema, vt, spec, at, materialise);
  return schema;
}

/** The value type of a schema, as the runtime's DeclaredValues names it. */
export function valueType(schema: Schema): ValueType {
  const type = schema["type"];
  if (type === "integer") {
    const max = typeof schema["maximum"] === "number" ? (schema["maximum"] as number) : Infinity;
    const min = typeof schema["minimum"] === "number" ? (schema["minimum"] as number) : -Infinity;
    return max <= INT_MAX && min >= INT_MIN ? "INT" : "LONG";
  }
  if (type === "number") return "NUMBER";
  if (type === "boolean") return "BOOLEAN";
  if (type === "array") return "ARRAY";
  if (type !== "string") return "OBJECT";
  if ("enum" in schema) return "ENUM";
  if (schema["contentEncoding"] === "base64") return "BINARY";
  switch (schema["format"]) {
    case "date":
      return "DATE";
    case "date-time":
      return "DATE_TIME";
    case "duration":
      return "DURATION";
    case "uri":
      return "URI";
    default:
      return "STRING";
  }
}

// ---------------------------------------------------------------------------------------------------------------
// Zod

interface Def {
  readonly typeName?: string;
  readonly description?: string;
  readonly toolgateSensitive?: boolean;
  readonly [key: string]: unknown;
}

function defOf(schema: unknown): Def | undefined {
  if (schema === null || typeof schema !== "object") return undefined;
  const def = (schema as { _def?: unknown })._def;
  return def !== null && typeof def === "object" ? (def as Def) : undefined;
}

/** Whether a value is a Zod 3 object schema. */
export function isZodObject(schema: unknown): boolean {
  return defOf(schema)?.typeName === "ZodObject";
}

/** Whether a value looks like a Zod schema of any major. */
export function isZodLike(schema: unknown): boolean {
  return defOf(schema) !== undefined || (schema !== null && typeof schema === "object" && "_zod" in (schema as object));
}

const NOT_ALLOWED: Record<string, string> = {
  ZodUnion: "unions",
  ZodDiscriminatedUnion: "unions",
  ZodIntersection: "intersections",
  ZodNullable: "nullable",
  ZodEffects: "transform, refine and preprocess",
  ZodLazy: "lazy",
  ZodAny: "any",
  ZodUnknown: "unknown",
  ZodBigInt: "bigint",
  ZodDate: "z.date()",
};

/** A converted property: its schema, whether it may be absent, and its value type. */
export interface Converted {
  readonly schema: Schema;
  readonly optional: boolean;
  readonly description: string;
}

/** Converts one Zod property (descriptor §5). `what` names it in errors. */
export function zodProperty(zod: unknown, what: string, at: string | undefined): Converted {
  let optional = false;
  let description: string | undefined;
  let sensitive = false;
  let defaultValue: { value: unknown } | undefined;
  let current = zod;
  for (;;) {
    const def = defOf(current);
    if (def === undefined) {
      if (current !== null && typeof current === "object" && "_zod" in (current as object)) {
        throw new Problem(at, what + ": this schema is from Zod 4; the SDK converts Zod 3 only (import z from the SDK)");
      }
      throw new Problem(at, what + " is not a Zod schema");
    }
    if (description === undefined && typeof def.description === "string") description = def.description;
    if (def.toolgateSensitive === true) sensitive = true;
    if (def.typeName === "ZodOptional") {
      optional = true;
      current = def["innerType"];
    } else if (def.typeName === "ZodDefault") {
      optional = true;
      if (defaultValue === undefined) defaultValue = { value: (def["defaultValue"] as () => unknown)() };
      current = def["innerType"];
    } else {
      break;
    }
  }
  const schema = zodValue(current, what, at);
  if (description !== undefined) schema["description"] = description;
  if (defaultValue !== undefined) schema["default"] = zodDefault(defaultValue.value, schema, what, at);
  if (sensitive) schema["x-toolgate-sensitive"] = true;
  return { schema, optional, description: description ?? "" };
}

/** The output schema of a tool: a Zod object or record. */
export function zodOutput(zod: unknown, at: string | undefined): Schema {
  const def = defOf(zod);
  if (def?.typeName !== "ZodObject" && def?.typeName !== "ZodRecord") {
    throw new Problem(at, "output must be a Zod object or record schema");
  }
  const schema = zodValue(zod, "output", at);
  if (def.toolgateSensitive === true) schema["x-toolgate-sensitive"] = true;
  if (typeof def.description === "string") schema["description"] = def.description;
  return schema;
}

function zodValue(zod: unknown, what: string, at: string | undefined): Schema {
  const def = defOf(zod)!;
  const typeName = def.typeName ?? "";
  if (def["coerce"] === true) throw new Problem(at, what + ": z.coerce is not allowed in generation 1 (descriptor §5)");
  switch (typeName) {
    case "ZodString":
      return zodString(def, what, at);
    case "ZodNumber":
      return zodNumber(def, what, at);
    case "ZodBoolean":
      return { type: "boolean" };
    case "ZodEnum": {
      const values = def["values"] as unknown[];
      if (!values.every((v) => typeof v === "string")) throw new Problem(at, what + ": enum values must be strings");
      return { type: "string", enum: [...(values as string[])] };
    }
    case BINARY_TYPE_NAME:
      return { type: "string", contentEncoding: "base64" };
    case "ZodArray": {
      const schema: Schema = { type: "array", items: zodNested(def["type"], what + "[]", at) };
      const exact = def["exactLength"] as { value: number } | null;
      const min = def["minLength"] as { value: number } | null;
      const max = def["maxLength"] as { value: number } | null;
      if (exact && (min || max)) throw new Problem(at, what + ": .length() cannot be combined with .min() or .max()");
      if (exact) Object.assign(schema, { minItems: exact.value, maxItems: exact.value });
      if (min) schema["minItems"] = min.value;
      if (max) schema["maxItems"] = max.value;
      if (min && max && min.value > max.value) throw new Problem(at, what + ": min is greater than max");
      return schema;
    }
    case "ZodObject": {
      const unknownKeys = def["unknownKeys"];
      if (unknownKeys === "passthrough") throw new Problem(at, what + ": .passthrough() is not allowed in generation 1 (descriptor §5)");
      if (defOf(def["catchall"])?.typeName !== "ZodNever") {
        throw new Problem(at, what + ": .catchall() is not allowed in generation 1 (descriptor §5)");
      }
      const shape = (def["shape"] as () => Record<string, unknown>)();
      const properties: Schema = {};
      const required: string[] = [];
      for (const [name, value] of Object.entries(shape)) {
        const p = zodProperty(value, what === "output" ? name : what + "." + name, at);
        properties[name] = p.schema;
        if (!p.optional) required.push(name);
      }
      return closed(properties, required);
    }
    case "ZodRecord": {
      const key = defOf(def["keyType"]);
      if (key?.typeName !== "ZodString" || (key["checks"] as unknown[]).length > 0) {
        throw new Problem(at, what + ": record keys must be z.string() without checks");
      }
      const value = defOf(def["valueType"]);
      // z.record(z.string(), z.unknown()) holds any JSON value: the empty schema, as Java's Map<String, Object>.
      const values = value?.typeName === "ZodUnknown" ? {} : zodNested(def["valueType"], what + "{}", at);
      return { type: "object", additionalProperties: values };
    }
    default: {
      const named = NOT_ALLOWED[typeName];
      throw new Problem(
        at,
        what + ": " + (named ?? (typeName || "this schema")) + " is not allowed in generation 1 (descriptor §5)",
      );
    }
  }
}

/** Array items and map values: no optional or default, which JSON Schema cannot say there. */
function zodNested(zod: unknown, what: string, at: string | undefined): Schema {
  const def = defOf(zod);
  if (def?.typeName === "ZodOptional" || def?.typeName === "ZodDefault") {
    throw new Problem(at, what + ": .optional() and .default() apply to object properties only");
  }
  return zodProperty(zod, what, at).schema;
}

const FORMATS: Record<string, string> = {
  email: "email",
  url: "uri",
  uuid: "uuid",
  date: "date",
  datetime: "date-time",
  duration: "duration",
};

function zodString(def: Def, what: string, at: string | undefined): Schema {
  const schema: Schema = { type: "string" };
  let format: string | undefined;
  const seen = new Set<string>();
  for (const check of def["checks"] as Array<Record<string, unknown>>) {
    const kind = String(check["kind"]);
    if (seen.has(kind)) throw new Problem(at, what + ": ." + kind + "() is given twice");
    seen.add(kind);
    switch (kind) {
      case "min":
        schema["minLength"] = check["value"];
        break;
      case "max":
        schema["maxLength"] = check["value"];
        break;
      case "length":
        schema["minLength"] = check["value"];
        schema["maxLength"] = check["value"];
        break;
      case "regex": {
        const regex = check["regex"] as RegExp;
        if (regex.flags.replace(/[gy]/g, "") !== "") {
          throw new Problem(at, what + ": a pattern cannot carry flags (" + regex.flags + ") in JSON Schema");
        }
        schema["pattern"] = regex.source;
        break;
      }
      case "datetime":
        if (check["offset"] !== true || check["local"] === true || check["precision"] != null) {
          throw new Problem(at, what + ": only .datetime({ offset: true }) is allowed (RFC 3339 with an offset)");
        }
        if (format !== undefined) throw new Problem(at, what + ": a string has at most one format");
        format = FORMATS[kind];
        break;
      case "email":
      case "url":
      case "uuid":
      case "date":
      case "duration":
        if (format !== undefined) throw new Problem(at, what + ": a string has at most one format");
        format = FORMATS[kind];
        break;
      default:
        throw new Problem(at, what + ": ." + kind + "() is not allowed in generation 1 (descriptor §5)");
    }
  }
  if (seen.has("length") && (seen.has("min") || seen.has("max"))) {
    throw new Problem(at, what + ": .length() cannot be combined with .min() or .max()");
  }
  if (typeof schema["minLength"] === "number" && typeof schema["maxLength"] === "number" && schema["minLength"] > schema["maxLength"]) {
    throw new Problem(at, what + ": minLength is greater than maxLength");
  }
  if (format !== undefined) {
    if (format !== "uri" && format !== "email" && format !== "uuid" && "pattern" in schema) {
      throw new Problem(at, what + ": minLength, maxLength and pattern apply to STRING and URI only");
    }
    if ((format === "date" || format === "date-time" || format === "duration") && ("minLength" in schema || "maxLength" in schema)) {
      throw new Problem(at, what + ": minLength, maxLength and pattern apply to STRING and URI only");
    }
    schema["format"] = format;
  }
  return schema;
}

function zodNumber(def: Def, what: string, at: string | undefined): Schema {
  let int = false;
  const mins: number[] = [];
  const maxs: number[] = [];
  for (const check of def["checks"] as Array<Record<string, unknown>>) {
    const kind = String(check["kind"]);
    switch (kind) {
      case "int":
        int = true;
        break;
      case "min":
      case "max":
        if (check["inclusive"] !== true) {
          throw new Problem(at, what + ": exclusive bounds (.gt, .lt, .positive, .negative) are not allowed in generation 1; use .min and .max");
        }
        (kind === "min" ? mins : maxs).push(check["value"] as number);
        break;
      default:
        throw new Problem(at, what + ": ." + kind + "() is not allowed in generation 1 (descriptor §5)");
    }
  }
  const min = mins.length ? Math.max(...mins) : undefined;
  const max = maxs.length ? Math.min(...maxs) : undefined;
  if (min !== undefined && max !== undefined && min > max) throw new Problem(at, what + ": min is greater than max");
  if (!int) {
    const schema: Schema = { type: "number" };
    if (min !== undefined) schema["minimum"] = min;
    if (max !== undefined) schema["maximum"] = max;
    return schema;
  }
  for (const v of [...mins, ...maxs]) {
    if (!Number.isInteger(v)) throw new Problem(at, what + ": " + v + " is not an integer");
    if (Math.abs(v) > SAFE) throw new Problem(at, what + ": " + v + " is outside " + -SAFE + ".." + SAFE);
  }
  // .safe() (or any bound beyond 32 bits) makes a LONG; otherwise INT, with its range materialised.
  const long = [...mins, ...maxs].some((v) => v < INT_MIN || v > INT_MAX);
  const lo = long ? -SAFE : INT_MIN;
  const hi = long ? SAFE : INT_MAX;
  return { type: "integer", minimum: min ?? lo, maximum: max ?? hi };
}

function zodDefault(value: unknown, schema: Schema, what: string, at: string | undefined): unknown {
  const vt = valueType(schema);
  switch (vt) {
    case "INT":
    case "LONG": {
      if (typeof value !== "number" || !Number.isInteger(value)) throw new Problem(at, what + ": default " + String(value) + " is not an integer");
      const lo = schema["minimum"] as number;
      const hi = schema["maximum"] as number;
      if (value < lo || value > hi) throw new Problem(at, what + " default: " + value + " is outside " + lo + ".." + hi);
      return value;
    }
    case "NUMBER":
      if (typeof value !== "number" || !Number.isFinite(value)) throw new Problem(at, what + ": default " + String(value) + " is not finite");
      return value;
    case "BOOLEAN":
      if (typeof value !== "boolean") throw new Problem(at, what + ": default must be true or false");
      return value;
    case "ENUM":
      if (!(schema["enum"] as unknown[]).includes(value)) throw new Problem(at, what + ": default " + String(value) + " is not an enum value");
      return value;
    case "STRING":
    case "DATE":
    case "DATE_TIME":
    case "DURATION":
    case "URI":
      if (typeof value !== "string") throw new Problem(at, what + ": default must be a string");
      return value;
    default:
      throw new Problem(at, what + ": " + vt + " values cannot have a default");
  }
}
