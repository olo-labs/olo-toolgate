// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;
import io.ololabs.toolgate.contracts.*;
/** Dedicated effect-capability key boundary, independent from policy and identity signing keys. */
public interface EffectSigner {
    EnterpriseSignedPermit sign(EnterprisePermitClaims claims);
    EnterprisePermitClaims verify(EnterpriseSignedPermit permit);
}
