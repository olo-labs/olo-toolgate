// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

/** Optional best-effort notification boundary. Polling durable status remains authoritative. */
public interface ApprovalNotification {
    /** Sanitized references only; adapters must never receive arguments, resource locators or tokens. */
    void pending(String tenantId, String approvalId);
}
