// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;

namespace OloLabs.ToolGate.Sdk.Analyzers.Model;

/// <summary>The members Param, Variable and Global share. Value types use the neutral names (INT, DATE_TIME, ...).</summary>
internal sealed class Spec
{
    public string Name = "";
    public string Description = "";
    public string Type = "INFER";
    public bool Required = true;
    public string DefaultValue = "";
    public string Min = "";
    public string Max = "";
    public int MinLength = -1;
    public int MaxLength = -1;
    public string Pattern = "";
    public List<string> EnumValues = new();
    public string Format = "";
    public bool Sensitive;

    /// <summary>Reads the shared constraint members; name, description, type and required come from the caller.</summary>
    public static Spec Of(Attr a, string name, string description, string type, bool required)
    {
        return new Spec
        {
            Name = name,
            Description = description,
            Type = type,
            Required = required,
            DefaultValue = a.String("DefaultValue"),
            Min = a.String("Min"),
            Max = a.String("Max"),
            MinLength = a.Int("MinLength", -1),
            MaxLength = a.Int("MaxLength", -1),
            Pattern = a.String("Pattern"),
            EnumValues = a.Strings("EnumValues"),
            Format = a.String("Format"),
            Sensitive = a.Bool("Sensitive"),
        };
    }

    /// <summary>A [Param].</summary>
    public static Spec OfParam(Attr a) =>
        Of(a, a.String("Name"), a.String("Description"), a.Enum("Type", "INFER"), a.Bool("Required", true));

    /// <summary>A [Variable] (name, description, type).</summary>
    public static Spec OfVariable(Attr a) =>
        Of(a, a.CtorString(0), a.CtorString(1), a.CtorEnum(2, "INFER"), a.Bool("Required", true));

    /// <summary>A [Global] (name, type).</summary>
    public static Spec OfGlobal(Attr a) =>
        Of(a, a.CtorString(0), a.String("Description"), a.CtorEnum(1, "INFER"), a.Bool("Required", true));
}
