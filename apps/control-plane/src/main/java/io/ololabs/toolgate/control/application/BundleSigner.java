// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.BundlePayload;
import io.ololabs.toolgate.contracts.SignedPolicyBundle;

/** Dedicated organization policy signing boundary, replaceable by an external KMS adapter. */
public interface BundleSigner {
    SignedPolicyBundle sign(BundlePayload payload);
}
