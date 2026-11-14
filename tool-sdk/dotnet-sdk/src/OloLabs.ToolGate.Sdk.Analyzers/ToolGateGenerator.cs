// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Text;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.Diagnostics;
using Microsoft.CodeAnalysis.Text;
using OloLabs.ToolGate.Sdk.Analyzers.Model;

namespace OloLabs.ToolGate.Sdk.Analyzers;

/// <summary>
/// Reads the authoring attributes, reports every declaration problem as a build error, and emits the package class
/// (canonical descriptors, index and invokers), the <c>&lt;Class&gt;Vars</c> typed accessors and, for a
/// <c>ToolGatePackage</c> project, the entry point.
/// </summary>
/// <remarks>
/// Descriptors are computed in the generator rather than by a post-compile MSBuild step: they hold only strings,
/// booleans, integers and finite doubles, the canonicalizer (NFC, RFC 8785) is plain C# shared with the runtime,
/// and running here makes every declaration check a compiler diagnostic at the attribute that caused it.
/// </remarks>
[Generator(LanguageNames.CSharp)]
public sealed class ToolGateGenerator : IIncrementalGenerator
{
    internal sealed class Options
    {
        public string? Generation;
        public bool Package;
        public bool EntryPoint = true;

        public static Options From(AnalyzerConfigOptions global)
        {
            var o = new Options();
            if (global.TryGetValue("build_property." + PackageBuilder.GenerationProperty, out var g) && g.Trim().Length > 0) o.Generation = g;
            if (global.TryGetValue("build_property.ToolGatePackage", out var p)) o.Package = p.Trim().Equals("true", System.StringComparison.OrdinalIgnoreCase);
            if (global.TryGetValue("build_property.ToolGateGenerateEntryPoint", out var e) && e.Trim().Equals("false", System.StringComparison.OrdinalIgnoreCase))
            {
                o.EntryPoint = false;
            }
            return o;
        }
    }

    public void Initialize(IncrementalGeneratorInitializationContext context)
    {
        var options = context.AnalyzerConfigOptionsProvider.Select((p, _) => Options.From(p.GlobalOptions));
        context.RegisterSourceOutput(context.CompilationProvider.Combine(options), (spc, input) => Execute(spc, input.Left, input.Right));
    }

    private static void Execute(SourceProductionContext context, Compilation compilation, Options options)
    {
        if (compilation.GetTypeByMetadataName("OloLabs.ToolGate.Sdk.ToolPackageAttribute") == null) return;
        var model = PackageBuilder.Build(compilation, options.Generation);
        if (model == null)
        {
            if (options.Package)
            {
                context.ReportDiagnostic(Diagnostic.Create(Diagnostics.Declaration, Location.None,
                    "a ToolGatePackage project needs [assembly: ToolPackage] and at least one tool"));
            }
            return;
        }
        foreach (var p in model.Problems)
        {
            context.ReportDiagnostic(Diagnostic.Create(Diagnostics.ById(p.Id), p.At, p.Message));
        }
        if (model.Failed) return;
        context.AddSource("ToolGatePackage.g.cs", SourceText.From(Emitter.Package(model), Encoding.UTF8));
        foreach (var a in model.Accessors)
        {
            string hint = (a.Namespace.Length == 0 ? "" : a.Namespace + ".") + a.ClassName + ".g.cs";
            context.AddSource(hint, SourceText.From(Emitter.Accessors(a), Encoding.UTF8));
        }
        if (options.Package && options.EntryPoint)
        {
            context.AddSource("ToolGateProgram.g.cs", SourceText.From(Emitter.EntryPoint(), Encoding.UTF8));
        }
    }
}
