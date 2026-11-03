// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using Microsoft.CodeAnalysis;

namespace OloLabs.ToolGate.Sdk.Analyzers;

/// <summary>The TG diagnostics. Every one is an error: a package that would fail them never builds.</summary>
internal static class Diagnostics
{
    public const string GenerationId = "TG0001";
    public const string DeclarationId = "TG0002";
    public const string ClientToolId = "TG0003";
    public const string UndeclaredValueId = "TG0010";
    public const string WrongGetterId = "TG0011";
    public const string ConvenienceId = "TG0020";
    public const string ReflectionId = "TG0030";
    public const string UndeclaredErrorId = "TG0040";

    private const string Category = "ToolGate";

    public static readonly DiagnosticDescriptor Generation = Make(GenerationId, "Descriptor generation is missing or unsupported");
    public static readonly DiagnosticDescriptor Declaration = Make(DeclarationId, "Invalid ToolGate declaration");
    public static readonly DiagnosticDescriptor ClientTool = Make(ClientToolId, "Client tools have no network access or secrets");
    public static readonly DiagnosticDescriptor UndeclaredValue = Make(UndeclaredValueId, "The tool does not declare this value");
    public static readonly DiagnosticDescriptor WrongGetter = Make(WrongGetterId, "The getter does not read the declared value type");
    public static readonly DiagnosticDescriptor Convenience = Make(ConvenienceId, "The convenience call cannot be proven to match one declared route");
    public static readonly DiagnosticDescriptor Reflection = Make(ReflectionId, "Reflective access to the network API cannot be analysed");
    public static readonly DiagnosticDescriptor UndeclaredError = Make(UndeclaredErrorId, "The tool does not declare this error code");

    public static DiagnosticDescriptor ById(string id) => id switch
    {
        GenerationId => Generation,
        ClientToolId => ClientTool,
        UndeclaredValueId => UndeclaredValue,
        WrongGetterId => WrongGetter,
        ConvenienceId => Convenience,
        ReflectionId => Reflection,
        UndeclaredErrorId => UndeclaredError,
        _ => Declaration,
    };

    private static DiagnosticDescriptor Make(string id, string title) =>
        new(id, title, "{0}", Category, DiagnosticSeverity.Error, isEnabledByDefault: true,
            helpLinkUri: null, customTags: WellKnownDiagnosticTags.NotConfigurable);
}
