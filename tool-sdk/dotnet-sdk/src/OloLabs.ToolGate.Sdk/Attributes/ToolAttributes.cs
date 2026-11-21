// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;

namespace OloLabs.ToolGate.Sdk;

/// <summary>Members shared by <see cref="ServerToolAttribute"/> and <see cref="ClientToolAttribute"/>.</summary>
public abstract class ToolAttributeBase : Attribute
{
    protected ToolAttributeBase(string name) { Name = name; }

    /// <summary>Tool local name; the toolId is derived from the package id.</summary>
    public string Name { get; }
    /// <summary>Human title, 1-100 characters. Exactly one of Title and Text is set.</summary>
    public string? Title { get; set; }
    /// <summary>Alias of Title.</summary>
    public string? Text { get; set; }
    /// <summary>Shown to the model, 1-2048 characters. Required.</summary>
    public string? Description { get; set; }
    /// <summary>Guidance appended to the description, at most 512 characters.</summary>
    public string? UseWhen { get; set; }
    public EffectSafety Effect { get; set; } = EffectSafety.Unknown;
    public bool Destructive { get; set; }
    public OpenWorld OpenWorld { get; set; } = OpenWorld.Default;
    /// <summary>Runs as a task.</summary>
    public bool Async { get; set; }
    /// <summary>-1 means 30000 for sync tools and 900000 for async tools.</summary>
    public long MaxDurationMs { get; set; } = -1;
    public bool Stateless { get; set; }
    /// <summary>-1 means unlimited.</summary>
    public int MaxConcurrency { get; set; } = -1;
    /// <summary>Route ids this tool may call.</summary>
    public string[]? Routes { get; set; }
    /// <summary>TCP destination ids this tool may open.</summary>
    public string[]? Tcp { get; set; }
    public BusinessKey RequiresBusinessKey { get; set; } = BusinessKey.Default;
    public KeyNamespace BusinessKeyNamespace { get; set; } = KeyNamespace.Agent;
    /// <summary>Input carrying the business key when it is not sent as Idempotency-Key.</summary>
    public string? BusinessKeyArgument { get; set; }
    /// <summary>Tasks only.</summary>
    public bool CancelIfAbandoned { get; set; }
    public string[]? Toolsets { get; set; }
}

/// <summary>Declares a tool that runs on a Tool Host or in standalone mode.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public sealed class ServerToolAttribute : ToolAttributeBase
{
    public ServerToolAttribute(string name) : base(name) { }
}

/// <summary>
/// Declares a tool that runs on an endpoint device. Client tools have no network access or secrets:
/// Routes, Tcp, secrets and HttpAuth on a client tool are build errors.
/// </summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public sealed class ClientToolAttribute : ToolAttributeBase
{
    public ClientToolAttribute(string name) : base(name) { }

    /// <summary>Defaults to <see cref="RuntimeTier.OciSandbox"/>.</summary>
    public RuntimeTier[]? Tiers { get; set; }
}

/// <summary>A declared business error code of the tool.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = true)]
public sealed class ErrorDeclAttribute : Attribute
{
    public ErrorDeclAttribute(string code, string description) { Code = code; Description = description; }

    public string Code { get; }
    public string Description { get; }
    public bool Retryable { get; set; }
    /// <summary>Optional JSON Schema for the error data.</summary>
    public string? DataSchema { get; set; }
}

/// <summary>Forwards the business key upstream in a header.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public sealed class IdempotencyAttribute : Attribute
{
    public IdempotencyAttribute(string header) { Header = header; }

    public string Header { get; }
    public IdempotencyScope Scope { get; set; } = IdempotencyScope.BusinessKey;
}

/// <summary>Marks a tool handler that reconciles OUTCOME_UNKNOWN invocations of another tool.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public sealed class ReconcileAttribute : Attribute
{
    public ReconcileAttribute(string tool) { Tool = tool; }

    /// <summary>Local name of the tool this handler reconciles.</summary>
    public string Tool { get; }
}

/// <summary>A self-test; at most 8 per tool.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = true)]
public sealed class ExampleAttribute : Attribute
{
    /// <param name="arguments">A JSON object.</param>
    public ExampleAttribute(string arguments) { Arguments = arguments; }

    public string Arguments { get; }
    public string? ExpectedOutput { get; set; }
    public string? ExpectedError { get; set; }
    /// <summary>
    /// A project-relative file holding the recorded exchanges this example's route calls get, in the format of
    /// spec/authoring/testing.md; the build embeds <c>exchanges/**/*.json</c>. An example without one must not call a route.
    /// </summary>
    public string? Exchanges { get; set; }
}
