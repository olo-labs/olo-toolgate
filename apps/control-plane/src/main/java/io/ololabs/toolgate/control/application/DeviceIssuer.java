// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.contracts.DeviceIdentity;
import io.ololabs.toolgate.contracts.SignedClientDiscovery;

/** Dedicated device identity trust domain; receives public CSR, never device secrets. */
public interface DeviceIssuer {
    String fingerprint(String csr);
    DeviceIdentity issue(String csr,String device,String tenant,String user,String server,long now);
    SignedClientDiscovery discovery(String payload);
    String issuerCertificate();
    String peerFingerprint(java.security.cert.X509Certificate peer,long now);
}
