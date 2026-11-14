// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// Descriptor vectors tool-02 and tool-03; mirrors the Java processor fixture acme/orders/OrdersPackage.java.
import { destination, httpAuth, httpRoute, toolPackage } from "@olo-labs/toolgate-sdk";

export default toolPackage({ id: "acme.orders", name: "Acme orders", publisher: "Acme" });

export const acmeApi = destination({ id: "acme-api", hostSetting: "AcmeApiHost", defaultHost: "api.acme.com" });

export const acmeKey = httpAuth({ id: "acme-key", type: "API_KEY", header: "X-Api-Key", secret: "ApiKey" });

export const acmeOauth = httpAuth({
  id: "acme-oauth",
  type: "OAUTH2_CLIENT_CREDENTIALS",
  secret: "AcmeClient",
  scopes: ["refunds:write"],
  audience: "https://api.acme.com",
});

export const ordersGet = httpRoute({
  id: "orders.get",
  destination: "acme-api",
  method: "GET",
  path: "/orders/{orderId:[0-9]{6,12}}",
  query: ["since"],
  headers: ["X-On-Behalf-Of"],
  auth: "acme-key",
});

export const ordersRefund = httpRoute({
  id: "orders.refund",
  destination: "acme-api",
  method: "POST",
  path: "/orders/{orderId:[0-9]{6,12}}/refunds",
  headers: ["Idempotency-Key"],
  maxRequestBytes: 65536,
  maxResponseBytes: 65536,
  timeoutMs: 15000,
  auth: "acme-oauth",
});
