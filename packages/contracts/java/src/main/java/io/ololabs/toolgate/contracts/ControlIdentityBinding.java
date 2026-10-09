// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
package io.ololabs.toolgate.contracts;

/** Stable verified tenant/issuer/subject identity; email never determines identity.
 *
 * @param id canonical id value
 * @param name canonical name value
 * @param enabled canonical enabled value
 * @param revision canonical revision value
 * @param userId canonical userId value
 * @param issuer canonical issuer value
 * @param subject canonical subject value
 * @param sessionEpoch canonical sessionEpoch value
 * @param firstSeenUnixMs canonical firstSeenUnixMs value
 * @param lastAttemptUnixMs canonical lastAttemptUnixMs value
 * @param attemptCount canonical attemptCount value
 * @param registrationReason canonical registrationReason value
 * @param sessionsValidAfterUnixMs canonical sessionsValidAfterUnixMs value
 */
@com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
public record ControlIdentityBinding(
    @com.fasterxml.jackson.annotation.JsonProperty(value = "id", required = true) String id,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "name", required = true) String name,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "enabled", required = true) Boolean enabled,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "revision", required = true) Long revision,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "userId", required = true) String userId,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "issuer", required = true) String issuer,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "subject", required = true) String subject,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sessionEpoch", required = true) Long sessionEpoch,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "firstSeenUnixMs", required = true) Long firstSeenUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "lastAttemptUnixMs", required = true) Long lastAttemptUnixMs,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "attemptCount", required = true) Long attemptCount,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "registrationReason", required = true) String registrationReason,
    @com.fasterxml.jackson.annotation.JsonProperty(value = "sessionsValidAfterUnixMs", required = true) Long sessionsValidAfterUnixMs
) {
    /** Reject absent required references and copy collections to retain value semantics.
     *
     * @param id canonical id value
     * @param name canonical name value
     * @param enabled canonical enabled value
     * @param revision canonical revision value
     * @param userId canonical userId value
     * @param issuer canonical issuer value
     * @param subject canonical subject value
     * @param sessionEpoch canonical sessionEpoch value
     * @param firstSeenUnixMs canonical firstSeenUnixMs value
     * @param lastAttemptUnixMs canonical lastAttemptUnixMs value
     * @param attemptCount canonical attemptCount value
     * @param registrationReason canonical registrationReason value
     * @param sessionsValidAfterUnixMs canonical sessionsValidAfterUnixMs value
     */
    public ControlIdentityBinding {
        java.util.Objects.requireNonNull(id, "id");
        java.util.Objects.requireNonNull(name, "name");
        java.util.Objects.requireNonNull(enabled, "enabled");
        java.util.Objects.requireNonNull(revision, "revision");
        java.util.Objects.requireNonNull(userId, "userId");
        java.util.Objects.requireNonNull(issuer, "issuer");
        java.util.Objects.requireNonNull(subject, "subject");
        java.util.Objects.requireNonNull(sessionEpoch, "sessionEpoch");
        java.util.Objects.requireNonNull(firstSeenUnixMs, "firstSeenUnixMs");
        java.util.Objects.requireNonNull(lastAttemptUnixMs, "lastAttemptUnixMs");
        java.util.Objects.requireNonNull(attemptCount, "attemptCount");
        java.util.Objects.requireNonNull(registrationReason, "registrationReason");
        java.util.Objects.requireNonNull(sessionsValidAfterUnixMs, "sessionsValidAfterUnixMs");
    }
}
