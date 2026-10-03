// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission.
 *
 * @param toolId canonical toolId value
 * @param arguments canonical arguments value
 * @param expectedOutput canonical expectedOutput value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record FleetSelfTest(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "toolId", required = true) String toolId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "arguments", required = true) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> arguments,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "expectedOutput", required = true) java.util.Map<String, com.fasterxml.jackson.databind.JsonNode> expectedOutput
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param toolId canonical toolId value
     * @param arguments canonical arguments value
     * @param expectedOutput canonical expectedOutput value
     */
    public FleetSelfTest {
        java.util.Objects.requireNonNull(toolId, "toolId");
        java.util.Objects.requireNonNull(arguments, "arguments");
        arguments = arguments == null ? null : java.util.Map.copyOf(arguments);
        java.util.Objects.requireNonNull(expectedOutput, "expectedOutput");
        expectedOutput = expectedOutput == null ? null : java.util.Map.copyOf(expectedOutput);
    }
}
