// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.Linq;
using System.Text;
using Microsoft.CodeAnalysis;

namespace OloLabs.ToolGate.Sdk.Analyzers.Model;

/// <summary>A declaration problem; becomes a build error at its location.</summary>
internal sealed class Problem : System.Exception
{
    public Problem(Location? at, string message, string id = Diagnostics.DeclarationId) : base(message)
    {
        At = at ?? Location.None;
        Id = id;
    }

    public Location At { get; }
    public string Id { get; }
}

/// <summary>Typed reads of one attribute application, with the defaults the SDK attributes declare.</summary>
internal sealed class Attr
{
    private readonly AttributeData data;

    public Attr(AttributeData data, Location fallback)
    {
        this.data = data;
        Location = data.ApplicationSyntaxReference?.GetSyntax().GetLocation() ?? fallback;
    }

    public Location Location { get; }

    public string ClassName => data.AttributeClass?.Name ?? "";

    private TypedConstant? Named(string name)
    {
        foreach (var kv in data.NamedArguments) if (kv.Key == name) return kv.Value;
        return null;
    }

    public bool Has(string name) => Named(name) != null;

    public string CtorString(int index)
    {
        if (data.ConstructorArguments.Length <= index) return "";
        return data.ConstructorArguments[index].Value as string ?? "";
    }

    public string CtorEnum(int index, string fallback)
    {
        if (data.ConstructorArguments.Length <= index) return fallback;
        return EnumName(data.ConstructorArguments[index]) ?? fallback;
    }

    public string String(string name, string fallback = "")
    {
        var v = Named(name);
        return v?.Value as string ?? fallback;
    }

    public bool Bool(string name, bool fallback = false)
    {
        var v = Named(name);
        return v?.Value is bool b ? b : fallback;
    }

    public int Int(string name, int fallback)
    {
        var v = Named(name);
        return v?.Value is int i ? i : fallback;
    }

    public long Long(string name, long fallback)
    {
        var v = Named(name);
        if (v?.Value is long l) return l;
        if (v?.Value is int i) return i;
        return fallback;
    }

    public List<string> Strings(string name, params string[] fallback)
    {
        var v = Named(name);
        if (v == null || v.Value.IsNull) return fallback.ToList();
        if (v.Value.Kind != TypedConstantKind.Array) return fallback.ToList();
        var output = new List<string>();
        foreach (var item in v.Value.Values)
        {
            if (item.Value is not string s) throw new Problem(Location, ClassName + "." + name + " may not contain null");
            output.Add(s);
        }
        return output;
    }

    public string Enum(string name, string fallback)
    {
        var v = Named(name);
        return v == null ? fallback : EnumName(v.Value) ?? fallback;
    }

    public List<string> Enums(string name, params string[] fallback)
    {
        var v = Named(name);
        if (v == null || v.Value.IsNull || v.Value.Kind != TypedConstantKind.Array) return fallback.ToList();
        return v.Value.Values.Select(x => EnumName(x) ?? "").ToList();
    }

    /// <summary>The neutral upper-snake name of an enum constant, e.g. OAuth2ClientCredentials is OAUTH2_CLIENT_CREDENTIALS.</summary>
    private static string? EnumName(TypedConstant c)
    {
        if (c.Type is not INamedTypeSymbol type || c.Value == null) return null;
        foreach (var member in type.GetMembers().OfType<IFieldSymbol>())
        {
            if (member.HasConstantValue && Equals(member.ConstantValue, c.Value)) return UpperSnake(member.Name);
        }
        return null;
    }

    public static string UpperSnake(string pascal)
    {
        var output = new StringBuilder();
        for (int i = 0; i < pascal.Length; i++)
        {
            char c = pascal[i];
            if (i > 0 && char.IsUpper(c) && (char.IsLower(pascal[i - 1]) || char.IsDigit(pascal[i - 1]))) output.Append('_');
            output.Append(char.ToUpperInvariant(c));
        }
        return output.ToString();
    }
}
