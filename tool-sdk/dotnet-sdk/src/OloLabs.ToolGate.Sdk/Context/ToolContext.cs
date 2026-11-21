// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using Microsoft.Extensions.Logging;

namespace OloLabs.ToolGate.Sdk;

/// <summary>
/// Typed read access to declared values. Reading an undeclared name, or with the wrong type, is a build
/// error where the analyzer can see it and a <see cref="Errors.ToolConfigurationError"/> otherwise.
/// </summary>
public interface IValues
{
    string GetStringValue(string name);
    int GetIntValue(string name);
    long GetLongValue(string name);
    double GetNumberValue(string name);
    bool GetBooleanValue(string name);
    DateOnly GetDateValue(string name);
    DateTimeOffset GetDateTimeValue(string name);
    TimeSpan GetDurationValue(string name);
    T GetObjectValue<T>(string name);
    IReadOnlyList<T> GetListValue<T>(string name);
    byte[] GetBinaryValue(string name);
}

/// <summary>Declared claims of the verified caller, plus the standard identity members.</summary>
public interface IJwt : IValues
{
    string GetUserName();
    string GetSubject();
    string GetIssuer();
}

/// <summary>
/// Everything a handler can reach besides its typed parameters. Identical in standalone and governed mode.
/// The value getters read AI inputs in the grouped <see cref="ToolVariablesAttribute"/> form.
/// </summary>
#pragma warning disable IDE1006 // the spec names this interface ToolContext
public interface ToolContext : IValues
#pragma warning restore IDE1006
{
    IValues Global { get; }
    IValues Secret { get; }
    IJwt JWT { get; }

    IHttpClient Http { get; }
    ITcpConnector Tcp { get; }
    Invocation Invocation { get; }

    /// <summary>Fraction 0..1, monotonic; ignored for sync calls without a progress token.</summary>
    void Progress(double fraction, string? message = null);
    bool IsCancelled { get; }
    /// <summary>Bound to the invocation: cancelled on caller cancellation and at the deadline.</summary>
    CancellationToken CancellationToken { get; }

    /// <summary>A question for the caller: an MRTR round in sync calls, <c>input_required</c> in tasks.</summary>
    Task<T> RequestInputAsync<T>(InputRequest<T> request);

    /// <summary>The only supported log channel; entries are redacted and size-capped.</summary>
    ILogger Log { get; }
}
