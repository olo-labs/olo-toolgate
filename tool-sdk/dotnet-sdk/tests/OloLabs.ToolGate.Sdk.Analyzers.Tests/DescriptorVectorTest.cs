// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using OloLabs.ToolGate.Sdk.TestSupport;
using OloLabs.ToolGate.Shared;
using Xunit;

namespace OloLabs.ToolGate.Sdk.Analyzers.Tests;

/// <summary>Conformance: descriptors/canonical-bytes, set-digest, excludes-deployment and reproducibility for .NET.</summary>
public class DescriptorVectorTest
{
    private static byte[] Vector(string file) => File.ReadAllBytes(Path.Combine(Compiler.Vectors, file));

    private static string Expected(string file)
    {
        string json = File.ReadAllText(Path.Combine(Compiler.Vectors, "expected.json"), Encoding.UTF8);
        var m = Regex.Match(json, "\"" + Regex.Escape(file) + "\"\\s*:\\s*\"(sha256:[0-9a-f]{64})\"");
        Assert.True(m.Success, file);
        return m.Groups[1].Value;
    }

    private static void AssertVector(Compiler.Result result, string tool, string vector)
    {
        Assert.True(result.Success, result.Errors);
        byte[] emitted = Encoding.UTF8.GetBytes(result.Descriptor(tool));
        byte[] expected = Vector(vector.Replace(".json", ".jcs"));
        Assert.Equal(Encoding.UTF8.GetString(expected), Encoding.UTF8.GetString(emitted));
        Assert.Equal(expected, emitted);
        Assert.Equal(Expected(vector), Jcs.Sha256(emitted));
    }

    [Fact]
    public void ClientToolMatchesTool01() =>
        AssertVector(Compiler.Fixture("files"), "files.checksum", "tool-01-checksum.json");

    [Fact]
    public void ServerToolMatchesTool02() =>
        AssertVector(Compiler.Fixture("orders"), "orders.lookup", "tool-02-orders-lookup.json");

    [Fact]
    public void TaskToolWithNonAsciiNamesMatchesTool03() =>
        AssertVector(Compiler.Fixture("orders"), "orders.refund", "tool-03-refund.json");

    [Fact]
    public void SetDigestMatchesSet01()
    {
        var result = Compiler.Fixture("orders");
        Assert.True(result.Success, result.Errors);
        var entries = new List<object?>();
        foreach (string tool in new[] { "orders.lookup", "orders.refund" })
        {
            entries.Add(new Dictionary<string, object?>
            {
                ["toolId"] = "acme.orders/" + tool,
                ["toolDescriptorDigest"] = Jcs.Sha256(Encoding.UTF8.GetBytes(result.Descriptor(tool))),
            });
        }
        Assert.Equal(Expected("set-01-orders.json"), Jcs.Sha256(Jcs.Bytes(entries)));
    }

    [Fact]
    public void PackageIndexCarriesDigestsAndHandlers()
    {
        var result = Compiler.Fixture("orders");
        Assert.True(result.Success, result.Errors);
        string index = result.Index;
        Assert.Contains("\"handlerClass\":\"Acme.Orders.RefundTools\",\"handlerMethod\":\"RefundOrder\"", index);
        Assert.Contains("\"parameters\":[\"orderId\",\"reason\",\"refundKey\",\"$context\"]", index);
        Assert.Contains("\"toolDescriptorDigest\":\"" + Jcs.Sha256(Encoding.UTF8.GetBytes(result.Descriptor("orders.refund"))) + "\"", index);
        Assert.StartsWith("{\"descriptorGeneration\":1,\"id\":\"acme.orders\"", index);
    }

    [Fact]
    public void SameSourceGivesIdenticalBytes()
    {
        var a = Compiler.Fixture("orders");
        var b = Compiler.Fixture("orders");
        foreach (string tool in new[] { "orders.lookup", "orders.refund", "orders.refund-status" })
        {
            Assert.Equal(a.Descriptor(tool), b.Descriptor(tool));
        }
        Assert.Equal(a.Index, b.Index);
        Assert.Equal(a.Source("ToolGatePackage.g.cs"), b.Source("ToolGatePackage.g.cs"));
    }

    [Fact]
    public void DeploymentValuesAndExamplesDoNotChangeTheDigest()
    {
        var sources = Compiler.FixtureSources("orders");
        sources["orders/OrdersPackage.cs"] = sources["orders/OrdersPackage.cs"]
            .Replace("DefaultHost = \"api.acme.com\"", "DefaultHost = \"staging.acme.example\"");
        sources["orders/LookupTools.cs"] = sources["orders/LookupTools.cs"]
            .Replace("    public Task<Order> Lookup(", "    [Example(\"{\\\"orderId\\\":\\\"123456\\\"}\")]\n    public Task<Order> Lookup(");
        var changed = Compiler.Compile(sources);
        AssertVector(changed, "orders.lookup", "tool-02-orders-lookup.json");
        Assert.Contains("\"examples\":[{\"arguments\":\"{\\\"orderId\\\":\\\"123456\\\"}\"}]", changed.Index);
    }

    [Fact]
    public void GeneratedSourceEscapesNonAscii()
    {
        var result = Compiler.Fixture("orders");
        string source = result.Source("ToolGatePackage.g.cs")!;
        Assert.True(source.All(c => c < 0x80), "the generated package source is ASCII");
    }
}
