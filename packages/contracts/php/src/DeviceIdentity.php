<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class DeviceIdentity implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public string $tenantId,
        public string $userId,
        public string $serverId,
        public string $certificatePem,
        public string $issuerCertificatePem,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'tenantId', 'userId', 'serverId', 'certificatePem', 'issuerCertificatePem', 'expiresAtUnixMs']) || array_diff(['deviceId', 'tenantId', 'userId', 'serverId', 'certificatePem', 'issuerCertificatePem', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['tenantId'],
            $data['userId'],
            $data['serverId'],
            $data['certificatePem'],
            $data['issuerCertificatePem'],
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
