// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using OloLabs.ToolGate.Sdk;

[assembly: ToolPackage("acme.orders", Name = "Acme orders", Publisher = "Acme")]
[assembly: Destination("acme-api", HostSetting = "AcmeApiHost", DefaultHost = "api.acme.com")]
[assembly: HttpAuth("acme-key", AuthType.ApiKey, Header = "X-Api-Key", Secret = "ApiKey")]
[assembly: HttpAuth("acme-oauth", AuthType.OAuth2ClientCredentials, Secret = "AcmeClient",
    Scopes = new[] { "refunds:write" }, Audience = "https://api.acme.com")]
[assembly: HttpRoute("orders.get", Destination = "acme-api", Method = HttpMethod.Get, Path = "/orders/{orderId:[0-9]{6,12}}",
    Query = new[] { "since" }, Headers = new[] { "X-On-Behalf-Of" }, Auth = "acme-key")]
[assembly: HttpRoute("orders.refund", Destination = "acme-api", Method = HttpMethod.Post, Path = "/orders/{orderId:[0-9]{6,12}}/refunds",
    Headers = new[] { "Idempotency-Key" }, MaxRequestBytes = 65536, MaxResponseBytes = 65536, TimeoutMs = 15000,
    Auth = "acme-oauth")]
