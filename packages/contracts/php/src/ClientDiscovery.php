<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class ClientDiscovery implements \JsonSerializable {
    public function __construct(
        public int $protocolVersion,
        public string $serverId,
        public string $organization,
        public string $tenantId,
        public string $controlUrl,
        public string $gatewayUrl,
        public string $verificationUri,
        public string $minimumClientVersion,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public string $issuerCertificatePem
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['protocolVersion', 'serverId', 'organization', 'tenantId', 'controlUrl', 'gatewayUrl', 'verificationUri', 'minimumClientVersion', 'issuedAtUnixMs', 'expiresAtUnixMs', 'issuerCertificatePem']) || array_diff(['protocolVersion', 'serverId', 'organization', 'tenantId', 'controlUrl', 'gatewayUrl', 'verificationUri', 'minimumClientVersion', 'issuedAtUnixMs', 'expiresAtUnixMs', 'issuerCertificatePem'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['protocolVersion'],
            $data['serverId'],
            $data['organization'],
            $data['tenantId'],
            $data['controlUrl'],
            $data['gatewayUrl'],
            $data['verificationUri'],
            $data['minimumClientVersion'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['issuerCertificatePem']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
