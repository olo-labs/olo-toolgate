// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Text;
using OloLabs.ToolGate.Shared;
using Xunit;

namespace OloLabs.ToolGate.Sdk.Analyzers.Tests;

/// <summary>RFC 8785 serialisation details the vectors do not reach.</summary>
public class JcsTest
{
    [Fact]
    public void NumbersUseEcmaScriptFormatting()
    {
        Assert.Equal("1", Jcs.Number(1.0));
        Assert.Equal("0.1", Jcs.Number(0.1));
        Assert.Equal("-2.5", Jcs.Number(-2.5));
        Assert.Equal("0", Jcs.Number(-0.0));
        Assert.Equal("1e+21", Jcs.Number(1e21));
        Assert.Equal("123456789012345680000", Jcs.Number(123456789012345680000.0));
        Assert.Equal("0.000001", Jcs.Number(0.000001));
        Assert.Equal("1e-7", Jcs.Number(1e-7));
        Assert.Equal("5e-324", Jcs.Number(double.Epsilon));
        Assert.Equal("1.7976931348623157e+308", Jcs.Number(double.MaxValue));
        Assert.Equal("333333333.3333333", Jcs.Number(333333333.33333329));
        Assert.Throws<ArgumentException>(() => Jcs.Number(double.NaN));
    }

    [Fact]
    public void StringsEscapeOnlyWhatJsonRequires()
    {
        Assert.Equal("\"\\u0001\\n\\\"\\\\/ é\"", Encoding.UTF8.GetString(Jcs.Bytes("\u0001\n\"\\/ é")));
    }

    [Fact]
    public void KeysAreNfcAndSortedByUtf16()
    {
        var m = new Dictionary<string, object?>
        {
            ["Ａlpha"] = 1L,
            ["😀note"] = 2L,
            ["état"] = new List<object?>(),
        };
        Assert.Equal("{\"état\":[],\"😀note\":2,\"Ａlpha\":1}", Encoding.UTF8.GetString(Jcs.Bytes(m)));
        m["état"] = true;
        Assert.Throws<ArgumentException>(() => Jcs.Bytes(m));
    }

    [Fact]
    public void IntegersBeyondTheSafeRangeAreRefused()
    {
        Assert.Equal("9007199254740991", Jcs.Canonical(9007199254740991L));
        Assert.Throws<ArgumentException>(() => Jcs.Canonical(9007199254740992L));
    }
}
