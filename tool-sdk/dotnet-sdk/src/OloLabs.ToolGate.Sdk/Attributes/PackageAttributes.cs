// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;

namespace OloLabs.ToolGate.Sdk;

// Package-scope declarations are assembly attributes. The source generator reads them from source;
// nothing here is read by reflection at run time.

/// <summary>Declares the package; exactly one per assembly. The version comes from the build.</summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = false)]
public sealed class ToolPackageAttribute : Attribute
{
    public ToolPackageAttribute(string id) { Id = id; }

    public string Id { get; }
    /// <summary>Required.</summary>
    public string? Name { get; set; }
    /// <summary>Required.</summary>
    public string? Publisher { get; set; }
    public string? Description { get; set; }
}

/// <summary>An HTTP destination; the host is a reviewed service-profile setting.</summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public sealed class DestinationAttribute : Attribute
{
    public DestinationAttribute(string id) { Id = id; }

    public string Id { get; }
    /// <summary>Required.</summary>
    public string? HostSetting { get; set; }
    /// <summary>Proposed value only; never part of the descriptor.</summary>
    public string? DefaultHost { get; set; }
    public int Port { get; set; } = 443;
    public string[]? SpkiPins { get; set; }
    public string? CaBundle { get; set; }
}

/// <summary>How routes authenticate.</summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public sealed class HttpAuthAttribute : Attribute
{
    public HttpAuthAttribute(string id, AuthType type) { Id = id; Type = type; }

    public string Id { get; }
    public AuthType Type { get; }
    public string? Header { get; set; }
    public string? Secret { get; set; }
    public string? Connection { get; set; }
    public string[]? Scopes { get; set; }
    public string? Audience { get; set; }
}

/// <summary>A named HTTP route, the only way tool code reaches HTTP.</summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public sealed class HttpRouteAttribute : Attribute
{
    public HttpRouteAttribute(string id) { Id = id; }

    public string Id { get; }
    /// <summary>Required.</summary>
    public string? Destination { get; set; }
    public HttpMethod Method { get; set; } = HttpMethod.Get;
    /// <summary>RFC 6570 level-1 template; variables carry a pattern, <c>{name:regex}</c>. Required.</summary>
    public string? Path { get; set; }
    public string[]? Query { get; set; }
    public string[]? Headers { get; set; }
    /// <summary>Defaults to <c>application/json</c>.</summary>
    public string[]? RequestContentTypes { get; set; }
    public long MaxRequestBytes { get; set; } = 1_048_576;
    public long MaxResponseBytes { get; set; } = 4_194_304;
    public Streaming Streaming { get; set; } = Streaming.None;
    public int TimeoutMs { get; set; } = 10_000;
    public string? Auth { get; set; }
}

/// <summary>A TCP destination for <see cref="ITcpConnector.ConnectAsync"/>.</summary>
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = true)]
public sealed class TcpDestinationAttribute : Attribute
{
    public TcpDestinationAttribute(string id) { Id = id; }

    public string Id { get; }
    /// <summary>Required.</summary>
    public string? HostSetting { get; set; }
    public string? DefaultHost { get; set; }
    /// <summary>Required, 1..65535.</summary>
    public int Port { get; set; }
}
