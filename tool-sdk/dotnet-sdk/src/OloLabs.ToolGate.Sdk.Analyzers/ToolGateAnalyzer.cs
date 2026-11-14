// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using Microsoft.CodeAnalysis;
using Microsoft.CodeAnalysis.Diagnostics;
using Microsoft.CodeAnalysis.Operations;
using OloLabs.ToolGate.Sdk.Analyzers.Model;

namespace OloLabs.ToolGate.Sdk.Analyzers;

/// <summary>
/// The build-time analyser of http.md §3 and dotnet.md, a port of the Java SourceAnalysis. It proves every URL-style
/// convenience call (<c>Http.Get(destination, template)</c> and its siblings): both arguments are constant strings,
/// the call sits in a tool handler, and method, destination and template match exactly one declared route that the
/// handler's tool lists. Anything it cannot prove is an error at the call site, as is <c>typeof</c> of the network
/// API. It also checks <c>Get*Value</c> calls with a constant name (the name must be declared for the tool and the
/// getter must read its type), and business error codes the handler raises with a constant code.
/// </summary>
[DiagnosticAnalyzer(LanguageNames.CSharp)]
public sealed class ToolGateAnalyzer : DiagnosticAnalyzer
{
    private const string Ns = "OloLabs.ToolGate.Sdk";
    private static readonly HashSet<string> ConvenienceMethods = new() { "Get", "Post", "Put", "Patch", "Delete", "Head" };
    private static readonly HashSet<string> NetworkTypes = new() { Ns + ".IHttpClient", Ns + ".IRouteCall", Ns + ".ITcpConnector" };

    /// <summary>The value types each getter reads, exactly as the runtime's DeclaredValues accepts them.</summary>
    internal static readonly Dictionary<string, HashSet<string>> Getters = new()
    {
        ["GetStringValue"] = new() { "STRING", "ENUM", "URI", "DATE", "DATE_TIME", "DURATION" },
        ["GetIntValue"] = new() { "INT" },
        ["GetLongValue"] = new() { "INT", "LONG" },
        ["GetNumberValue"] = new() { "INT", "LONG", "NUMBER" },
        ["GetBooleanValue"] = new() { "BOOLEAN" },
        ["GetDateValue"] = new() { "DATE" },
        ["GetDateTimeValue"] = new() { "DATE_TIME" },
        ["GetDurationValue"] = new() { "DURATION" },
        ["GetObjectValue"] = new() { "OBJECT" },
        ["GetListValue"] = new() { "ARRAY" },
        ["GetBinaryValue"] = new() { "BINARY" },
    };

    public override ImmutableArray<DiagnosticDescriptor> SupportedDiagnostics { get; } = ImmutableArray.Create(
        Diagnostics.UndeclaredValue, Diagnostics.WrongGetter, Diagnostics.Convenience, Diagnostics.Reflection, Diagnostics.UndeclaredError);

    public override void Initialize(AnalysisContext context)
    {
        context.ConfigureGeneratedCodeAnalysis(GeneratedCodeAnalysisFlags.None);
        context.EnableConcurrentExecution();
        context.RegisterCompilationStartAction(start =>
        {
            if (start.Compilation.GetTypeByMetadataName(Ns + ".ToolPackageAttribute") == null) return;
            var options = ToolGateGenerator.Options.From(start.Options.AnalyzerConfigOptionsProvider.GlobalOptions);
            // Declaration problems are the generator's to report; with any of them there is nothing to check against.
            var model = PackageBuilder.Build(start.Compilation, options.Generation ?? PackageBuilder.Generation.ToString());
            var analysis = new Analysis(model == null || model.Failed ? null : model);
            start.RegisterOperationAction(analysis.Invocation, OperationKind.Invocation);
            start.RegisterOperationAction(analysis.TypeOf, OperationKind.TypeOf);
            start.RegisterOperationAction(analysis.Creation, OperationKind.ObjectCreation);
        });
    }

    private sealed class Analysis
    {
        private readonly PackageModel? model;

        public Analysis(PackageModel? model)
        {
            this.model = model;
        }

        private Facts? FactsOf(ISymbol? symbol)
        {
            if (model == null) return null;
            // Lambdas and local functions are part of the handler's own code.
            while (symbol is IMethodSymbol m && m.MethodKind is MethodKind.LambdaMethod or MethodKind.LocalFunction) symbol = m.ContainingSymbol;
            return symbol is IMethodSymbol method && model.Facts.TryGetValue(method.OriginalDefinition, out var f) ? f : null;
        }

        public void TypeOf(OperationAnalysisContext context)
        {
            var op = (ITypeOfOperation)context.Operation;
            string name = op.TypeOperand.OriginalDefinition.ToDisplayString();
            if (NetworkTypes.Contains(name))
            {
                Report(context, Diagnostics.Reflection, op.Syntax.GetLocation(),
                    "reflective access to " + op.TypeOperand.Name + " cannot be analysed; call the network API directly");
            }
        }

        public void Creation(OperationAnalysisContext context)
        {
            var op = (IObjectCreationOperation)context.Operation;
            string? type = op.Constructor?.ContainingType.ToDisplayString();
            if (type != Ns + ".Errors.ToolBusinessError" && type != Ns + ".Errors.ToolRetryableError") return;
            var facts = FactsOf(context.ContainingSymbol);
            if (facts == null || op.Arguments.Length == 0) return;
            var code = op.Arguments.FirstOrDefault(a => a.Parameter?.Name == "code")?.Value.ConstantValue;
            if (code is not { HasValue: true, Value: string c }) return;
            if (!facts.Errors.Contains(c))
            {
                Report(context, Diagnostics.UndeclaredError, op.Syntax.GetLocation(),
                    "tool " + facts.Tool + " does not declare error code " + c + "; add [ErrorDecl(\"" + c + "\", ...)]");
            }
        }

        public void Invocation(OperationAnalysisContext context)
        {
            var op = (IInvocationOperation)context.Operation;
            var method = op.TargetMethod;
            string owner = method.ContainingType.ToDisplayString();
            if (owner == Ns + ".IHttpClient" && ConvenienceMethods.Contains(method.Name) && op.Arguments.Length == 2)
            {
                Convenience(context, op, method.Name.ToUpperInvariant());
            }
            else if (Getters.ContainsKey(method.Name) && op.Arguments.Length > 0 && (owner == Ns + ".IValues" || owner == Ns + ".IJwt"))
            {
                Value(context, op, method.Name);
            }
        }

        private static string? Constant(IArgumentOperation argument) =>
            argument.Value.ConstantValue is { HasValue: true, Value: string s } ? s : null;

        private void Convenience(OperationAnalysisContext context, IInvocationOperation call, string method)
        {
            string? destination = Constant(call.Arguments[0]);
            string? template = Constant(call.Arguments[1]);
            var at = call.Syntax.GetLocation();
            if (destination == null || template == null)
            {
                Report(context, Diagnostics.Convenience, at, "a convenience call needs constant strings for the destination and the path template,"
                    + " so the route can be proven at build time; use Http.Route(id) instead");
                return;
            }
            var facts = FactsOf(context.ContainingSymbol);
            if (facts == null || model == null)
            {
                Report(context, Diagnostics.Convenience, at, "a convenience call outside a tool handler cannot be proven; make it in the handler or use Http.Route(id)");
                return;
            }
            string wanted = PackageBuilder.WithoutPatterns(template);
            var matches = model.Routes.Where(r => r.Method == method && r.Destination == destination && r.Template == wanted).ToList();
            if (matches.Count == 0)
            {
                string candidates = string.Join(", ", model.Routes.Where(r => r.Destination == destination)
                    .Select(r => r.Id + " (" + r.Method + " " + r.Template + ")"));
                Report(context, Diagnostics.Convenience, at, method + " " + destination + " " + template + " matches no declared route"
                    + (candidates.Length == 0 ? "; destination " + destination + " has no routes" : "; candidates: " + candidates));
            }
            else if (matches.Count > 1)
            {
                Report(context, Diagnostics.Convenience, at, method + " " + destination + " " + template + " matches more than one route: "
                    + string.Join(" and ", matches.Select(r => r.Id)));
            }
            else if (!facts.Routes.Contains(matches[0].Id))
            {
                Report(context, Diagnostics.Convenience, at, method + " " + destination + " " + template + " is route " + matches[0].Id
                    + ", which tool " + facts.Tool + " does not list in Routes");
            }
        }

        private void Value(OperationAnalysisContext context, IInvocationOperation call, string getter)
        {
            string? name = Constant(call.Arguments[0]);
            var facts = FactsOf(context.ContainingSymbol);
            if (name == null || facts == null) return;
            string kind;
            IDictionary<string, string> declared;
            var receiver = call.Instance;
            while (receiver is IConversionOperation conversion) receiver = conversion.Operand;
            if (receiver?.Type?.ToDisplayString() == Ns + ".ToolContext")
            {
                kind = "input";
                declared = facts.Inputs;
            }
            else if (receiver is IPropertyReferenceOperation p && p.Property.ContainingType.ToDisplayString() == Ns + ".ToolContext")
            {
                // Only a direct context.Global / Secret / JWT receiver says which values these are.
                switch (p.Property.Name)
                {
                    case "Global":
                        kind = "global";
                        declared = facts.Globals;
                        break;
                    case "Secret":
                        kind = "secret";
                        declared = facts.Secrets;
                        break;
                    case "JWT":
                        kind = "claim";
                        declared = facts.Claims;
                        break;
                    default:
                        return;
                }
            }
            else
            {
                return;
            }
            var at = call.Syntax.GetLocation();
            if (!declared.TryGetValue(name, out var valueType))
            {
                Report(context, Diagnostics.UndeclaredValue, at, "tool " + facts.Tool + " declares no " + kind + " " + name
                    + (declared.Count == 0 ? "" : "; declared: " + string.Join(", ", declared.Keys)));
            }
            else if (!Getters[getter].Contains(valueType))
            {
                Report(context, Diagnostics.WrongGetter, at, kind + " " + name + " of tool " + facts.Tool + " is " + valueType + "; " + getter + " does not read it");
            }
        }

        private static void Report(OperationAnalysisContext context, DiagnosticDescriptor descriptor, Location at, string message) =>
            context.ReportDiagnostic(Diagnostic.Create(descriptor, at, message));
    }
}
