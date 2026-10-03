<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetArtifactGrantClaims implements \JsonSerializable {
    public function __construct(
        public string $tenantId,
        public string $serverId,
        public string $deviceId,
        public int $generation,
        public string $manifestDigest,
        public int $sizeBytes,
        public string $grantId,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['tenantId', 'serverId', 'deviceId', 'generation', 'manifestDigest', 'sizeBytes', 'grantId', 'issuedAtUnixMs', 'expiresAtUnixMs']) || array_diff(['tenantId', 'serverId', 'deviceId', 'generation', 'manifestDigest', 'sizeBytes', 'grantId', 'issuedAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['tenantId'],
            $data['serverId'],
            $data['deviceId'],
            $data['generation'],
            $data['manifestDigest'],
            $data['sizeBytes'],
            $data['grantId'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
