// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using Microsoft.CodeAnalysis;

namespace OloLabs.ToolGate.Sdk.Analyzers.Model;

/// <summary>How a handler returns its result.</summary>
internal enum ReturnShape { Value, Task, ValueTask }

/// <summary>One handler parameter as the invoker binds it.</summary>
internal sealed class Binding
{
    public Binding(string name, string typeName)
    {
        Name = name;
        TypeName = typeName;
    }

    /// <summary>The input name, <c>$context</c> or <c>$cancellation</c>.</summary>
    public string Name { get; }
    /// <summary>Fully qualified C# type, for <c>HandlerArguments.Get&lt;T&gt;</c>.</summary>
    public string TypeName { get; }
}

/// <summary>One tool, ready to emit.</summary>
internal sealed class ToolModel
{
    public string Name = "";
    public string Canonical = "";
    public string Digest = "";
    public IMethodSymbol Method = null!;
    public List<Binding> Bindings = new();
    public ReturnShape Shape;
    public string ResultTypeName = "";
    public bool HasFactory;
}

/// <summary>What one tool handler may use, for the analyzer. Value maps are name to value type.</summary>
internal sealed class Facts
{
    public string Tool = "";
    public HashSet<string> Routes = new();
    public SortedDictionary<string, string> Inputs = new(System.StringComparer.Ordinal);
    public SortedDictionary<string, string> Globals = new(System.StringComparer.Ordinal);
    public SortedDictionary<string, string> Secrets = new(System.StringComparer.Ordinal);
    public SortedDictionary<string, string> Claims = new(System.StringComparer.Ordinal);
    public HashSet<string> Errors = new();
}

/// <summary>A declared route as the analyzer compares it: method, destination and the template without its patterns.</summary>
internal sealed class RouteShape
{
    public RouteShape(string id, string method, string destination, string template)
    {
        Id = id;
        Method = method;
        Destination = destination;
        Template = template;
    }

    public string Id { get; }
    public string Method { get; }
    public string Destination { get; }
    public string Template { get; }
}

/// <summary>One <c>&lt;HandlerClass&gt;Vars</c> class.</summary>
internal sealed class AccessorClass
{
    public string Namespace = "";
    public string ClassName = "";
    public string HandlerClass = "";
    public SortedDictionary<string, string> Globals = new(System.StringComparer.Ordinal);
    public SortedDictionary<string, string> Secrets = new(System.StringComparer.Ordinal);
    public SortedDictionary<string, string> Claims = new(System.StringComparer.Ordinal);
}

/// <summary>The result of reading one compilation's declarations.</summary>
internal sealed class PackageModel
{
    public List<Problem> Problems = new();
    public bool Failed;
    public string IndexJson = "";
    public SortedDictionary<string, ToolModel> Tools = new(System.StringComparer.Ordinal);
    public List<AccessorClass> Accessors = new();
    public List<RouteShape> Routes = new();
    public Dictionary<IMethodSymbol, Facts> Facts = new(SymbolEqualityComparer.Default);
}
