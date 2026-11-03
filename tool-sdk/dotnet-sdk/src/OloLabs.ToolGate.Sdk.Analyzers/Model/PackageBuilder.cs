// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using Microsoft.CodeAnalysis;
using OloLabs.ToolGate.Shared;

namespace OloLabs.ToolGate.Sdk.Analyzers.Model;

/// <summary>
/// Turns the authoring attributes of one compilation into generation-1 semantic descriptors (tool-sdk/spec/descriptor),
/// with every declaration check of the Java processor. A port of DescriptorProcessor.Build; messages name the .NET
/// attributes instead of the Java annotations.
/// </summary>
internal sealed class PackageBuilder
{
    public const int Generation = 1;
    public const string GenerationProperty = "ToolGateDescriptorGeneration";

    internal static readonly Regex PackageId = new("^[a-z][a-z0-9-]{1,30}(\\.[a-z][a-z0-9-]{1,30}){1,3}$", RegexOptions.CultureInvariant);
    internal static readonly Regex LocalName = new("^[a-z][a-z0-9_-]{0,47}(\\.[a-z][a-z0-9_-]{0,47}){0,2}$", RegexOptions.CultureInvariant);
    internal static readonly Regex ToolId = new("^[a-zA-Z0-9][a-zA-Z0-9._:/-]{0,127}$", RegexOptions.CultureInvariant);
    internal static readonly Regex McpName = new("^[A-Za-z0-9_.-]{1,128}$", RegexOptions.CultureInvariant);
    internal static readonly Regex Variable = new("^[A-Za-z][A-Za-z0-9_]{0,63}$", RegexOptions.CultureInvariant);
    internal static readonly Regex DeclarationId = new("^[a-z][a-z0-9_.-]{0,63}$", RegexOptions.CultureInvariant);
    internal static readonly Regex ErrorCode = new("^[A-Z][A-Z0-9_]{2,63}$", RegexOptions.CultureInvariant);
    internal static readonly Regex Header = new("^[A-Za-z0-9-]{1,128}$", RegexOptions.CultureInvariant);
    private static readonly string[] Reserved = { "toolgate.", "hotfolder.", "builtin." };
    private static readonly HashSet<string> ForbiddenHeaders = new() { "authorization", "proxy-authorization", "cookie", "host" };
    private static readonly Regex TemplateVariable = new("\\{([A-Za-z][A-Za-z0-9_]*)(:[^{}]*(\\{[^{}]*\\}[^{}]*)*)?\\}", RegexOptions.CultureInvariant);

    internal const string Ns = "OloLabs.ToolGate.Sdk";

    private readonly Compilation compilation;
    private readonly string? generation;
    private readonly PackageModel model = new();

    private readonly SortedDictionary<string, Attr> destinations = new(StringComparer.Ordinal);
    private readonly SortedDictionary<string, Attr> auths = new(StringComparer.Ordinal);
    private readonly SortedDictionary<string, Attr> routes = new(StringComparer.Ordinal);
    private readonly SortedDictionary<string, Attr> tcps = new(StringComparer.Ordinal);
    private readonly SortedDictionary<string, IMethodSymbol> tools = new(StringComparer.Ordinal);
    private readonly Dictionary<string, string> reconcilers = new();
    private readonly Dictionary<string, List<string>> parametersByTool = new();
    private readonly Dictionary<string, SortedDictionary<string, object?>> dataSchemasByTool = new();
    private readonly Dictionary<string, List<object?>> examplesByTool = new();
    private readonly Dictionary<string, ToolModel> toolModels = new();
    private Attr pkg = null!;
    private string pkgId = "";

    private PackageBuilder(Compilation compilation, string? generation)
    {
        this.compilation = compilation;
        this.generation = generation;
    }

    /// <summary>Builds the package of a compilation, or returns null when it declares nothing.</summary>
    public static PackageModel? Build(Compilation compilation, string? generation)
    {
        var builder = new PackageBuilder(compilation, generation);
        return builder.Run() ? builder.model : null;
    }

    private void Error(Problem p)
    {
        model.Failed = true;
        model.Problems.Add(p);
    }

    private void Guard(Action step)
    {
        try
        {
            step();
        }
        catch (Problem p)
        {
            Error(p);
        }
    }

    private static bool Full(Regex r, string s)
    {
        var m = r.Match(s);
        return m.Success && m.Index == 0 && m.Length == s.Length;
    }

    private static Location Loc(ISymbol s) => s.Locations.FirstOrDefault(l => l.IsInSource) ?? Location.None;

    private List<Attr> AssemblyAttributes(string name) =>
        compilation.Assembly.GetAttributes().Where(a => IsSdk(a, name)).Select(a => new Attr(a, Location.None)).ToList();

    private static bool IsSdk(AttributeData a, string name) =>
        a.AttributeClass?.Name == name && a.AttributeClass.ContainingNamespace?.ToDisplayString() == Ns;

    private static List<Attr> Attributes(ISymbol s, string name) =>
        s.GetAttributes().Where(a => IsSdk(a, name)).Select(a => new Attr(a, Loc(s))).ToList();

    private static Attr? Attribute(ISymbol s, string name) => Attributes(s, name).FirstOrDefault();

    private IEnumerable<IMethodSymbol> SourceMethods()
    {
        var stack = new Stack<INamespaceOrTypeSymbol>();
        stack.Push(compilation.SourceModule.GlobalNamespace);
        var methods = new List<IMethodSymbol>();
        while (stack.Count > 0)
        {
            var current = stack.Pop();
            foreach (var member in current.GetMembers())
            {
                if (member is INamespaceSymbol ns) stack.Push(ns);
                else if (member is INamedTypeSymbol t)
                {
                    if (!t.Locations.Any(l => l.IsInSource)) continue;
                    stack.Push(t);
                }
                else if (member is IMethodSymbol m && m.MethodKind == MethodKind.Ordinary) methods.Add(m);
            }
        }
        // Source order, so diagnostics are stable.
        return methods.OrderBy(m => Loc(m).SourceTree?.FilePath, StringComparer.Ordinal).ThenBy(m => Loc(m).SourceSpan.Start);
    }

    private bool Run()
    {
        var packages = AssemblyAttributes("ToolPackageAttribute");
        var handlers = SourceMethods().Where(m => Attribute(m, "ServerToolAttribute") != null || Attribute(m, "ClientToolAttribute") != null).ToList();
        var network = AssemblyAttributes("DestinationAttribute").Concat(AssemblyAttributes("HttpAuthAttribute"))
            .Concat(AssemblyAttributes("HttpRouteAttribute")).Concat(AssemblyAttributes("TcpDestinationAttribute")).ToList();
        if (packages.Count == 0 && handlers.Count == 0 && network.Count == 0) return false;
        Location anchor = packages.Count > 0 ? packages[0].Location : handlers.Count > 0 ? Loc(handlers[0]) : network[0].Location;
        if (generation == null || generation.Trim().Length == 0)
        {
            Error(new Problem(anchor, "the build must set <" + GenerationProperty + "> in the project file; it has no default", Diagnostics.GenerationId));
            return true;
        }
        if (generation.Trim() != Generation.ToString())
        {
            Error(new Problem(anchor, GenerationProperty + "=" + generation + " is not supported; this SDK emits generation " + Generation, Diagnostics.GenerationId));
            return true;
        }
        try
        {
            PackageDeclaration(packages, handlers);
        }
        catch (Problem p)
        {
            Error(p);
            return true;
        }
        Network();
        foreach (var h in handlers) Guard(() => Register(h));
        foreach (var r in SourceMethods().Where(m => Attribute(m, "ReconcileAttribute") != null)) Guard(() => Reconciler(r));
        if (model.Failed) return true;
        var descriptors = new SortedDictionary<string, object?>(StringComparer.Ordinal);
        foreach (var t in tools) Guard(() => descriptors[t.Key] = Descriptor(t.Key, t.Value));
        if (model.Failed) return true;
        foreach (var r in routes.Values)
        {
            model.Routes.Add(new RouteShape(r.CtorString(0), r.Enum("Method", "GET"), r.String("Destination"), WithoutPatterns(r.String("Path"))));
        }
        Accessors();
        if (model.Failed) return true;
        Guard(() => Write(descriptors));
        return true;
    }

    internal static string WithoutPatterns(string template) =>
        TemplateVariable.Replace(template, m => "{" + m.Groups[1].Value + "}");

    private void PackageDeclaration(List<Attr> declared, List<IMethodSymbol> handlers)
    {
        if (declared.Count != 1)
        {
            Location at = declared.Count == 0 ? handlers.Select(Loc).FirstOrDefault() ?? Location.None : declared[1].Location;
            throw new Problem(at, "exactly one [assembly: ToolPackage] is required per package; found " + declared.Count);
        }
        pkg = declared[0];
        pkgId = pkg.CtorString(0);
        if (!Full(PackageId, pkgId)) throw new Problem(pkg.Location, "package id " + pkgId + " does not match " + PackageId);
        ReservedPrefix(pkgId, "package id", pkg.Location);
        if (pkg.String("Name").Trim().Length == 0) throw new Problem(pkg.Location, "package Name is required");
        if (pkg.String("Publisher").Trim().Length == 0) throw new Problem(pkg.Location, "package Publisher is required");
    }

    private void Network()
    {
        foreach (var d in AssemblyAttributes("DestinationAttribute")) Put(destinations, d, "destination");
        foreach (var a in AssemblyAttributes("HttpAuthAttribute")) Put(auths, a, "auth");
        foreach (var r in AssemblyAttributes("HttpRouteAttribute")) Put(routes, r, "route");
        foreach (var t in AssemblyAttributes("TcpDestinationAttribute")) Put(tcps, t, "TCP destination");
        foreach (var r in routes.Values) Guard(() => Route(r));
        foreach (var d in destinations.Values)
        {
            Guard(() =>
            {
                CheckVariable(d.String("HostSetting"), "HostSetting of destination " + d.CtorString(0), d.Location);
                Port(d.Int("Port", 443), d.Location);
            });
        }
        foreach (var d in tcps.Values)
        {
            Guard(() =>
            {
                CheckVariable(d.String("HostSetting"), "HostSetting of TCP destination " + d.CtorString(0), d.Location);
                Port(d.Int("Port", 0), d.Location);
            });
        }
        foreach (var a in auths.Values)
        {
            Guard(() =>
            {
                string id = a.CtorString(0);
                string header = a.String("Header");
                if (header.Length > 0 && !Full(Header, header)) throw new Problem(a.Location, "auth " + id + " Header is not a header name");
                if (a.String("Secret").Length > 0) CheckVariable(a.String("Secret"), "Secret of auth " + id, a.Location);
                if (a.String("Connection").Length > 0) CheckDeclarationId(a.String("Connection"), "connection", a.Location);
            });
        }
    }

    private void Put(SortedDictionary<string, Attr> map, Attr value, string kind)
    {
        Guard(() =>
        {
            string id = value.CtorString(0);
            CheckDeclarationId(id, kind + " id", value.Location);
            if (map.ContainsKey(id)) throw new Problem(value.Location, kind + " id " + id + " is declared twice in the package");
            map[id] = value;
        });
    }

    private void Route(Attr r)
    {
        string id = r.CtorString(0);
        string destination = r.String("Destination");
        string auth = r.String("Auth");
        if (!destinations.ContainsKey(destination)) throw new Problem(r.Location, "route " + id + " names undeclared destination " + destination);
        if (auth.Length > 0 && !auths.ContainsKey(auth)) throw new Problem(r.Location, "route " + id + " names undeclared auth " + auth);
        PathTemplate(id, r.String("Path"), r.Location);
        string authHeader = auth.Length == 0 ? "" : auths[auth].String("Header").ToLowerInvariant();
        foreach (string h in r.Strings("Headers"))
        {
            string lower = h.ToLowerInvariant();
            if (!Full(Header, h)) throw new Problem(r.Location, "route " + id + " header " + h + " is not a header name");
            if (ForbiddenHeaders.Contains(lower) || lower.StartsWith("x-forwarded-", StringComparison.Ordinal) || lower == authHeader)
            {
                throw new Problem(r.Location, "route " + id + " may not allow header " + h);
            }
        }
        Unique(r.Strings("Query"), "query name", r.Location);
        Unique(r.Strings("Headers"), "header", r.Location);
        Unique(r.Strings("RequestContentTypes", "application/json"), "content type", r.Location);
        Range(r.Long("MaxRequestBytes", 1_048_576), 0, 1L << 30, "MaxRequestBytes", r.Location);
        Range(r.Long("MaxResponseBytes", 4_194_304), 0, 1L << 30, "MaxResponseBytes", r.Location);
        Range(r.Int("TimeoutMs", 10_000), 1, 900_000, "TimeoutMs", r.Location);
    }

    /// <summary>RFC 6570 level 1 with {name:regex}: every variable needs a pattern, and the patterns .* and .+ are refused.</summary>
    private static void PathTemplate(string id, string path, Location at)
    {
        if (!path.StartsWith("/", StringComparison.Ordinal)) throw new Problem(at, "route " + id + " path must start with /");
        var names = new HashSet<string>();
        int i = 0;
        while (i < path.Length)
        {
            char c = path[i];
            if (c == '}') throw new Problem(at, "route " + id + " path has an unmatched }");
            if (c != '{')
            {
                i++;
                continue;
            }
            int depth = 1;
            int j = i + 1;
            while (j < path.Length && depth > 0)
            {
                if (path[j] == '{') depth++;
                if (path[j] == '}') depth--;
                j++;
            }
            if (depth != 0) throw new Problem(at, "route " + id + " path has an unmatched {");
            string body = path.Substring(i + 1, j - 1 - (i + 1));
            int colon = body.IndexOf(':');
            string name = colon < 0 ? body : body.Substring(0, colon);
            CheckVariable(name, "path variable of route " + id, at);
            if (!names.Add(name)) throw new Problem(at, "route " + id + " repeats path variable " + name);
            if (colon < 0) throw new Problem(at, "route " + id + " path variable " + name + " needs a pattern, {" + name + ":regex}");
            string regex = body.Substring(colon + 1);
            if (regex == ".*" || regex == ".+") throw new Problem(at, "route " + id + " path variable " + name + " may not use " + regex);
            if (!Schemas.Compiles(regex, out _)) throw new Problem(at, "route " + id + " path variable " + name + " pattern does not compile");
            i = j;
        }
    }

    private void Register(IMethodSymbol method)
    {
        var server = Attribute(method, "ServerToolAttribute");
        var client = Attribute(method, "ClientToolAttribute");
        if (server != null && client != null) throw new Problem(Loc(method), "a handler is either [ServerTool] or [ClientTool]");
        string name = (server ?? client)!.CtorString(0);
        if (!Full(LocalName, name)) throw new Problem(Loc(method), "tool name " + name + " does not match " + LocalName);
        ReservedPrefix(name, "tool name", Loc(method));
        if (tools.ContainsKey(name)) throw new Problem(Loc(method), "tool " + name + " is declared twice");
        tools[name] = method;
        HandlerShape(method);
    }

    private void Reconciler(IMethodSymbol method)
    {
        var r = Attribute(method, "ReconcileAttribute")!;
        string target = r.CtorString(0);
        string? self = ToolName(method);
        if (self == null) throw new Problem(Loc(method), "a [Reconcile] handler must also be a [ServerTool] or [ClientTool]");
        if (!tools.ContainsKey(target)) throw new Problem(Loc(method), "[Reconcile] names unknown tool " + target);
        if (target == self) throw new Problem(Loc(method), "a tool cannot reconcile itself");
        if (reconcilers.ContainsKey(target)) throw new Problem(Loc(method), "tool " + target + " has two [Reconcile] handlers");
        reconcilers[target] = self;
    }

    private static string? ToolName(IMethodSymbol method) =>
        (Attribute(method, "ServerToolAttribute") ?? Attribute(method, "ClientToolAttribute"))?.CtorString(0);

    private static void HandlerShape(IMethodSymbol method)
    {
        if (method.DeclaredAccessibility != Accessibility.Public || method.IsStatic || method.IsGenericMethod)
        {
            throw new Problem(Loc(method), "a handler must be a public, non-generic instance method");
        }
        for (var t = method.ContainingType; t != null; t = t.ContainingType)
        {
            if (t.DeclaredAccessibility != Accessibility.Public || t.TypeKind != TypeKind.Class || t.IsAbstract || t.IsStatic
                || t.IsGenericType)
            {
                throw new Problem(Loc(method), "a handler must live in a public, concrete, non-generic class, nested only in public classes");
            }
        }
    }

    private static bool HasParameterlessConstructor(INamedTypeSymbol type) =>
        type.InstanceConstructors.Any(c => c.Parameters.Length == 0 && c.DeclaredAccessibility == Accessibility.Public);

    private Dictionary<string, object?> Descriptor(string name, IMethodSymbol method)
    {
        var server = Attribute(method, "ServerToolAttribute");
        var client = Attribute(method, "ClientToolAttribute");
        var t = (server ?? client)!;
        bool isClient = client != null;
        var owner = method.ContainingType;
        Location at = Loc(method);

        string toolId = pkgId + "/" + name;
        if (toolId.Length > 128 || !Full(ToolId, toolId)) throw new Problem(at, "toolId " + toolId + " is longer than 128 characters or not a valid id");
        string alias = pkgId.Substring(pkgId.LastIndexOf('.') + 1);
        string mcpName = alias + "." + name;
        if (!Full(McpName, mcpName)) throw new Problem(at, "mcpName " + mcpName + " is longer than 128 characters");

        string titleValue = t.String("Title");
        string text = t.String("Text");
        if ((titleValue.Length == 0) == (text.Length == 0)) throw new Problem(at, "tool " + name + " must set exactly one of Title and Text");
        string title = titleValue.Length == 0 ? text : titleValue;
        Length(title, 1, 100, "Title", at);
        string description = t.String("Description");
        Length(description, 1, 2048, "Description", at);
        string useWhen = t.String("UseWhen");
        if (useWhen.Length > 512) throw new Problem(at, "UseWhen is longer than 512 characters");

        if (isClient && t.Strings("Routes").Count > 0)
        {
            throw new Problem(at, "client tool " + name + " cannot list Routes; client tools have no network access", Diagnostics.ClientToolId);
        }
        if (isClient && t.Strings("Tcp").Count > 0)
        {
            throw new Problem(at, "client tool " + name + " cannot list Tcp; client tools have no network access", Diagnostics.ClientToolId);
        }

        bool grouped = Attribute(method, "ToolVariablesAttribute") != null;
        var schemas = new Schemas(compilation);
        var properties = new Dictionary<string, object?>();
        var required = new List<string>();
        var parameters = new List<string>();
        var bindings = new List<Binding>();
        Inputs(method, grouped, schemas, properties, required, parameters, bindings);

        var d = new Dictionary<string, object?>
        {
            ["descriptorGeneration"] = (long)Generation,
            ["toolId"] = toolId,
            ["mcpName"] = mcpName,
            ["kind"] = isClient ? "CLIENT" : "SERVER",
            ["title"] = title,
            ["description"] = description,
        };
        if (useWhen.Length > 0) d["useWhen"] = useWhen;
        d["inputSchema"] = schemas.Inputs(properties, required);
        var tool = new ToolModel { Name = name, Method = method, Bindings = bindings, HasFactory = HasParameterlessConstructor(owner) };
        var output = Output(method, tool);
        if (output != null) d["outputSchema"] = output;
        var errorDecls = Attributes(method, "ErrorDeclAttribute");
        d["errors"] = Errors(errorDecls, at);

        string effect = t.Enum("Effect", "UNKNOWN");
        bool destructive = t.Bool("Destructive");
        var routeIdsDeclared = t.Strings("Routes");
        string openWorld = t.Enum("OpenWorld", "DEFAULT");
        d["effect"] = new Dictionary<string, object?>
        {
            ["safety"] = effect,
            ["destructive"] = destructive,
            ["openWorld"] = openWorld switch
            {
                "TRUE" => true,
                "FALSE" => false,
                _ => !isClient && routeIdsDeclared.Count > 0,
            },
        };
        d["businessKey"] = BusinessKey(t, method, effect, destructive, properties.Keys);
        d["execution"] = Execution(t, at);
        if (reconcilers.TryGetValue(name, out var reconciler)) d["reconcileTool"] = reconciler;
        if (isClient) d["tiers"] = Tiers(client!.Enums("Tiers", "OCI_SANDBOX"), at);

        var routeIds = new SortedSet<string>(Unique(routeIdsDeclared, "route", at), StringComparer.Ordinal);
        var tcpIds = new SortedSet<string>(Unique(t.Strings("Tcp"), "TCP destination", at), StringComparer.Ordinal);
        var routeList = new List<object?>();
        var destinationIds = new SortedSet<string>(StringComparer.Ordinal);
        var authIds = new SortedSet<string>(StringComparer.Ordinal);
        foreach (string id in routeIds)
        {
            if (!routes.TryGetValue(id, out var r)) throw new Problem(at, "tool " + name + " lists undeclared route " + id);
            routeList.Add(RouteJson(r));
            destinationIds.Add(r.String("Destination"));
            if (r.String("Auth").Length > 0) authIds.Add(r.String("Auth"));
        }
        d["routes"] = routeList;
        d["destinations"] = destinationIds.Select(id => (object?)DestinationJson(destinations[id])).ToList();
        d["httpAuth"] = authIds.Select(id => (object?)AuthJson(auths[id])).ToList();
        var tcpList = new List<object?>();
        foreach (string id in tcpIds)
        {
            if (!tcps.TryGetValue(id, out var tcp)) throw new Problem(at, "tool " + name + " lists undeclared TCP destination " + id);
            tcpList.Add(new Dictionary<string, object?>
            {
                ["id"] = id,
                ["hostSetting"] = tcp.String("HostSetting"),
                ["port"] = (long)tcp.Int("Port", 0),
            });
        }
        d["tcp"] = tcpList;

        var vars = Variables(method, owner, properties.Keys, isClient);
        foreach (string authId in authIds)
        {
            string secret = auths[authId].String("Secret");
            if (secret.Length > 0 && vars.Secrets.All(s => s.CtorString(0) != secret))
            {
                throw new Problem(at, "auth " + authId + " uses secret " + secret + ", which tool " + name + " does not declare");
            }
        }
        d["variables"] = vars.ToJson();

        var permissions = new SortedSet<string>(StringComparer.Ordinal);
        if (routeIds.Count > 0) permissions.Add("HTTP_ROUTE");
        if (tcpIds.Count > 0)
        {
            permissions.Add("TCP");
            permissions.Add("CREDENTIALS_IN_SANDBOX");
        }
        foreach (string authId in authIds)
        {
            string type = auths[authId].CtorEnum(1, "");
            if (type == "CUSTOM") permissions.Add("CREDENTIALS_IN_SANDBOX");
            if (type == "TOKEN_EXCHANGE") permissions.Add("TOKEN_EXCHANGE");
            if (type == "CONNECTION") permissions.Add("CONNECTION");
        }
        if (vars.Secrets.Any(s => s.Enum("Scope", "GROUP") == "USER")) permissions.Add("USER_SECRET");
        d["permissions"] = Jcs.Sorted(permissions).Cast<object?>().ToList();

        var toolsets = t.Strings("Toolsets");
        foreach (string set in toolsets) Length(set, 1, 64, "toolset", at);
        d["toolsets"] = Jcs.Sorted(Unique(toolsets, "toolset", at)).Cast<object?>().ToList();

        var examples = Attributes(method, "ExampleAttribute");
        if (examples.Count > 8) throw new Problem(at, "tool " + name + " declares " + examples.Count + " examples; at most 8 are allowed");
        var exampleEntries = new List<object?>();
        foreach (var e in examples)
        {
            string arguments = e.CtorString(0);
            if (arguments.Trim().Length == 0) throw new Problem(e.Location, "an example needs arguments");
            string expectedOutput = e.String("ExpectedOutput");
            string expectedError = e.String("ExpectedError");
            if (expectedOutput.Length > 0 && expectedError.Length > 0) throw new Problem(e.Location, "an example expects either an output or an error");
            // Examples are not part of the descriptor; the index carries them for check and ToolHarness.
            var entry = new Dictionary<string, object?> { ["arguments"] = arguments };
            if (expectedOutput.Length > 0) entry["expectedOutput"] = expectedOutput;
            if (expectedError.Length > 0) entry["expectedError"] = expectedError;
            if (e.String("Exchanges").Length > 0) entry["exchanges"] = e.String("Exchanges");
            exampleEntries.Add(entry);
        }
        examplesByTool[name] = exampleEntries;
        parametersByTool[name] = parameters;

        var facts = new Facts { Tool = name, Routes = new HashSet<string>(routeIds) };
        foreach (var p in properties)
        {
            if (p.Value is Dictionary<string, object?> schema) facts.Inputs[p.Key] = ValueType(schema);
        }
        foreach (var g in vars.Globals) if (!facts.Globals.ContainsKey(g.CtorString(0))) facts.Globals[g.CtorString(0)] = g.CtorEnum(1, "INFER");
        foreach (var s in vars.Secrets) if (!facts.Secrets.ContainsKey(s.CtorString(0))) facts.Secrets[s.CtorString(0)] = s.Enum("Type", "STRING");
        foreach (var c in vars.Claims) if (!facts.Claims.ContainsKey(c.CtorString(0))) facts.Claims[c.CtorString(0)] = c.Enum("Type", "STRING");
        foreach (var e in errorDecls) facts.Errors.Add(e.CtorString(0));
        model.Facts[method] = facts;

        var dataSchemas = new SortedDictionary<string, object?>(StringComparer.Ordinal);
        foreach (var e in errorDecls) if (e.String("DataSchema").Length > 0) dataSchemas[e.CtorString(0)] = e.String("DataSchema");
        dataSchemasByTool[name] = dataSchemas;
        toolModels[name] = tool;
        return d;
    }

    private void Inputs(IMethodSymbol method, bool grouped, Schemas schemas, Dictionary<string, object?> properties,
        List<string> required, List<string> parameters, List<Binding> bindings)
    {
        int contexts = 0;
        int tokens = 0;
        var variables = Attributes(method, "VariableAttribute");
        if (variables.Count > 0 && !grouped) throw new Problem(Loc(method), "[Variable] inputs need [ToolVariables] on the handler");
        bool groupedInputs = grouped && variables.Count > 0;
        foreach (var p in method.Parameters)
        {
            Location pat = Loc(p);
            if (pat == Location.None) pat = Loc(method);
            if (p.RefKind != RefKind.None) throw new Problem(pat, "parameter " + p.Name + " cannot be ref, out or in");
            string typeName = p.Type.ToDisplayString();
            if (typeName == Ns + ".ToolContext")
            {
                contexts++;
                parameters.Add("$context");
                bindings.Add(new Binding("$context", ""));
                continue;
            }
            if (typeName == "System.Threading.CancellationToken")
            {
                tokens++;
                parameters.Add("$cancellation");
                bindings.Add(new Binding("$cancellation", ""));
                continue;
            }
            if (groupedInputs) throw new Problem(pat, "with [ToolVariables] inputs the handler takes only ToolContext");
            var param = Schemas.AttributeOf(p.GetAttributes(), "ParamAttribute", pat);
            if (param == null) throw new Problem(pat, "parameter " + p.Name + " needs [Param]");
            var spec = Spec.OfParam(param);
            string name = spec.Name.Length == 0 ? p.Name : spec.Name;
            Input(name, spec, p.Type, schemas, pat, properties, required, Schemas.Optional(p.Type, p.NullableAnnotation), p.Name);
            parameters.Add(name);
            bindings.Add(new Binding(name, p.Type.ToDisplayString(TypeFormat)));
        }
        if (contexts > 1) throw new Problem(Loc(method), "a handler takes at most one ToolContext");
        if (tokens > 1) throw new Problem(Loc(method), "a handler takes at most one CancellationToken");
        if (groupedInputs)
        {
            foreach (var v in variables)
            {
                var spec = Spec.OfVariable(v);
                if (spec.Type is "INFER" or "OBJECT" or "ARRAY")
                {
                    throw new Problem(v.Location, "grouped input " + spec.Name + " needs a scalar type; use the typed form for objects and arrays");
                }
                Input(spec.Name, spec, ScalarType(spec.Type), schemas, v.Location, properties, required, false, spec.Name);
            }
        }
    }

    internal static readonly SymbolDisplayFormat TypeFormat = SymbolDisplayFormat.FullyQualifiedFormat
        .AddMiscellaneousOptions(SymbolDisplayMiscellaneousOptions.IncludeNullableReferenceTypeModifier);

    private ITypeSymbol ScalarType(string type) => type switch
    {
        "INT" => compilation.GetSpecialType(SpecialType.System_Int32),
        "LONG" => compilation.GetSpecialType(SpecialType.System_Int64),
        "NUMBER" => compilation.GetSpecialType(SpecialType.System_Double),
        "BOOLEAN" => compilation.GetSpecialType(SpecialType.System_Boolean),
        "BINARY" => compilation.CreateArrayTypeSymbol(compilation.GetSpecialType(SpecialType.System_Byte)),
        _ => compilation.GetSpecialType(SpecialType.System_String),
    };

    private static void Input(string name, Spec spec, ITypeSymbol type, Schemas schemas, Location at,
        Dictionary<string, object?> properties, List<string> required, bool optional, string elementName)
    {
        CheckVariable(name, "input name", at);
        if (spec.Description.Length == 0) throw new Problem(at, "input " + name + " needs a Description");
        if (spec.Description.Length > 1024) throw new Problem(at, "input " + name + " Description is longer than 1024 characters");
        var schema = schemas.Schema(type, spec, at, elementName);
        if (properties.ContainsKey(name)) throw new Problem(at, "input " + name + " is declared twice");
        properties[name] = schema;
        if (spec.Required && spec.DefaultValue.Length == 0 && !optional) required.Add(name);
    }

    private Dictionary<string, object?>? Output(IMethodSymbol method, ToolModel tool)
    {
        var type = method.ReturnType;
        Location at = Loc(method);
        if (method.ReturnsVoid) throw new Problem(at, "a handler returns a record, a class, a dictionary or ToolResult");
        tool.Shape = ReturnShape.Value;
        if (type is INamedTypeSymbol named)
        {
            string definition = named.OriginalDefinition.ToDisplayString();
            if (definition is "System.Threading.Tasks.Task" or "System.Threading.Tasks.ValueTask")
            {
                throw new Problem(at, "a handler returns a record, a class, a dictionary or ToolResult; " + definition + " has no result");
            }
            if (definition == "System.Threading.Tasks.Task<TResult>")
            {
                tool.Shape = ReturnShape.Task;
                type = named.TypeArguments[0];
            }
            else if (definition == "System.Threading.Tasks.ValueTask<TResult>")
            {
                tool.Shape = ReturnShape.ValueTask;
                type = named.TypeArguments[0];
            }
        }
        tool.ResultTypeName = type.ToDisplayString(TypeFormat);
        if (type.ToDisplayString() == Ns + ".ToolResult") return null;
        return new Schemas(compilation).Output(type, at);
    }

    private static List<object?> Errors(List<Attr> errors, Location at)
    {
        var byCode = new SortedDictionary<string, object?>(StringComparer.Ordinal);
        foreach (var e in errors)
        {
            string code = e.CtorString(0);
            if (!Full(ErrorCode, code)) throw new Problem(e.Location, "error code " + code + " does not match " + ErrorCode);
            Length(e.CtorString(1), 1, 1024, "error description", e.Location);
            if (byCode.ContainsKey(code)) throw new Problem(e.Location, "error code " + code + " is declared twice");
            byCode[code] = new Dictionary<string, object?>
            {
                ["code"] = code,
                ["description"] = e.CtorString(1),
                ["retryable"] = e.Bool("Retryable"),
            };
        }
        return byCode.Values.ToList();
    }

    private static Dictionary<string, object?> BusinessKey(Attr t, IMethodSymbol method, string effect, bool destructive, IEnumerable<string> inputs)
    {
        var m = new Dictionary<string, object?>
        {
            ["required"] = t.Enum("RequiresBusinessKey", "DEFAULT") switch
            {
                "REQUIRED" => true,
                "NOT_REQUIRED" => false,
                _ => effect == "NON_IDEMPOTENT" && destructive,
            },
            ["namespace"] = t.Enum("BusinessKeyNamespace", "AGENT"),
        };
        string argument = t.String("BusinessKeyArgument");
        if (argument.Length > 0)
        {
            if (!inputs.Contains(argument)) throw new Problem(Loc(method), "BusinessKeyArgument " + argument + " is not an input of the tool");
            m["argument"] = argument;
        }
        var idem = Attribute(method, "IdempotencyAttribute");
        if (idem != null)
        {
            string header = idem.CtorString(0);
            if (!Full(Header, header)) throw new Problem(idem.Location, "[Idempotency] header is not a header name");
            m["idempotencyHeader"] = header;
            m["idempotencyScope"] = idem.Enum("Scope", "BUSINESS_KEY");
        }
        return m;
    }

    private static Dictionary<string, object?> Execution(Attr t, Location at)
    {
        bool isAsync = t.Bool("Async");
        long ceiling = isAsync ? 900_000 : 60_000;
        long declared = t.Long("MaxDurationMs", -1);
        long duration = declared == -1 ? (isAsync ? 900_000 : 30_000) : declared;
        if (duration < 1 || duration > ceiling)
        {
            throw new Problem(at, "MaxDurationMs must be 1.." + ceiling + (isAsync ? " for async tools" : " for sync tools; make the tool async for longer work"));
        }
        bool cancelIfAbandoned = t.Bool("CancelIfAbandoned");
        if (cancelIfAbandoned && !isAsync) throw new Problem(at, "CancelIfAbandoned applies to async tools only");
        var m = new Dictionary<string, object?>
        {
            ["async"] = isAsync,
            ["maxDurationMs"] = duration,
            ["stateless"] = t.Bool("Stateless"),
        };
        int maxConcurrency = t.Int("MaxConcurrency", -1);
        if (maxConcurrency != -1)
        {
            Range(maxConcurrency, 1, 1_048_576, "MaxConcurrency", at);
            m["maxConcurrency"] = (long)maxConcurrency;
        }
        m["cancelIfAbandoned"] = cancelIfAbandoned;
        return m;
    }

    private static List<object?> Tiers(List<string> tiers, Location at)
    {
        if (tiers.Count == 0) throw new Problem(at, "a client tool needs at least one tier");
        return Jcs.Sorted(Unique(tiers, "tier", at)).Cast<object?>().ToList();
    }

    private static Dictionary<string, object?> RouteJson(Attr r)
    {
        var m = new Dictionary<string, object?>
        {
            ["id"] = r.CtorString(0),
            ["destination"] = r.String("Destination"),
            ["method"] = r.Enum("Method", "GET"),
            ["path"] = r.String("Path"),
            ["query"] = Jcs.Sorted(r.Strings("Query")).Cast<object?>().ToList(),
            ["headers"] = Jcs.Sorted(r.Strings("Headers")).Cast<object?>().ToList(),
            ["requestContentTypes"] = Jcs.Sorted(r.Strings("RequestContentTypes", "application/json")).Cast<object?>().ToList(),
            ["maxRequestBytes"] = r.Long("MaxRequestBytes", 1_048_576),
            ["maxResponseBytes"] = r.Long("MaxResponseBytes", 4_194_304),
            ["streaming"] = r.Enum("Streaming", "NONE"),
            ["timeoutMs"] = (long)r.Int("TimeoutMs", 10_000),
        };
        if (r.String("Auth").Length > 0) m["auth"] = r.String("Auth");
        return m;
    }

    private static Dictionary<string, object?> DestinationJson(Attr d)
    {
        var m = new Dictionary<string, object?>
        {
            ["id"] = d.CtorString(0),
            ["hostSetting"] = d.String("HostSetting"),
            ["port"] = (long)d.Int("Port", 443),
            ["spkiPins"] = d.Strings("SpkiPins").Cast<object?>().ToList(),
        };
        if (d.String("CaBundle").Length > 0) m["caBundle"] = d.String("CaBundle");
        return m;
    }

    private static Dictionary<string, object?> AuthJson(Attr a)
    {
        var m = new Dictionary<string, object?>
        {
            ["id"] = a.CtorString(0),
            ["type"] = a.CtorEnum(1, ""),
        };
        if (a.String("Header").Length > 0) m["header"] = a.String("Header");
        if (a.String("Secret").Length > 0) m["secret"] = a.String("Secret");
        if (a.String("Connection").Length > 0) m["connection"] = a.String("Connection");
        m["scopes"] = a.Strings("Scopes").Cast<object?>().ToList();
        if (a.String("Audience").Length > 0) m["audience"] = a.String("Audience");
        return m;
    }

    /// <summary>Globals, secrets and claims in scope: the handler's class, then the handler.</summary>
    private Vars Variables(IMethodSymbol method, INamedTypeSymbol owner, IEnumerable<string> inputs, bool isClient)
    {
        var scope = new Vars();
        scope.Add(Attributes(owner, "GlobalAttribute"), Attributes(owner, "SecretAttribute"), Attributes(owner, "JwtClaimAttribute"));
        var tool = new Vars();
        tool.Add(Attributes(method, "GlobalAttribute"), Attributes(method, "SecretAttribute"), Attributes(method, "JwtClaimAttribute"));
        foreach (var n in tool.Names)
        {
            if (scope.Names.TryGetValue(n.Key, out var prior))
            {
                throw new Problem(Loc(method), "tool-level " + n.Value + " cannot override the declaration " + prior + " of its class");
            }
        }
        var all = new Vars();
        all.Merge(scope);
        all.Merge(tool);
        var seen = new Dictionary<string, string>(all.Names);
        foreach (string input in inputs)
        {
            string key = input.ToLowerInvariant();
            if (seen.TryGetValue(key, out var prior)) throw new Problem(Loc(method), "input " + input + " and " + prior + " have the same name, ignoring case");
            seen[key] = "input " + input;
        }
        if (isClient && all.Secrets.Count > 0)
        {
            throw new Problem(Loc(method), "client tool cannot use secrets; found " + all.Secrets[0].CtorString(0), Diagnostics.ClientToolId);
        }
        foreach (var g in all.Globals) GlobalConstraints(g);
        return all;
    }

    /// <summary>Scope-level or tool-level globals, secrets and claims, with case-insensitive name tracking.</summary>
    private sealed class Vars
    {
        public readonly List<Attr> Globals = new();
        public readonly List<Attr> Secrets = new();
        public readonly List<Attr> Claims = new();
        public readonly Dictionary<string, string> Names = new();

        public void Add(List<Attr> gs, List<Attr> ss, List<Attr> cs)
        {
            foreach (var g in gs)
            {
                Name(g.CtorString(0), "global " + g.CtorString(0), g.Location);
                Globals.Add(g);
            }
            foreach (var s in ss)
            {
                Name(s.CtorString(0), "secret " + s.CtorString(0), s.Location);
                if (s.Enum("Type", "STRING") == "INFER") throw new Problem(s.Location, "secret " + s.CtorString(0) + " needs a type");
                Secrets.Add(s);
            }
            foreach (var c in cs)
            {
                string claim = c.String("Claim");
                Name(c.CtorString(0), "claim " + c.CtorString(0), c.Location);
                if (claim.Length == 0 || claim.Length > 256) throw new Problem(c.Location, "claim " + c.CtorString(0) + " needs a Claim of 1-256 characters");
                if (c.Enum("Type", "STRING") == "INFER") throw new Problem(c.Location, "claim " + c.CtorString(0) + " needs a type");
                Claims.Add(c);
            }
        }

        public void Merge(Vars other)
        {
            foreach (var e in other.Names) if (!Names.ContainsKey(e.Key)) Names[e.Key] = e.Value;
            Globals.AddRange(other.Globals);
            Secrets.AddRange(other.Secrets);
            Claims.AddRange(other.Claims);
        }

        private void Name(string name, string what, Location at)
        {
            CheckVariable(name, what, at);
            string key = name.ToLowerInvariant();
            if (Names.TryGetValue(key, out var prior)) throw new Problem(at, what + " and " + prior + " have the same name, ignoring case");
            Names[key] = what;
        }

        public Dictionary<string, object?> ToJson()
        {
            var g = new SortedDictionary<string, object?>(StringComparer.Ordinal);
            foreach (var x in Globals)
            {
                var spec = Spec.OfGlobal(x);
                var v = new Dictionary<string, object?>
                {
                    ["name"] = spec.Name,
                    ["type"] = spec.Type,
                    ["required"] = spec.Required,
                    ["sensitive"] = spec.Sensitive,
                };
                if (spec.Description.Length > 0) v["description"] = spec.Description;
                foreach (var c in GlobalConstraints(x)) v[c.Key] = c.Value;
                g[Jcs.Nfc(spec.Name)] = v;
            }
            var s = new SortedDictionary<string, object?>(StringComparer.Ordinal);
            foreach (var x in Secrets)
            {
                s[Jcs.Nfc(x.CtorString(0))] = new Dictionary<string, object?>
                {
                    ["name"] = x.CtorString(0),
                    ["type"] = x.Enum("Type", "STRING"),
                    ["scope"] = x.Enum("Scope", "GROUP"),
                };
            }
            var c2 = new SortedDictionary<string, object?>(StringComparer.Ordinal);
            foreach (var x in Claims)
            {
                c2[Jcs.Nfc(x.CtorString(0))] = new Dictionary<string, object?>
                {
                    ["name"] = x.CtorString(0),
                    ["claim"] = x.String("Claim"),
                    ["type"] = x.Enum("Type", "STRING"),
                    ["required"] = x.Bool("Required", true),
                };
            }
            return new Dictionary<string, object?>
            {
                ["globals"] = g.Values.ToList(),
                ["secrets"] = s.Values.ToList(),
                ["claims"] = c2.Values.ToList(),
            };
        }
    }

    /// <summary>The value type of an input schema, as the runtime's DeclaredValues names it.</summary>
    internal static string ValueType(Dictionary<string, object?> schema)
    {
        schema.TryGetValue("type", out var type);
        if (Equals(type, "integer"))
        {
            long max = schema.TryGetValue("maximum", out var mx) && mx is long a ? a : long.MaxValue;
            long min = schema.TryGetValue("minimum", out var mn) && mn is long b ? b : long.MinValue;
            return max <= int.MaxValue && min >= int.MinValue ? "INT" : "LONG";
        }
        if (Equals(type, "number")) return "NUMBER";
        if (Equals(type, "boolean")) return "BOOLEAN";
        if (Equals(type, "array")) return "ARRAY";
        if (!Equals(type, "string")) return "OBJECT";
        if (schema.ContainsKey("enum")) return "ENUM";
        if (schema.TryGetValue("contentEncoding", out var enc) && Equals(enc, "base64")) return "BINARY";
        schema.TryGetValue("format", out var format);
        return format switch
        {
            "date" => "DATE",
            "date-time" => "DATE_TIME",
            "duration" => "DURATION",
            "uri" => "URI",
            _ => "STRING",
        };
    }

    /// <summary>The constraint members of a global; INT and LONG bounds are not materialised.</summary>
    internal static Dictionary<string, object?> GlobalConstraints(Attr g)
    {
        var spec = Spec.OfGlobal(g);
        if (spec.Type == "INFER") throw new Problem(g.Location, "global " + spec.Name + " needs a type");
        if (spec.Type == "ENUM" && spec.EnumValues.Count == 0) throw new Problem(g.Location, "global " + spec.Name + " of type ENUM needs EnumValues");
        if (spec.Description.Length > 1024) throw new Problem(g.Location, "global " + spec.Name + " Description is longer than 1024 characters");
        var constraints = new Dictionary<string, object?>();
        if (spec.Type == "ENUM") constraints["enum"] = new List<object?>(spec.EnumValues);
        Schemas.Constraints(constraints, spec.Type, spec, g.Location, false, spec.Name);
        constraints.Remove("x-toolgate-sensitive");
        return constraints;
    }

    private static void ReservedPrefix(string value, string what, Location at)
    {
        foreach (string prefix in Reserved)
        {
            if (value.StartsWith(prefix, StringComparison.Ordinal) || value == prefix.Substring(0, prefix.Length - 1))
            {
                throw new Problem(at, what + " " + value + " uses the reserved prefix " + prefix);
            }
        }
    }

    internal static void CheckVariable(string name, string what, Location at)
    {
        if (!Full(Variable, name)) throw new Problem(at, what + " " + name + " does not match " + Variable);
    }

    private static void CheckDeclarationId(string id, string what, Location at)
    {
        if (!Full(DeclarationId, id)) throw new Problem(at, what + " " + id + " does not match " + DeclarationId);
    }

    private static void Length(string value, int min, int max, string what, Location at)
    {
        if (value.Length < min || value.Length > max) throw new Problem(at, what + " must be " + min + "-" + max + " characters");
    }

    private static void Range(long value, long min, long max, string what, Location at)
    {
        if (value < min || value > max) throw new Problem(at, what + " must be " + min + ".." + max);
    }

    private static void Port(int port, Location at) => Range(port, 1, 65535, "Port", at);

    private static List<string> Unique(List<string> values, string what, Location at)
    {
        var seen = new HashSet<string>();
        foreach (string v in values) if (!seen.Add(v)) throw new Problem(at, what + " " + v + " is listed twice");
        return values;
    }

    /// <summary>One <c>&lt;HandlerClass&gt;Vars</c> per class that holds tools, over the union of its tools' declarations.</summary>
    private void Accessors()
    {
        var byClass = new Dictionary<INamedTypeSymbol, List<Facts>>(SymbolEqualityComparer.Default);
        var order = new List<INamedTypeSymbol>();
        foreach (var tool in tools.Values)
        {
            var type = tool.ContainingType;
            if (!byClass.TryGetValue(type, out var list))
            {
                byClass[type] = list = new List<Facts>();
                order.Add(type);
            }
            list.Add(model.Facts[tool]);
        }
        foreach (var type in order)
        {
            Guard(() =>
            {
                var accessor = new AccessorClass();
                foreach (var f in byClass[type])
                {
                    Union(accessor.Globals, f.Globals, "global", type);
                    Union(accessor.Secrets, f.Secrets, "secret", type);
                    Union(accessor.Claims, f.Claims, "claim", type);
                }
                var names = new List<string>();
                for (var t = type; t != null; t = t.ContainingType) names.Insert(0, t.Name);
                accessor.ClassName = string.Join("", names) + "Vars";
                accessor.Namespace = type.ContainingNamespace.IsGlobalNamespace ? "" : type.ContainingNamespace.ToDisplayString();
                accessor.HandlerClass = type.ToDisplayString();
                foreach (var (what, view, values) in new[]
                {
                    ("global", "Globals", accessor.Globals), ("secret", "Secrets", accessor.Secrets), ("claim", "Claims", accessor.Claims),
                })
                {
                    string? collision = Collision(values, view);
                    if (collision != null) throw new Problem(Loc(type), what + " " + collision + " would have the same accessor in " + accessor.ClassName);
                }
                if (accessor.Globals.Count + accessor.Secrets.Count + accessor.Claims.Count > 0) model.Accessors.Add(accessor);
            });
        }
    }

    private static void Union(SortedDictionary<string, string> into, SortedDictionary<string, string> values, string what, INamedTypeSymbol at)
    {
        foreach (var v in values)
        {
            if (into.TryGetValue(v.Key, out var prior) && prior != v.Value)
            {
                throw new Problem(Loc(at), what + " " + v.Key + " is " + prior + " in one tool and " + v.Value + " in another");
            }
            into[v.Key] = v.Value;
        }
    }

    private static readonly HashSet<string> ReservedMembers = new() { "Equals", "GetHashCode", "ToString", "GetType", "MemberwiseClone", "Finalize" };

    /// <summary>The accessor property for a declared name: PascalCase, never an object member or its view's name.</summary>
    internal static string Member(string name, string view)
    {
        var output = new StringBuilder();
        foreach (string part in name.Split('_'))
        {
            if (part.Length == 0) continue;
            string p = part == part.ToUpperInvariant() ? part.ToLowerInvariant() : part;
            output.Append(char.ToUpperInvariant(p[0])).Append(p.Substring(1));
        }
        string m = output.ToString();
        return ReservedMembers.Contains(m) || m == view ? m + "Value" : m;
    }

    private static string? Collision(SortedDictionary<string, string> values, string view)
    {
        var seen = new Dictionary<string, string>();
        foreach (string name in values.Keys)
        {
            string member = Member(name, view);
            if (seen.TryGetValue(member, out var prior)) return prior + " and " + name;
            seen[member] = name;
        }
        return null;
    }

    private void Write(SortedDictionary<string, object?> descriptors)
    {
        var index = new List<object?>();
        try
        {
            foreach (var e in descriptors)
            {
                string canonical = Jcs.Canonical(e.Value);
                var tool = toolModels[e.Key];
                tool.Canonical = canonical;
                tool.Digest = Jcs.Sha256(Encoding.UTF8.GetBytes(canonical));
                var h = tools[e.Key];
                var entry = new Dictionary<string, object?>
                {
                    ["name"] = e.Key,
                    ["toolDescriptorDigest"] = tool.Digest,
                    ["handlerClass"] = h.ContainingType.ToDisplayString(),
                    ["handlerMethod"] = h.Name,
                    ["parameters"] = parametersByTool[e.Key].Cast<object?>().ToList(),
                };
                if (dataSchemasByTool[e.Key].Count > 0) entry["errorDataSchemas"] = new Dictionary<string, object?>(dataSchemasByTool[e.Key]);
                if (examplesByTool[e.Key].Count > 0) entry["examples"] = examplesByTool[e.Key];
                index.Add(entry);
                model.Tools[e.Key] = tool;
            }
            var manifest = new Dictionary<string, object?>
            {
                ["descriptorGeneration"] = (long)Generation,
                ["id"] = pkgId,
                ["name"] = pkg.String("Name"),
                ["publisher"] = pkg.String("Publisher"),
            };
            if (pkg.String("Description").Length > 0) manifest["description"] = pkg.String("Description");
            manifest["tools"] = index;
            model.IndexJson = Jcs.Canonical(manifest);
        }
        catch (ArgumentException e)
        {
            throw new Problem(pkg.Location, "cannot write descriptors: " + e.Message);
        }
    }
}
