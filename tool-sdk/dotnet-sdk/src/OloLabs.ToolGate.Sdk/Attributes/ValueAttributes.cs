// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;

namespace OloLabs.ToolGate.Sdk;

/// <summary>Constraint members shared by inputs, properties, globals and grouped variables.</summary>
public abstract class ConstrainedValueAttribute : Attribute
{
    /// <summary>Parsed as the value type; a default makes the value optional.</summary>
    public string? DefaultValue { get; set; }
    /// <summary>Minimum, parsed as the value type (Int, Long, Number).</summary>
    public string? Min { get; set; }
    /// <summary>Maximum, parsed as the value type.</summary>
    public string? Max { get; set; }
    /// <summary>Minimum string length; -1 for none.</summary>
    public int MinLength { get; set; } = -1;
    /// <summary>Maximum string length; -1 for none.</summary>
    public int MaxLength { get; set; } = -1;
    /// <summary>ECMA-262 pattern.</summary>
    public string? Pattern { get; set; }
    /// <summary>Allowed values, in declaration order; makes a string an Enum.</summary>
    public string[]? EnumValues { get; set; }
    /// <summary>JSON Schema format, for String values only.</summary>
    public string? Format { get; set; }
    /// <summary>Redacted from logs, errors and audit.</summary>
    public bool Sensitive { get; set; }
}

/// <summary>An AI-supplied input on a handler parameter, or a property of an input or output type.</summary>
[AttributeUsage(AttributeTargets.Parameter | AttributeTargets.Property | AttributeTargets.Field, AllowMultiple = false)]
public sealed class ParamAttribute : ConstrainedValueAttribute
{
    /// <summary>Defaults to the JSON name of the parameter or property.</summary>
    public string? Name { get; set; }
    /// <summary>Required for inputs; empty omits it on output properties.</summary>
    public string? Description { get; set; }
    public ValueType Type { get; set; } = ValueType.Infer;
    /// <summary>A nullable type is optional without setting this.</summary>
    public bool Required { get; set; } = true;
}

/// <summary>A configuration value supplied by the approved configuration revision.</summary>
[AttributeUsage(AttributeTargets.Class | AttributeTargets.Method, AllowMultiple = true)]
public sealed class GlobalAttribute : ConstrainedValueAttribute
{
    public GlobalAttribute(string name, ValueType type) { Name = name; Type = type; }

    public string Name { get; }
    public ValueType Type { get; }
    public bool Required { get; set; } = true;
    public string? Description { get; set; }
}

/// <summary>A secret delivered by Control or the credential broker.</summary>
[AttributeUsage(AttributeTargets.Class | AttributeTargets.Method, AllowMultiple = true)]
public sealed class SecretAttribute : Attribute
{
    public SecretAttribute(string name) { Name = name; }

    public string Name { get; }
    public ValueType Type { get; set; } = ValueType.String;
    public SecretScope Scope { get; set; } = SecretScope.Group;
    public string? Description { get; set; }
}

/// <summary>A claim of the verified caller identity.</summary>
[AttributeUsage(AttributeTargets.Class | AttributeTargets.Method, AllowMultiple = true)]
public sealed class JwtClaimAttribute : Attribute
{
    public JwtClaimAttribute(string name) { Name = name; }

    public string Name { get; }
    /// <summary>A JSON pointer or a top-level claim name. Required.</summary>
    public string? Claim { get; set; }
    public ValueType Type { get; set; } = ValueType.String;
    public bool Required { get; set; } = true;
}

/// <summary>
/// The grouped form: the handler takes only <see cref="ToolContext"/> and declares its inputs with
/// <see cref="VariableAttribute"/>; its globals, claims and secrets use the usual attributes on the method.
/// </summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = false)]
public sealed class ToolVariablesAttribute : Attribute
{
}

/// <summary>An input in the grouped <see cref="ToolVariablesAttribute"/> form.</summary>
[AttributeUsage(AttributeTargets.Method, AllowMultiple = true)]
public sealed class VariableAttribute : ConstrainedValueAttribute
{
    public VariableAttribute(string name, string description, ValueType type)
    {
        Name = name;
        Description = description;
        Type = type;
    }

    public string Name { get; }
    public string Description { get; }
    public ValueType Type { get; }
    public bool Required { get; set; } = true;
}
