// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using System.Collections.Generic;
using System.IO;
using System.Text;
using OloLabs.ToolGate.Sdk.TestSupport;
using Xunit;

namespace OloLabs.ToolGate.Sdk.Analyzers.Tests;

/// <summary>
/// The conformance cases this module passes for the dotnet target (tool-sdk/conformance). Each case also targets
/// other SDKs, so its status stays authored until they pass it too.
/// </summary>
public class ConformanceTest
{
    public static readonly Dictionary<string, string> Runs = new()
    {
        ["descriptors/canonical-bytes"] = "DescriptorVectorTest#ClientToolMatchesTool01,ServerToolMatchesTool02,TaskToolWithNonAsciiNamesMatchesTool03",
        ["descriptors/set-digest"] = "DescriptorVectorTest#SetDigestMatchesSet01",
        ["descriptors/excludes-deployment"] = "DescriptorVectorTest#DeploymentValuesAndExamplesDoNotChangeTheDigest",
        ["descriptors/generation-required"] = "DeclarationTest#GenerationIsRequiredAndHasNoDefault",
        ["route-analysis/path-variable-regex"] = "DeclarationTest#RoutesAreChecked",
        ["route-analysis/literal-single-route"] = "AnalysisTest#ALiteralCallMatchingOneListedRoutePackages",
        ["route-analysis/concatenation"] = "AnalysisTest#AConcatenatedTemplateFailsAtTheCallSite",
        ["route-analysis/global-template"] = "AnalysisTest#ATemplateOrDestinationFromAGlobalFails",
        ["route-analysis/ambiguous"] = "AnalysisTest#ATemplateMatchingTwoRoutesFailsNamingBoth",
        ["route-analysis/unlisted"] = "AnalysisTest#ARouteTheToolDoesNotListFails",
        ["route-analysis/reflection"] = "AnalysisTest#ReflectiveAccessToTheNetworkApiFails",
        ["declarations/identifier-grammar"] = "DeclarationTest#IdentifiersFollowTheirGrammars,ToolIdIsAtMost128Characters",
        ["declarations/reserved-prefixes"] = "DeclarationTest#ReservedPrefixesAreRejected",
        ["declarations/variable-name-unique"] = "DeclarationTest#VariableNamesAreUniqueIgnoringCase",
        ["declarations/tool-defaults"] = "DeclarationTest#ToolAttributesTakeTheSpecifiedDefaults",
        ["declarations/requires-business-key-default"] = "DeclarationTest#NonIdempotentDestructiveToolsRequireABusinessKey",
        ["declarations/async-duration"] = "DeclarationTest#AsyncToolsDefaultTo900000Ms",
        ["declarations/title-text-alias"] = "DeclarationTest#TitleAndTextAreExclusiveAliases",
        ["declarations/client-tool-restrictions"] = "DeclarationTest#ClientToolsCannotDeclareEgressOrSecrets",
        ["declarations/no-override"] = "DeclarationTest#ToolLevelDeclarationsNeverOverrideScopeLevelOnes",
        ["declarations/typed-grouped-identical"] = "DeclarationTest#TypedAndGroupedFormsGiveIdenticalBytes",
        ["declarations/closed-objects"] = "DeclarationTest#ObjectsAreClosedUnlessExplicitMaps",
        ["declarations/examples-limit"] = "DeclarationTest#AtMostEightExamples",
    };

    [Fact]
    public void EveryMappedCaseExistsAndTargetsDotnet()
    {
        string root = Compiler.Conformance;
        string cases = File.ReadAllText(Path.Combine(root, "sdk-unit", "declarations.json"), Encoding.UTF8)
            + File.ReadAllText(Path.Combine(root, "contract-descriptor", "descriptors.json"), Encoding.UTF8)
            + File.ReadAllText(Path.Combine(root, "contract-descriptor", "route-analysis.json"), Encoding.UTF8);
        ConformanceMap.Check(cases, Runs, typeof(ConformanceTest));
    }
}
