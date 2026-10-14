// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.Linq;
using OloLabs.ToolGate.Sdk.TestSupport;
using Xunit;

namespace OloLabs.ToolGate.Sdk.Analyzers.Tests;

/// <summary>Conformance: the sdk-unit declarations cases and descriptors/generation-required for .NET.</summary>
public class DeclarationTest
{
    private static string Tool(string name) => $$"""
            [ServerTool("{{name}}", Title = "T", Description = "D")]
            public Out Run([Param(Description = "Id")] string id) => null!;
        """;

    internal static Compiler.Result Ok(Compiler.Result r)
    {
        Assert.True(r.Success, r.Errors);
        return r;
    }

    internal static void Fails(Compiler.Result r, params string[] fragments)
    {
        Assert.False(r.Success, "compiled");
        foreach (string f in fragments) Assert.True(r.Errors.Contains(f), "missing \"" + f + "\" in:\n" + r.Errors);
    }

    [Fact]
    public void GenerationIsRequiredAndHasNoDefault()
    {
        var missing = Compiler.Tools("", Tool("items.list"), properties: Compiler.NoGeneration);
        Fails(missing, "ToolGateDescriptorGeneration");
        Assert.Contains(missing.ErrorDiagnostics, d => d.Id == "TG0001");
        Fails(Compiler.Tools("", Tool("items.list"), properties: new Dictionary<string, string>
        {
            ["build_property.ToolGateDescriptorGeneration"] = "2",
        }), "is not supported");
        Ok(Compiler.Tools("", Tool("items.list")));
    }

    [Fact]
    public void IdentifiersFollowTheirGrammars()
    {
        Fails(Compiler.Tools("", Tool("Items.list")), "tool name Items.list");
        Fails(Compiler.Tools("", Tool("a.b.c.d")), "tool name a.b.c.d");
        Ok(Compiler.Tools("", Tool("a.b.c")));
        Fails(Compiler.Tools("", Tool("items.list").Replace("[Param(Description = \"Id\")] string id", "[Param(Name = \"1id\", Description = \"x\")] string other")),
            "input name 1id");
        Fails(Compiler.Tools("[assembly: HttpRoute(\"Bad\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/x\")]", Tool("items.list")), "route id Bad");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                [ErrorDecl("AB", "x")]
                public Out Run() => null!;
            """), "error code AB");
        Fails(WithPackageId("Acme.demo"), "package id Acme.demo");
        Fails(WithPackageId("acme"), "package id acme");
    }

    [Fact]
    public void ToolIdIsAtMost128Characters()
    {
        // "acme.demo/" is 10 characters, so a 118-character local name gives a 128-character toolId.
        string seg = new('a', 38);
        Ok(Compiler.Tools("", Tool(seg + "." + seg + "." + new string('c', 40))));
        Fails(Compiler.Tools("", Tool(seg + "." + seg + "." + new string('c', 41))), "longer than 128");
    }

    private static Compiler.Result WithPackageId(string id) => Compiler.Compile(new Dictionary<string, string>
    {
        ["Acme/Tools.cs"] = $$"""
            using OloLabs.ToolGate.Sdk;
            [assembly: ToolPackage("{{id}}", Name = "Demo", Publisher = "Acme")]
            namespace Acme.Demo;
            public class Tools
            {
                public record Out(string Value);
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Out Run() => null!;
            }
            """,
    });

    [Fact]
    public void ReservedPrefixesAreRejected()
    {
        Fails(WithPackageId("toolgate.demo"), "reserved prefix toolgate.");
        Fails(WithPackageId("hotfolder.demo"), "reserved prefix hotfolder.");
        Fails(Compiler.Tools("", Tool("builtin.items")), "reserved prefix builtin.");
    }

    [Fact]
    public void VariableNamesAreUniqueIgnoringCase()
    {
        Fails(Compiler.Tools("", Tool("items.list"), "[Global(\"ID\", ValueType.String)]"), "input id", "global ID");
        Fails(Compiler.Tools("", Tool("items.list"), "[Secret(\"Token\")] [JwtClaim(\"token\", Claim = \"sub\")]"), "claim token", "secret Token");
    }

    [Fact]
    public void ToolLevelDeclarationsNeverOverrideScopeLevelOnes()
    {
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                [Global("Limit", ValueType.Int, DefaultValue = "5")]
                public Out Run() => null!;
            """, "[Global(\"Limit\", ValueType.Int)]"), "cannot override", "global Limit");
    }

    [Fact]
    public void ToolAttributesTakeTheSpecifiedDefaults()
    {
        var r = Ok(Compiler.Tools("", """
                [ServerTool("items.get", Title = "T", Description = "D", Routes = new[] { "items.get" })]
                public Out Get() => null!;
                [ClientTool("items.local", Title = "T", Description = "D")]
                public Out Local() => null!;
            """));
        string server = r.Descriptor("items.get");
        string client = r.Descriptor("items.local");
        Assert.Contains("\"effect\":{\"destructive\":false,\"openWorld\":true,\"safety\":\"UNKNOWN\"}", server);
        Assert.Contains("\"effect\":{\"destructive\":false,\"openWorld\":false,\"safety\":\"UNKNOWN\"}", client);
        foreach (string d in new[] { server, client })
        {
            Assert.Contains("\"execution\":{\"async\":false,\"cancelIfAbandoned\":false,\"maxDurationMs\":30000,\"stateless\":false}", d);
            Assert.Contains("\"businessKey\":{\"namespace\":\"AGENT\",\"required\":false}", d);
        }
        Assert.Contains("\"tiers\":[\"OCI_SANDBOX\"]", client);
        Assert.DoesNotContain("\"tiers\"", server);
        Assert.Contains("\"permissions\":[\"HTTP_ROUTE\"]", server);
        Assert.Contains("\"destinations\":[{\"hostSetting\":\"ApiHost\",\"id\":\"api\",\"port\":443,\"spkiPins\":[]}]", server);
    }

    [Fact]
    public void NonIdempotentDestructiveToolsRequireABusinessKey()
    {
        var r = Ok(Compiler.Tools("", """
                [ServerTool("items.delete", Title = "T", Description = "D", Effect = EffectSafety.NonIdempotent, Destructive = true)]
                public Out Delete() => null!;
                [ServerTool("items.create", Title = "T", Description = "D", Effect = EffectSafety.NonIdempotent)]
                public Out Create() => null!;
                [ServerTool("items.purge", Title = "T", Description = "D", Effect = EffectSafety.NonIdempotent, Destructive = true,
                    RequiresBusinessKey = BusinessKey.NotRequired)]
                public Out Purge() => null!;
            """));
        Assert.Contains("\"businessKey\":{\"namespace\":\"AGENT\",\"required\":true}", r.Descriptor("items.delete"));
        Assert.Contains("\"businessKey\":{\"namespace\":\"AGENT\",\"required\":false}", r.Descriptor("items.create"));
        Assert.Contains("\"businessKey\":{\"namespace\":\"AGENT\",\"required\":false}", r.Descriptor("items.purge"));
    }

    [Fact]
    public void AsyncToolsDefaultTo900000Ms()
    {
        var r = Ok(Compiler.Tools("", """
                [ServerTool("items.export", Title = "T", Description = "D", Async = true)]
                public Out Export() => null!;
            """));
        Assert.Contains("\"maxDurationMs\":900000", r.Descriptor("items.export"));
        Fails(Compiler.Tools("", """
                [ServerTool("items.slow", Title = "T", Description = "D", MaxDurationMs = 120000)]
                public Out Slow() => null!;
            """), "make the tool async");
    }

    [Fact]
    public void TitleAndTextAreExclusiveAliases()
    {
        var r = Ok(Compiler.Tools("", """
                [ServerTool("items.list", Text = "Listed", Description = "D")]
                public Out Run() => null!;
            """));
        Assert.Contains("\"title\":\"Listed\"", r.Descriptor("items.list"));
        Assert.DoesNotContain("\"text\"", r.Descriptor("items.list"));
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "A", Text = "B", Description = "D")]
                public Out Run() => null!;
            """), "exactly one of Title and Text");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Description = "D")]
                public Out Run() => null!;
            """), "exactly one of Title and Text");
    }

    [Fact]
    public void ClientToolsCannotDeclareEgressOrSecrets()
    {
        string client = """
                [ClientTool("items.local", Title = "T", Description = "D"{0})]
                public Out Local() => null!;
            """;
        var routes = Compiler.Tools("", string.Format(client, ", Routes = new[] { \"items.get\" }"));
        Fails(routes, "cannot list Routes");
        Assert.Contains(routes.ErrorDiagnostics, d => d.Id == "TG0003");
        Fails(Compiler.Tools("[assembly: TcpDestination(\"db\", HostSetting = \"DbHost\", Port = 5432)]", string.Format(client, ", Tcp = new[] { \"db\" }")),
            "cannot list Tcp");
        var secrets = Compiler.Tools("", string.Format(client, ""), "[Secret(\"Token\")]");
        Fails(secrets, "client tool cannot use secrets");
        Assert.Contains(secrets.ErrorDiagnostics, d => d.Id == "TG0003");
        // HttpAuth is an assembly attribute in .NET, so it cannot sit next to a client tool at all.
        Assert.False(Compiler.Tools("", "[HttpAuth(\"key\", AuthType.ApiKey, Header = \"X-Key\")]\n" + string.Format(client, "")).Success);
    }

    [Fact]
    public void TypedAndGroupedFormsGiveIdenticalBytes()
    {
        var typed = Ok(Compiler.Tools("", """
                [ServerTool("items.find", Title = "T", Description = "D")]
                public Out Find([Param(Description = "Order number", Pattern = "^[0-9]+$")] string orderId,
                    [Param(Description = "How many", Min = "1", Required = false)] int count,
                    [Param(Description = "Sort", EnumValues = new[] { "ASC", "DESC" }, DefaultValue = "ASC")] string sort,
                    [Param(Description = "Token", Sensitive = true)] string token,
                    ToolContext context) => null!;
            """));
        var grouped = Ok(Compiler.Tools("", """
                [ServerTool("items.find", Title = "T", Description = "D")]
                [ToolVariables]
                [Variable("orderId", "Order number", ValueType.String, Pattern = "^[0-9]+$")]
                [Variable("count", "How many", ValueType.Int, Min = "1", Required = false)]
                [Variable("sort", "Sort", ValueType.Enum, EnumValues = new[] { "ASC", "DESC" }, DefaultValue = "ASC")]
                [Variable("token", "Token", ValueType.String, Sensitive = true)]
                public Out Find(ToolContext context) => null!;
            """));
        Assert.Equal(typed.Descriptor("items.find"), grouped.Descriptor("items.find"));
        Assert.Contains("\"count\":{\"description\":\"How many\",\"maximum\":2147483647,\"minimum\":1,\"type\":\"integer\"}", typed.Descriptor("items.find"));
        Assert.Contains("\"required\":[\"orderId\",\"token\"]", typed.Descriptor("items.find"));
        Assert.Contains("\"x-toolgate-sensitive\":true", typed.Descriptor("items.find"));
    }

    [Fact]
    public void ObjectsAreClosedUnlessExplicitMaps()
    {
        var r = Ok(Compiler.Tools("", """
                public record Address(string Street, string? Zip);
                [ServerTool("items.put", Title = "T", Description = "D")]
                public Out Put([Param(Description = "Where")] Address address,
                    [Param(Description = "Counts")] Dictionary<string, int> counts,
                    [Param(Description = "Ids")] IReadOnlyList<long> ids) => null!;
            """));
        string d = r.Descriptor("items.put");
        Assert.Contains("\"address\":{\"additionalProperties\":false,\"description\":\"Where\",\"properties\":{\"street\":{\"type\":\"string\"},"
            + "\"zip\":{\"type\":\"string\"}},\"required\":[\"street\"],\"type\":\"object\"}", d);
        Assert.Contains("\"counts\":{\"additionalProperties\":{\"maximum\":2147483647,\"minimum\":-2147483648,\"type\":\"integer\"},"
            + "\"description\":\"Counts\",\"type\":\"object\"}", d);
        Assert.Contains("\"ids\":{\"description\":\"Ids\",\"items\":{\"maximum\":9007199254740991,\"minimum\":-9007199254740991,\"type\":\"integer\"},\"type\":\"array\"}", d);
    }

    [Fact]
    public void RecursiveTypesUseDefs()
    {
        var r = Ok(Compiler.Tools("", """
                public record Node(string Name, List<Node> Children);
                [ServerTool("items.tree", Title = "T", Description = "D")]
                public Node Tree() => null!;
            """));
        Assert.Contains("\"outputSchema\":{\"$defs\":{\"Node\":{\"additionalProperties\":false,\"properties\":{\"children\":{\"items\":{\"$ref\":\"#/$defs/Node\"},"
            + "\"type\":\"array\"},\"name\":{\"type\":\"string\"}},\"required\":[\"children\",\"name\"],\"type\":\"object\"}},\"$ref\":\"#/$defs/Node\"}", r.Descriptor("items.tree"));
    }

    [Fact]
    public void AtMostEightExamples()
    {
        string eight = string.Concat(Enumerable.Repeat("[Example(\"{}\")]", 8));
        Ok(Compiler.Tools("", eight + Tool("items.list")));
        Fails(Compiler.Tools("", eight + "[Example(\"{}\")]" + Tool("items.list")), "9 examples");
    }

    [Fact]
    public void RoutesAreChecked()
    {
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.all\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items/{rest:.*}\")]",
            Tool("items.list")), "may not use .*");
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.all\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items/{rest:.+}\")]",
            Tool("items.list")), "may not use .+");
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.one\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items/{id}\")]",
            Tool("items.list")), "path variable id needs a pattern");
        Ok(Compiler.Tools("[assembly: HttpRoute(\"items.one\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items/{id:[0-9]+}\")]",
            Tool("items.list")));
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.all\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items\", Headers = new[] { \"Cookie\" })]",
            Tool("items.list")), "may not allow header Cookie");
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.all\", Destination = \"api\", Method = HttpMethod.Get, Path = \"/items\", Headers = new[] { \"X-Forwarded-For\" })]",
            Tool("items.list")), "may not allow header X-Forwarded-For");
        Fails(Compiler.Tools("[assembly: HttpRoute(\"items.all\", Destination = \"nowhere\", Method = HttpMethod.Get, Path = \"/items\")]",
            Tool("items.list")), "undeclared destination nowhere");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D", Routes = new[] { "items.missing" })]
                public Out Run() => null!;
            """), "undeclared route items.missing");
    }

    [Fact]
    public void PermissionsAreDerived()
    {
        var r = Ok(Compiler.Tools("""
                [assembly: HttpAuth("exchange", AuthType.TokenExchange, Audience = "api")]
                [assembly: HttpRoute("items.mine", Destination = "api", Method = HttpMethod.Get, Path = "/me", Auth = "exchange")]
                [assembly: TcpDestination("db", HostSetting = "DbHost", Port = 5432)]
                """, """
                [ServerTool("items.mine", Title = "T", Description = "D", Routes = new[] { "items.mine" }, Tcp = new[] { "db" })]
                public Out Mine() => null!;
            """, "[Secret(\"Personal\", Scope = SecretScope.User)]"));
        Assert.Contains("\"permissions\":[\"CREDENTIALS_IN_SANDBOX\",\"HTTP_ROUTE\",\"TCP\",\"TOKEN_EXCHANGE\",\"USER_SECRET\"]", r.Descriptor("items.mine"));
    }

    [Fact]
    public void HandlerShapesAreChecked()
    {
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public static Out Run() => null!;
            """), "public, non-generic instance method");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Task Run() => Task.CompletedTask;
            """), "has no result");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Out Run(string id) => null!;
            """), "parameter id needs [Param]");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public string Run() => "";
            """), "the return type must be");
        Fails(Compiler.Tools("", """
                [ServerTool("items.list", Title = "T", Description = "D")]
                public Out Run([Param(Description = "When")] Guid when) => null!;
            """), "unsupported type System.Guid");
    }

    [Fact]
    public void DotNetTypesMapToTheValueTypeTable()
    {
        var r = Ok(Compiler.Tools("", """
                public enum Color { RED, GREEN }
                [ServerTool("items.types", Title = "T", Description = "D")]
                public Task<ToolResult> Types([Param(Description = "d")] DateOnly date, [Param(Description = "d")] DateTimeOffset at,
                    [Param(Description = "d")] TimeSpan wait, [Param(Description = "d")] Uri link, [Param(Description = "d")] byte[] blob,
                    [Param(Description = "d")] decimal price, [Param(Description = "d")] bool flag, [Param(Description = "d")] Color color,
                    [Param(Description = "d", Type = ValueType.Date)] string day, [Param(Description = "d")] string[] tags,
                    CancellationToken cancellation) => null!;
            """));
        string d = r.Descriptor("items.types");
        Assert.Contains("\"date\":{\"description\":\"d\",\"format\":\"date\",\"type\":\"string\"}", d);
        Assert.Contains("\"at\":{\"description\":\"d\",\"format\":\"date-time\",\"type\":\"string\"}", d);
        Assert.Contains("\"wait\":{\"description\":\"d\",\"format\":\"duration\",\"type\":\"string\"}", d);
        Assert.Contains("\"link\":{\"description\":\"d\",\"format\":\"uri\",\"type\":\"string\"}", d);
        Assert.Contains("\"blob\":{\"contentEncoding\":\"base64\",\"description\":\"d\",\"type\":\"string\"}", d);
        Assert.Contains("\"price\":{\"description\":\"d\",\"type\":\"number\"}", d);
        Assert.Contains("\"color\":{\"description\":\"d\",\"enum\":[\"RED\",\"GREEN\"],\"type\":\"string\"}", d);
        Assert.Contains("\"day\":{\"description\":\"d\",\"format\":\"date\",\"type\":\"string\"}", d);
        Assert.Contains("\"tags\":{\"description\":\"d\",\"items\":{\"type\":\"string\"},\"type\":\"array\"}", d);
        Assert.DoesNotContain("outputSchema", d);
        Assert.Contains("\"parameters\":[\"date\",\"at\",\"wait\",\"link\",\"blob\",\"price\",\"flag\",\"color\",\"day\",\"tags\",\"$cancellation\"]", r.Index);
    }
}
