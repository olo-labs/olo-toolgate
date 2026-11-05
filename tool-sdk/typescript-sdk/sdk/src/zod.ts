// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

/**
 * The pinned Zod 3.x the SDK re-exports as `z`, plus the two SDK helpers of descriptor §5: `binary()` for BINARY
 * values and `sensitive()` for redacted ones. The descriptor converter reads Zod's `_def` by type name, so a schema
 * built with another copy of Zod 3 converts the same way.
 */

import { z, ZodType, type ParseInput, type ParseReturnType, type ZodTypeDef, INVALID, OK, addIssueToContext, ZodIssueCode } from "zod";
import type { ValueType } from "./context";

export { z };

export const BINARY_TYPE_NAME = "ToolgateBinary";

export interface ZodBinaryDef extends ZodTypeDef {
  readonly typeName: typeof BINARY_TYPE_NAME;
}

/** A BINARY value: base64 on the wire, a Uint8Array in the handler. */
export class ZodBinary extends ZodType<Uint8Array, ZodBinaryDef, Uint8Array> {
  _parse(input: ParseInput): ParseReturnType<Uint8Array> {
    if (!(input.data instanceof Uint8Array)) {
      const ctx = this._getOrReturnCtx(input);
      addIssueToContext(ctx, { code: ZodIssueCode.custom, message: "Expected a Uint8Array" });
      return INVALID;
    }
    return OK(input.data);
  }
}

/** A BINARY input or output property (descriptor §5). */
export function binary(): ZodBinary {
  return new ZodBinary({ typeName: BINARY_TYPE_NAME });
}

/** Marks a property as sensitive: `x-toolgate-sensitive: true`, redacted from logs, errors and audit. */
export function sensitive<T extends z.ZodTypeAny>(schema: T): T {
  const Ctor = schema.constructor as new (def: unknown) => T;
  return new Ctor({ ...schema._def, toolgateSensitive: true });
}

/** A Zod object schema, from this or another copy of Zod 3. */
export type ZodObjectLike = z.ZodObject<z.ZodRawShape, z.UnknownKeysParam, z.ZodTypeAny>;

type Unwrap<T> = T extends z.ZodOptional<infer U>
  ? Unwrap<U>
  : T extends z.ZodDefault<infer U>
    ? Unwrap<U>
    : T;

/** The value types a Zod property can hold, as the typed getters read them. */
export type ValueTypeOf<T> =
  Unwrap<T> extends z.ZodString
    ? "STRING" | "URI" | "DATE" | "DATE_TIME" | "DURATION"
    : Unwrap<T> extends z.ZodNumber
      ? "INT" | "LONG" | "NUMBER"
      : Unwrap<T> extends z.ZodBoolean
        ? "BOOLEAN"
        : Unwrap<T> extends z.ZodEnum<[string, ...string[]]>
          ? "ENUM"
          : Unwrap<T> extends z.ZodArray<z.ZodTypeAny>
            ? "ARRAY"
            : Unwrap<T> extends ZodBinary
              ? "BINARY"
              : "OBJECT";

/** Input name to value type, from a Zod object. */
export type InputMap<I> = I extends z.ZodObject<infer S> ? { [K in keyof S & string]: ValueTypeOf<S[K]> } : {};

/** Kept for symmetry with the runtime type names. */
export type { ValueType };
