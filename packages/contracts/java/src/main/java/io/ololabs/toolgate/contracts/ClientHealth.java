// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Endpoint identity foundation wire model.
 *
 * @param state canonical state value
 * @param ready canonical ready value
 * @param uptimeSeconds canonical uptimeSeconds value
 * @param successfulCheckIns canonical successfulCheckIns value
 * @param failedCheckIns canonical failedCheckIns value
 * @param reportSequence canonical reportSequence value
 * @param lastSuccessUnixMs canonical lastSuccessUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ClientHealth(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "state", required = true) EndpointState state,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "ready", required = true) Boolean ready,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "uptimeSeconds", required = true) Long uptimeSeconds,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "successfulCheckIns", required = true) Long successfulCheckIns,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "failedCheckIns", required = true) Long failedCheckIns,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "reportSequence", required = true) Long reportSequence,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "lastSuccessUnixMs", required = false) Long lastSuccessUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param state canonical state value
     * @param ready canonical ready value
     * @param uptimeSeconds canonical uptimeSeconds value
     * @param successfulCheckIns canonical successfulCheckIns value
     * @param failedCheckIns canonical failedCheckIns value
     * @param reportSequence canonical reportSequence value
     * @param lastSuccessUnixMs canonical lastSuccessUnixMs value
     */
    public ClientHealth {
        java.util.Objects.requireNonNull(state, "state");
        java.util.Objects.requireNonNull(ready, "ready");
        java.util.Objects.requireNonNull(uptimeSeconds, "uptimeSeconds");
        java.util.Objects.requireNonNull(successfulCheckIns, "successfulCheckIns");
        java.util.Objects.requireNonNull(failedCheckIns, "failedCheckIns");
        java.util.Objects.requireNonNull(reportSequence, "reportSequence");
    }
}
