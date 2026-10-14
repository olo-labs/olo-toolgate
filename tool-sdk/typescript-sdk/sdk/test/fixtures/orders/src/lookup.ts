// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// Descriptor vector tool-02; mirrors the Java processor fixture acme/orders/LookupTools.java.
import { errorDecl, global, jwtClaim, secret, serverTool, z } from "@olo-labs/toolgate-sdk";

const Order = z.object({
  orderId: z.string(),
  status: z.enum(["OPEN", "SHIPPED", "CANCELLED"]),
  items: z.array(z.object({ sku: z.string(), quantity: z.number().int() })),
});

export const lookup = serverTool({
  name: "orders.lookup",
  title: "Look up an order",
  description: "Returns status and items for one order.",
  useWhen: "The user asks about a specific order number.",
  effect: "READ_ONLY",
  routes: ["orders.get"],
  errors: [errorDecl({ code: "ORDER_NOT_FOUND", description: "No such order" })],
  globals: [global({ name: "TimeoutSeconds", type: "INT", defaultValue: "10" })],
  secrets: [secret({ name: "ApiKey" })],
  jwt: [jwtClaim({ name: "UserName", claim: "preferred_username" })],
  input: z.object({
    orderId: z.string().regex(/^[0-9]{6,12}$/).describe("Order number"),
    since: z.number().int().optional().describe("Only events after this epoch second"),
  }),
  output: Order,
  run: async (args, ctx) =>
    ctx.http.route("orders.get").pathVar("orderId", args.orderId).query("since", args.since).send<z.infer<typeof Order>>(),
});
