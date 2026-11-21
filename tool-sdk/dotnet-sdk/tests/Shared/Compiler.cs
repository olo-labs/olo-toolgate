// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.IO;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.CSharp;
using Microsoft.CodeAnalysis.Diagnostics;
using OloLabs.ToolGate.Sdk.Analyzers;

namespace OloLabs.ToolGate.Sdk.TestSupport;

/// <summary>Compiles test sources with the generator and analyzer, as a tool project build would.</summary>
internal static class Compiler
{
    /// <summary>The global usings the SDK's build props add to a tool project.</summary>
    public const string Usings = """
        global using OloLabs.ToolGate.Sdk;
        global using ValueType = OloLabs.ToolGate.Sdk.ValueType;
        global using HttpMethod = OloLabs.ToolGate.Sdk.HttpMethod;
        """;

    private static readonly Lazy<ImmutableArray<MetadataReference>> References = new(() =>
    {
        var paths = ((string)AppContext.GetData("TRUSTED_PLATFORM_ASSEMBLIES")!).Split(Path.PathSeparator).ToList();
        paths.Add(typeof(ToolContext).Assembly.Location);
        return paths.Where(p => p.Length > 0).Distinct().Select(p => (MetadataReference)MetadataReference.CreateFromFile(p)).ToImmutableArray();
    });

    public sealed class Result
    {
        public Result(Compilation compilation, ImmutableArray<Diagnostic> diagnostics, ImmutableArray<GeneratedSourceResult> generated)
        {
            Compilation = compilation;
            Diagnostics = diagnostics;
            Generated = generated;
        }

        public Compilation Compilation { get; }
        public ImmutableArray<Diagnostic> Diagnostics { get; }
        public ImmutableArray<GeneratedSourceResult> Generated { get; }

        public bool Success => Diagnostics.All(d => d.Severity != DiagnosticSeverity.Error);

        public string Errors => string.Join("\n", Diagnostics.Where(d => d.Severity == DiagnosticSeverity.Error)
            .Select(d => d.Id + ": " + d.GetMessage()));

        public IEnumerable<Diagnostic> ErrorDiagnostics => Diagnostics.Where(d => d.Severity == DiagnosticSeverity.Error);

        public string? Source(string hint) => Generated.FirstOrDefault(g => g.HintName == hint).SourceText?.ToString();

        /// <summary>The package class's canonical descriptor of a tool, read back from the generated literal.</summary>
        public string Descriptor(string tool) => Load().Tools.Single(t => t.Name == tool).DescriptorJson;

        public string Index => Load().IndexJson;

        private Hosting.GeneratedPackage? package;

        /// <summary>Emits the assembly, loads it and returns its generated package.</summary>
        public Hosting.GeneratedPackage Load()
        {
            if (package != null) return package;
            if (!Success) throw new InvalidOperationException("the compilation failed:\n" + Errors);
            using var stream = new MemoryStream();
            var emit = Compilation.Emit(stream);
            if (!emit.Success) throw new InvalidOperationException(string.Join("\n", emit.Diagnostics));
            var assembly = Assembly.Load(stream.ToArray());
            var attribute = assembly.GetCustomAttribute<Hosting.GeneratedPackageAttribute>()
                ?? throw new InvalidOperationException("no generated package");
            package = (Hosting.GeneratedPackage)Activator.CreateInstance(attribute.PackageType, nonPublic: true)!;
            Assembly = assembly;
            return package;
        }

        public Assembly? Assembly { get; private set; }
    }

    private sealed class Options : AnalyzerConfigOptionsProvider
    {
        private readonly Config global;

        public Options(Dictionary<string, string> properties)
        {
            global = new Config(properties);
        }

        public override AnalyzerConfigOptions GlobalOptions => global;
        public override AnalyzerConfigOptions GetOptions(SyntaxTree tree) => Config.Empty;
        public override AnalyzerConfigOptions GetOptions(AdditionalText textFile) => Config.Empty;

        private sealed class Config : AnalyzerConfigOptions
        {
            public static readonly Config Empty = new(new Dictionary<string, string>());
            private readonly Dictionary<string, string> values;

            public Config(Dictionary<string, string> values)
            {
                this.values = values;
            }

            public override bool TryGetValue(string key, out string value) => values.TryGetValue(key, out value!);
        }
    }

    /// <summary>Generation 1, the value a tool project sets.</summary>
    public static readonly IReadOnlyDictionary<string, string> Generation1 = new Dictionary<string, string>
    {
        ["build_property.ToolGateDescriptorGeneration"] = "1",
    };

    public static readonly IReadOnlyDictionary<string, string> NoGeneration = new Dictionary<string, string>();

    /// <summary>Compiles sources keyed by path, such as <c>Acme/Tools.cs</c>.</summary>
    public static Result Compile(IReadOnlyDictionary<string, string> sources, IReadOnlyDictionary<string, string>? properties = null)
    {
        var parse = new CSharpParseOptions(LanguageVersion.CSharp12);
        var trees = sources.Select(s => CSharpSyntaxTree.ParseText(s.Value, parse, s.Key, Encoding.UTF8)).ToList();
        trees.Add(CSharpSyntaxTree.ParseText(Usings, parse, "ToolGate.Usings.cs", Encoding.UTF8));
        var compilation = CSharpCompilation.Create("Fixture" + Guid.NewGuid().ToString("N"), trees, References.Value,
            new CSharpCompilationOptions(OutputKind.DynamicallyLinkedLibrary, nullableContextOptions: NullableContextOptions.Enable));
        var options = new Options(new Dictionary<string, string>(properties ?? Generation1));
        GeneratorDriver driver = CSharpGeneratorDriver.Create(new[] { new ToolGateGenerator().AsSourceGenerator() },
            optionsProvider: options, parseOptions: parse);
        driver = driver.RunGeneratorsAndUpdateCompilation(compilation, out var output, out var generatorDiagnostics);
        var run = driver.GetRunResult();
        var analyzers = output.WithAnalyzers(ImmutableArray.Create<DiagnosticAnalyzer>(new ToolGateAnalyzer()),
            new AnalyzerOptions(ImmutableArray<AdditionalText>.Empty, options));
        var all = analyzers.GetAllDiagnosticsAsync(CancellationToken.None).GetAwaiter().GetResult();
        var diagnostics = generatorDiagnostics.AddRange(all).Where(d => d.Severity == DiagnosticSeverity.Error || d.Id.StartsWith("TG", StringComparison.Ordinal))
            .ToImmutableArray();
        return new Result(output, diagnostics, run.Results.SelectMany(r => r.GeneratedSources).ToImmutableArray());
    }

    /// <summary>Compiles a fixture directory under tests/fixtures.</summary>
    public static Result Fixture(string name, IReadOnlyDictionary<string, string>? properties = null) =>
        Compile(FixtureSources(name), properties);

    public static Dictionary<string, string> FixtureSources(string name)
    {
        string dir = Path.Combine(AppContext.BaseDirectory, "fixtures", name);
        return Directory.GetFiles(dir, "*.cs").OrderBy(p => p, StringComparer.Ordinal)
            .ToDictionary(p => name + "/" + Path.GetFileName(p), p => File.ReadAllText(p, Encoding.UTF8));
    }

    /// <summary>A one-class package for declaration tests: the body goes inside <c>public class Tools</c>.</summary>
    public static Result Tools(string assemblyAttributes, string body, string classAttributes = "",
        IReadOnlyDictionary<string, string>? properties = null)
    {
        string source = $$"""
            using System;
            using System.Collections.Generic;
            using System.Linq;
            using System.Threading;
            using System.Threading.Tasks;
            using OloLabs.ToolGate.Sdk;
            using OloLabs.ToolGate.Sdk.Errors;

            [assembly: ToolPackage("acme.demo", Name = "Demo", Publisher = "Acme")]
            [assembly: Destination("api", HostSetting = "ApiHost")]
            [assembly: HttpRoute("items.get", Destination = "api", Method = HttpMethod.Get, Path = "/items/{id:[0-9]+}")]
            {{assemblyAttributes}}

            namespace Acme.Demo;

            {{classAttributes}}
            public class Tools
            {
                public record Out(string Value);
            {{body}}
            }
            """;
        return Compile(new Dictionary<string, string> { ["Acme/Tools.cs"] = source }, properties);
    }

    /// <summary>The descriptor vectors directory of the repository.</summary>
    public static string Vectors => Path.Combine(RepositoryRoot, "tool-sdk", "spec", "descriptor", "vectors");

    public static string Conformance => Path.Combine(RepositoryRoot, "tool-sdk", "conformance");

    public static string RepositoryRoot
    {
        get
        {
            for (var dir = new DirectoryInfo(AppContext.BaseDirectory); dir != null; dir = dir.Parent)
            {
                if (File.Exists(Path.Combine(dir.FullName, "VERSION")) && Directory.Exists(Path.Combine(dir.FullName, "tool-sdk"))) return dir.FullName;
            }
            throw new InvalidOperationException("cannot find the repository root from " + AppContext.BaseDirectory);
        }
    }
}
