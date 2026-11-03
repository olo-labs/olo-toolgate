// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Threading;
using System.Threading.Tasks;

namespace OloLabs.ToolGate.Sdk.Hosting;

// The contract between the source generator and the runtime. Tool code does not use these types.

/// <summary>Names the generated package class of an assembly.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
[AttributeUsage(AttributeTargets.Assembly, AllowMultiple = false)]
public sealed class GeneratedPackageAttribute : Attribute
{
    public GeneratedPackageAttribute(Type packageType) { PackageType = packageType; }

    public Type PackageType { get; }
}

/// <summary>A package as the generator emitted it: the index and each tool's canonical descriptor and invoker.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public abstract class GeneratedPackage
{
    /// <summary>The package index (canonical JSON): id, name, publisher and per tool the digest, handler and examples.</summary>
    public abstract string IndexJson { get; }

    public abstract IReadOnlyList<GeneratedTool> Tools { get; }
}

/// <summary>One tool: its descriptor's canonical JSON and how to call its handler.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public sealed class GeneratedTool
{
    public GeneratedTool(string name, string descriptorJson, Type handlerType, Func<object>? factory,
        Func<object, HandlerArguments, ValueTask<object?>> invoke)
    {
        Name = name;
        DescriptorJson = descriptorJson;
        HandlerType = handlerType;
        Factory = factory;
        Invoke = invoke;
    }

    /// <summary>The tool's local name.</summary>
    public string Name { get; }
    /// <summary>The canonical descriptor; its UTF-8 bytes are what the digest covers.</summary>
    public string DescriptorJson { get; }
    public Type HandlerType { get; }
    /// <summary>Null when the handler class has no public parameterless constructor.</summary>
    public Func<object>? Factory { get; }
    public Func<object, HandlerArguments, ValueTask<object?>> Invoke { get; }
}

/// <summary>The arguments of one handler call, bound by name.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public abstract class HandlerArguments
{
    /// <summary>The input converted to <typeparamref name="T"/>; default when absent.</summary>
    public abstract T Get<T>(string name);

    public abstract ToolContext Context { get; }

    public abstract CancellationToken Cancellation { get; }
}

/// <summary>Boxes handler results for <see cref="GeneratedTool.Invoke"/>.</summary>
[EditorBrowsable(EditorBrowsableState.Never)]
public static class HandlerResult
{
    public static ValueTask<object?> FromValue<T>(T value) => new(value);

    public static async ValueTask<object?> FromTask<T>(Task<T> task) => await task.ConfigureAwait(false);

    public static async ValueTask<object?> FromValueTask<T>(ValueTask<T> task) => await task.ConfigureAwait(false);
}
