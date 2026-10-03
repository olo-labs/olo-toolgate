// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.control.application.BundleSigner;
import io.ololabs.toolgate.control.application.Failure;
import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.*;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.Base64;

/** RFC 7515 RS256 over exact payload bytes, using a dedicated externally mounted key. */
public final class RsaBundleSigner implements BundleSigner {
    private final RSAPrivateCrtKey key;
    private final ContractCodec codec;
    private final String header;
    public RsaBundleSigner(RSAPrivateCrtKey key, String keyId, ContractCodec codec) {
        if (!java.util.Set.of(2048,3072,4096).contains(key.getModulus().bitLength())
                || !key.getPublicExponent().equals(java.math.BigInteger.valueOf(65537))) throw new IllegalArgumentException("Invalid bundle key parameters");
        this.key = key; this.codec = codec;
        this.header = encode(codec.json(new BundleHeader("RS256", "toolgate-policy-bundle+jws", Ids.valid(keyId))));
    }
    public SignedPolicyBundle sign(Object payload) {
        var document = codec.json(payload);
        if (payload instanceof BundlePayload) codec.model(document, BundlePayload.class);
        else if (payload instanceof ApprovalBundlePayload) codec.model(document, ApprovalBundlePayload.class);
        else throw Failure.validation();
        var message = header + "." + encode(document);
        try {
            var signature = java.security.Signature.getInstance("SHA256withRSA");
            signature.initSign(key); signature.update(message.getBytes(StandardCharsets.US_ASCII));
            return new SignedPolicyBundle(message + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign()));
        } catch (java.security.GeneralSecurityException e) { throw Failure.unavailable(); }
    }
    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
