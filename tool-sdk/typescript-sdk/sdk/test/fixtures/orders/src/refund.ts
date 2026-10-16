// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// Descriptor vector tool-03 (non-ASCII names and UTF-16 key order); mirrors acme/orders/RefundTools.java.
import { errorDecl, idempotency, reconcile, secret, serverTool, z } from "@olo-labs/toolgate-sdk";

const Refund = z.object({
  zeta: z.boolean(),
  état: z.string(),
  "€amount": z.number().int().safe().min(0),
  "😀note": z.string(),
  Ａlpha: z.string(),
});

export const refund = serverTool({
  name: "orders.refund",
  title: "Refund an order",
  description: "Refunds an order in full. Café orders are refunded to the original card; the amount is in €.",
  effect: "NON_IDEMPOTENT",
  destructive: true,
  async: true,
  stateless: true,
  maxConcurrency: 4,
  routes: ["orders.refund"],
  businessKeyNamespace: "TENANT",
  businessKeyArgument: "refundKey",
  errors: [
    errorDecl({ code: "PSP_BUSY", description: "The payment provider asked us to retry", retryable: true }),
    errorDecl({ code: "ALREADY_REFUNDED", description: "The order was already refunded" }),
  ],
  toolsets: ["payments"],
  idempotency: idempotency({ header: "Idempotency-Key" }),
  secrets: [secret({ name: "AcmeClient", type: "OBJECT" })],
  input: z.object({
    orderId: z.string().regex(/^[0-9]{6,12}$/).describe("Order number"),
    reason: z.enum(["DAMAGED", "LATE", "ÉCHANGE"]).describe("Reason shown to the customer"),
    refundKey: z.string().min(8).max(64).describe("Business key for this refund"),
  }),
  output: Refund,
  run: async (args, ctx) =>
    ctx.http.route("orders.refund").pathVar("orderId", args.orderId).body({ reason: args.reason }).send<z.infer<typeof Refund>>(),
});

export const refundStatus = serverTool({
  name: "orders.refund-status",
  title: "Check a refund",
  description: "Reconciles an uncertain refund.",
  effect: "READ_ONLY",
  reconcile: reconcile({ tool: "orders.refund" }),
  input: z.object({ orderId: z.string().describe("Order number") }),
  run: async () => ({}) as Record<string, unknown>,
});
