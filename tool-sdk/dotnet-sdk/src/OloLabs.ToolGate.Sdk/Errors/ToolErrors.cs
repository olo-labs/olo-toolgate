// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Linq;

namespace OloLabs.ToolGate.Sdk.Errors;

/// <summary>Base of every error a handler raises (errors.md).</summary>
public abstract class ToolError : Exception
{
    protected ToolError(string code, string message, Exception? cause = null) : base(message, cause)
    {
        Code = code;
    }

    public string Code { get; }
}

/// <summary>The arguments are invalid; code INVALID_ARGUMENT.</summary>
public sealed class ToolInputError : ToolError
{
    public ToolInputError(string message) : base("INVALID_ARGUMENT", message) { }
}

/// <summary>A business outcome with a code declared in the tool's errors.</summary>
public class ToolBusinessError : ToolError
{
    private static readonly IReadOnlyDictionary<string, object?> Empty = new Dictionary<string, object?>();

    public ToolBusinessError(string code, string message) : this(code, message, null) { }

    /// <param name="data">Serialized with System.Text.Json; must validate against the declared data schema.</param>
    public ToolBusinessError(string code, string message, IReadOnlyDictionary<string, object?>? data) : base(code, message)
    {
        ErrorData = data == null ? Empty : data.ToDictionary(e => e.Key, e => e.Value);
    }

    /// <summary>The error data (Exception.Data is a different, untyped bag).</summary>
    public IReadOnlyDictionary<string, object?> ErrorData { get; }
}

/// <summary>A transient failure with a declared code; retryable only for ReadOnly and Idempotent tools.</summary>
public class ToolRetryableError : ToolError
{
    public ToolRetryableError(string code, string message, TimeSpan? retryAfter = null) : base(code, message)
    {
        RetryAfter = retryAfter;
    }

    public TimeSpan? RetryAfter { get; }
}

/// <summary>A route call failed: UPSTREAM_4XX, UPSTREAM_5XX, UPSTREAM_TIMEOUT or UPSTREAM_CONNECTION.</summary>
public sealed class UpstreamError : ToolError
{
    public UpstreamError(string code, string route, int? status, string? upstreamRequestId, bool effectUncertain, string? body)
        : base(code, "Upstream call on route " + route + " failed: " + code)
    {
        Route = route;
        Status = status;
        UpstreamRequestId = upstreamRequestId;
        EffectUncertain = effectUncertain;
        Body = body;
    }

    public string Route { get; }
    public int? Status { get; }
    public string? UpstreamRequestId { get; }
    /// <summary>True when the request was fully sent and no complete response arrived.</summary>
    public bool EffectUncertain { get; }
    public string? Body { get; }
}

/// <summary>A declaration or configuration problem; code CONFIGURATION.</summary>
public sealed class ToolConfigurationError : ToolError
{
    public ToolConfigurationError(string message) : base("CONFIGURATION", message) { }
}

/// <summary>The platform codes of errors.md §2.</summary>
public enum PlatformErrorCode
{
    Cancelled, DeadlineExceeded, RouteRefused, AuditUnavailable, ClaimsUnavailable,
    ClientToolsUnavailable, UnsupportedTier, ConcurrencyLimit,
}

/// <summary>Raised by the SDK only.</summary>
public sealed class PlatformError : ToolError
{
    public PlatformError(PlatformErrorCode code, string message) : base(CodeName(code), message)
    {
        PlatformCode = code;
    }

    public PlatformErrorCode PlatformCode { get; }

    /// <summary>The upper-snake code, e.g. DEADLINE_EXCEEDED.</summary>
    public static string CodeName(PlatformErrorCode code) => code switch
    {
        PlatformErrorCode.Cancelled => "CANCELLED",
        PlatformErrorCode.DeadlineExceeded => "DEADLINE_EXCEEDED",
        PlatformErrorCode.RouteRefused => "ROUTE_REFUSED",
        PlatformErrorCode.AuditUnavailable => "AUDIT_UNAVAILABLE",
        PlatformErrorCode.ClaimsUnavailable => "CLAIMS_UNAVAILABLE",
        PlatformErrorCode.ClientToolsUnavailable => "CLIENT_TOOLS_UNAVAILABLE",
        PlatformErrorCode.UnsupportedTier => "UNSUPPORTED_TIER",
        PlatformErrorCode.ConcurrencyLimit => "CONCURRENCY_LIMIT",
        _ => throw new ArgumentOutOfRangeException(nameof(code)),
    };
}
