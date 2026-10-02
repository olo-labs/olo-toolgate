// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import jakarta.enterprise.context.RequestScoped;

/** Server-generated correlation identifier; caller headers cannot forge audit correlation. */
@RequestScoped
public class Correlation {
    private final String id = java.util.UUID.randomUUID().toString();
    public String id() { return id; }
}
