// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Linq;
using OloLabs.ToolGate.Sdk.Analyzers.Model;
using OloLabs.ToolGate.Sdk.TestSupport;
using Xunit;
using static OloLabs.ToolGate.Sdk.Analyzers.Tests.DeclarationTest;

namespace OloLabs.ToolGate.Sdk.Analyzers.Tests;

/// <summary>Conformance: the route-analysis cases of http.md §3, the declared-value checks and the typed accessors of dotnet.md.</summary>
public class AnalysisTest
{
    private const string Routes = """
        [assembly: HttpRoute("items.list", Destination = "api", Method = HttpMethod.Get, Path = "/items")]
        [assembly: HttpRoute("admin.get", Destination = "api", Method = HttpMethod.Get, Path = "/admin")]
        """;

    /// <summary>A server tool listing items.get and items.list, whose body is <paramref name="statement"/>.</summary>
    private static Compiler.Result Handler(string classAttributes, string statement, string assemblyAttributes = "") =>
        Compiler.Tools(Routes + assemblyAttributes, $$"""
                [ServerTool("items.get", Title = "T", Description = "D", Routes = new[] { "items.get", "items.list" })]
                [ErrorDecl("NOT_FOUND", "Missing")]
                public async Task<Out> Run([Param(Description = "Id")] string id, ToolContext context)
                {
                    {{statement}}
                    await Task.Yield();
                    return null!;
                }
            """, classAttributes);

    [Fact]
    public void ALiteralCallMatchingOneListedRoutePackages()
    {
        Ok(Handler("", "await context.Http.Get(\"api\", \"/items/{id}\").PathVar(\"id\", id).SendAsync<string>();"));
        Ok(Handler("", "await context.Http.Get(\"api\", \"/items/{id:[0-9]+}\").PathVar(\"id\", id).SendAsync<string>();"));
        Ok(Handler("", "IHttpClient http = context.Http; await http.Get(\"api\", \"/items\").SendAsync<string>();"));
        Ok(Handler("", "const string api = \"api\"; await context.Http.Get(api, \"/items\").SendAsync<string>();"));
    }

    [Fact]
    public void AConcatenatedTemplateFailsAtTheCallSite()
    {
        var r = Handler("", "await context.Http.Get(\"api\", \"/items/\" + id).SendAsync<string>();");
        Fails(r, "constant strings");
        Assert.Contains(r.ErrorDiagnostics, d => d.Id == "TG0020" && d.Location.GetLineSpan().StartLinePosition.Line > 0);
    }

    [Fact]
    public void ATemplateOrDestinationFromAGlobalFails()
    {
        Fails(Handler("[Global(\"Path\", ValueType.String)]",
            "await context.Http.Get(\"api\", context.Global.GetStringValue(\"Path\")).SendAsync<string>();"), "constant strings");
        Fails(Handler("[Global(\"Dest\", ValueType.String)]",
            "await context.Http.Get(context.Global.GetStringValue(\"Dest\"), \"/items\").SendAsync<string>();"), "constant strings");
    }

    [Fact]
    public void ATemplateMatchingTwoRoutesFailsNamingBoth()
    {
        Fails(Handler("", "await context.Http.Get(\"api\", \"/items/{id}\").SendAsync<string>();",
                "[assembly: HttpRoute(\"items.also\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items/{id:[a-z]+}\")]"),
            "more than one route", "items.get", "items.also");
    }

    [Fact]
    public void ARouteTheToolDoesNotListFails()
    {
        Fails(Handler("", "await context.Http.Get(\"api\", \"/admin\").SendAsync<string>();"), "route admin.get", "does not list");
        Fails(Handler("", "await context.Http.Post(\"api\", \"/items\").SendAsync<string>();"), "matches no declared route", "candidates");
    }

    [Fact]
    public void CallsOutsideAHandlerFail()
    {
        Fails(Compiler.Tools(Routes, """
                [ServerTool("items.get", Title = "T", Description = "D", Routes = new[] { "items.get" })]
                public Out Run(ToolContext context) => null!;
                static object Helper(IHttpClient http) => http.Get("api", "/items").SendAsync<string>();
            """), "outside a tool handler");
    }

    [Fact]
    public void ReflectiveAccessToTheNetworkApiFails()
    {
        Fails(Handler("", "typeof(IHttpClient).GetMethod(\"Get\")!.Invoke(context.Http, new object[] { \"api\", \"/items\" });"),
            "reflective access to IHttpClient");
        Fails(Handler("", "object c = typeof(ITcpConnector);"), "reflective access to ITcpConnector");
    }

    [Fact]
    public void LiteralValueNamesMustBeDeclaredAndReadWithTheirType()
    {
        Ok(Handler("[Global(\"Limit\", ValueType.Int)]", "context.Global.GetIntValue(\"Limit\"); context.Global.GetLongValue(\"Limit\");"));
        Fails(Handler("[Global(\"Limit\", ValueType.Int)]", "context.Global.GetIntValue(\"Limt\");"), "declares no global Limt", "declared: Limit");
        Fails(Handler("[Global(\"Limit\", ValueType.Int)]", "context.Global.GetStringValue(\"Limit\");"),
            "global Limit of tool items.get is INT; GetStringValue does not read it");
        Fails(Handler("[Secret(\"Token\")]", "context.Secret.GetStringValue(\"Tokn\");"), "declares no secret Tokn");
        Fails(Handler("[JwtClaim(\"tenant\", Claim = \"tid\")]", "context.JWT.GetIntValue(\"tenant\");"), "claim tenant", "is STRING");
        Ok(Handler("[JwtClaim(\"tenant\", Claim = \"tid\")]", "context.JWT.GetStringValue(\"tenant\");"));
        Fails(Handler("", "context.GetStringValue(\"nope\");"), "declares no input nope");
        Ok(Handler("", "context.GetStringValue(\"id\");"));
        // A name the build cannot see is left to the runtime.
        Ok(Handler("", "string n = string.Concat(\"no\", \"pe\"); context.Global.GetStringValue(n);"));
        var wrong = Handler("[Global(\"Limit\", ValueType.Int)]", "context.Global.GetStringValue(\"Limit\");");
        Assert.Contains(wrong.ErrorDiagnostics, d => d.Id == "TG0011");
        var missing = Handler("", "context.GetStringValue(\"nope\");");
        Assert.Contains(missing.ErrorDiagnostics, d => d.Id == "TG0010");
    }

    [Fact]
    public void BusinessErrorCodesMustBeDeclared()
    {
        Ok(Handler("", "if (id == \"x\") throw new ToolBusinessError(\"NOT_FOUND\", \"No item\");"));
        var r = Handler("", "if (id == \"x\") throw new ToolBusinessError(\"GONE\", \"No item\");");
        Fails(r, "does not declare error code GONE");
        Assert.Contains(r.ErrorDiagnostics, d => d.Id == "TG0040");
        Ok(Handler("", "string code = id; if (id == \"x\") throw new ToolBusinessError(code, \"No item\");"));
    }

    [Fact]
    public void CodeTheAnalyserDoesNotCheckCompilesUntouched()
    {
        Ok(Handler("", """
                var xs = new List<string> { "a" };
                    xs.Select(x => x.Length).ToList().ForEach(n => { });
                    Func<Task<string>> f = () => context.Http.Get("api", "/items").SendAsync<string>();
                    async Task<string> Local() => await context.Http.Get("api", "/items").SendAsync<string>();
            """));
    }

    [Fact]
    public void TypedAccessorsReadEachDeclaredValueWithItsGetter()
    {
        var r = Ok(Compiler.Tools(Routes, """
                [ServerTool("items.get", Title = "T", Description = "D", Routes = new[] { "items.get" })]
                public Out Run(ToolContext context)
                {
                    int limit = ToolsVars.Global(context).Limit;
                    bool d = ToolsVars.Global(context).Default;
                    string key = ToolsVars.Secret(context).ApiKey;
                    string tenant = ToolsVars.Jwt(context).Tenant;
                    Out config = ToolsVars.Secret(context).Config<Out>();
                    string h = ToolsVars.Global(context).GetHashCodeValue;
                    return new Out(limit + key + tenant + d + config + h);
                }
            """, """
                [Global("Limit", ValueType.Int)]
                [Global("default", ValueType.Boolean)]
                [Global("GetHashCode", ValueType.String)]
                [Secret("API_KEY")]
                [Secret("Config", Type = ValueType.Object)]
                [JwtClaim("tenant", Claim = "tid")]
                """));
        Assert.NotNull(r.Source("Acme.Demo.ToolsVars.g.cs"));
    }

    [Fact]
    public void NoAccessorsAreGeneratedWithoutDeclaredValues()
    {
        var r = Ok(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Out Run() => null!;
            """));
        Assert.Null(r.Source("Acme.Demo.ToolsVars.g.cs"));
    }

    [Fact]
    public void AccessorNamesAreCSharpIdentifiers()
    {
        Assert.Equal("Limit", PackageBuilder.Member("Limit", "Globals"));
        Assert.Equal("ApiKey", PackageBuilder.Member("API_KEY", "Globals"));
        Assert.Equal("MaxItemsPerPage", PackageBuilder.Member("maxItemsPerPage", "Globals"));
        Assert.Equal("ToStringValue", PackageBuilder.Member("ToString", "Globals"));
        Assert.Equal("GlobalsValue", PackageBuilder.Member("globals", "Globals"));
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Out Run() => null!;
            """, "[Global(\"max_items\", ValueType.Int)] [Global(\"maxItems\", ValueType.Int)]"), "maxItems and max_items");
    }

    [Fact]
    public void CamelCaseMatchesSystemTextJson()
    {
        foreach (string name in new[] { "OrderId", "URL", "URLValue", "ID", "Id", "already", "Ａlpha", "ÉTAT", "A", "AB C", "XMLHttpRequest", "Sha256", "" })
        {
            Assert.Equal(System.Text.Json.JsonNamingPolicy.CamelCase.ConvertName(name), Schemas.CamelCase(name));
        }
    }

    [Fact]
    public void EveryTgDiagnosticIsAnError()
    {
        var analyzer = new ToolGateAnalyzer();
        Assert.All(analyzer.SupportedDiagnostics, d => Assert.Equal(Microsoft.CodeAnalysis.DiagnosticSeverity.Error, d.DefaultSeverity));
        Assert.All(analyzer.SupportedDiagnostics.Select(d => d.Id), id => Assert.Matches("^TG00[0-9]{2}$", id));
        Assert.Equal(new[] { "TG0010", "TG0011", "TG0020", "TG0030", "TG0040" }, analyzer.SupportedDiagnostics.Select(d => d.Id).OrderBy(i => i).ToArray());
    }
}
