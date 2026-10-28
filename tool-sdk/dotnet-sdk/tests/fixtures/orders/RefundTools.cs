// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.Text.Json.Serialization;
using System.Threading.Tasks;
using OloLabs.ToolGate.Sdk;

namespace Acme.Orders;

[Secret("AcmeClient", Type = ValueType.Object)]
public class RefundTools
{
    public enum Reason { DAMAGED, LATE, ÉCHANGE }

    // C# identifiers cannot hold € or an emoji, so those properties name themselves with [Param]; camel-casing
    // would lower the full-width Ａ, so that one keeps its name with [JsonPropertyName].
    public record Refund(bool zeta, string état, [Param(Name = "€amount", Description = "", Min = "0")] long amount,
        [Param(Name = "😀note", Description = "")] string note, [property: JsonPropertyName("Ａlpha")] string Ａlpha);

    [ServerTool("orders.refund", Title = "Refund an order",
        Description = "Refunds an order in full. Café orders are refunded to the original card; the amount is in €.",
        Effect = EffectSafety.NonIdempotent, Destructive = true, Async = true, Stateless = true, MaxConcurrency = 4,
        Routes = new[] { "orders.refund" }, BusinessKeyNamespace = KeyNamespace.Tenant, BusinessKeyArgument = "refundKey",
        Toolsets = new[] { "payments" })]
    [ErrorDecl("PSP_BUSY", "The payment provider asked us to retry", Retryable = true)]
    [ErrorDecl("ALREADY_REFUNDED", "The order was already refunded")]
    [Idempotency("Idempotency-Key")]
    public Task<Refund> RefundOrder(
        [Param(Description = "Order number", Pattern = "^[0-9]{6,12}$")] string orderId,
        [Param(Description = "Reason shown to the customer")] Reason reason,
        [Param(Description = "Business key for this refund", MinLength = 8, MaxLength = 64)] string refundKey,
        ToolContext context) =>
        context.Http.Route("orders.refund").PathVar("orderId", orderId)
            .Body(new Dictionary<string, object> { ["reason"] = reason }).SendAsync<Refund>();

    [ServerTool("orders.refund-status", Title = "Check a refund", Description = "Reconciles an uncertain refund.",
        Effect = EffectSafety.ReadOnly)]
    [Reconcile("orders.refund")]
    public Dictionary<string, object?> RefundStatus([Param(Description = "Order number")] string orderId) => new();
}
