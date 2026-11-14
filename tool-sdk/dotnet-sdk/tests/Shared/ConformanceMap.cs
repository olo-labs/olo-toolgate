// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System;
using System.Collections.Generic;
using System.Reflection;
using Xunit;

namespace OloLabs.ToolGate.Sdk.TestSupport;

/// <summary>Checks a module's map of conformance case ids to the tests that pass them.</summary>
internal static class ConformanceMap
{
    public static void Check(string cases, IReadOnlyDictionary<string, string> runs, Type anchor)
    {
        foreach (var run in runs)
        {
            int at = cases.IndexOf("\"id\": \"" + run.Key + "\"", StringComparison.Ordinal);
            Assert.True(at >= 0, run.Key);
            int end = cases.IndexOf("\"status\"", at, StringComparison.Ordinal);
            Assert.True(cases.Substring(at, end - at).Contains("\"dotnet\""), run.Key + " does not target dotnet");
            string[] parts = run.Value.Split('#');
            var type = anchor.Assembly.GetType(anchor.Namespace + "." + parts[0]);
            Assert.True(type != null, run.Value);
            foreach (string method in parts[1].Split(','))
            {
                var m = type!.GetMethod(method, BindingFlags.Public | BindingFlags.Instance | BindingFlags.Static);
                Assert.True(m != null && (m.GetCustomAttribute<FactAttribute>() != null), run.Value + " is not a test");
            }
        }
    }
}
