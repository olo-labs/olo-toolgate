// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.Threading.Tasks;
using OloLabs.ToolGate.Sdk;

namespace Acme.Orders;

[Global("TimeoutSeconds", ValueType.Int, DefaultValue = "10")]
[Secret("ApiKey")]
[JwtClaim("UserName", Claim = "preferred_username")]
public class LookupTools
{
    public enum Status { OPEN, SHIPPED, CANCELLED }

    public record Item(string Sku, int Quantity);

    public record Order(string OrderId, Status Status, IReadOnlyList<Item> Items);

    [ServerTool("orders.lookup", Title = "Look up an order", Description = "Returns status and items for one order.",
        UseWhen = "The user asks about a specific order number.", Effect = EffectSafety.ReadOnly,
        Routes = new[] { "orders.get" })]
    [ErrorDecl("ORDER_NOT_FOUND", "No such order")]
    public Task<Order> Lookup(
        [Param(Description = "Order number", Pattern = "^[0-9]{6,12}$")] string orderId,
        [Param(Description = "Only events after this epoch second")] int? since,
        ToolContext context) =>
        context.Http.Route("orders.get").PathVar("orderId", orderId).Query("since", since).SendAsync<Order>();
}
