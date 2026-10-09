// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.SignedPolicyBundle;

/** Dedicated organization policy signing boundary, replaceable by an external KMS adapter. */
public interface BundleSigner {
    /** Accept only the canonical format 3 group snapshot; adapters validate before signing. */
    SignedPolicyBundle sign(Object payload);
}
