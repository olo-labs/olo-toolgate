# Key Management

## Purpose

Separate keys for Marketplace signing, organization deployment signing, bundle signing, Gateway permits and device identity. Never reuse trust-domain keys.

Policy bundle keys are implemented in Module 04 as a dedicated external RSA trust
domain. Control rejects reuse of its administrative IdP modulus; Gateway accepts
only the configured policy keyring and protected bundle header type. Operators
must also separate Marketplace/deployment/permit/device and release keys. See
[bundle key configuration and rotation](../control-plane/policy-bundles.md).
