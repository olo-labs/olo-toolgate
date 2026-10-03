// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.SignedPolicyBundle;

/** Dedicated organization policy signing boundary, replaceable by an external KMS adapter. */
public interface BundleSigner {
    /** Accept only the canonical format 1 or format 2 payload; adapters validate before signing. */
    SignedPolicyBundle sign(Object payload);
}
