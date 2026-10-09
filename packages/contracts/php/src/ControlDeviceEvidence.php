<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Administrator/attestation sourced facts; callers cannot supply authoritative evidence. */
final readonly class ControlDeviceEvidence implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $deviceId,
        public array $posture,
        public string $region,
        public string $verifiedNetworkAddress,
        public int $verifiedAtUnixMs,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'deviceId', 'posture', 'region', 'verifiedNetworkAddress', 'verifiedAtUnixMs', 'expiresAtUnixMs']) || array_diff(['id', 'name', 'enabled', 'revision', 'deviceId', 'posture', 'region', 'verifiedNetworkAddress', 'verifiedAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['deviceId'],
            array_map(static fn ($item) => $item, $data['posture']),
            $data['region'],
            $data['verifiedNetworkAddress'],
            $data['verifiedAtUnixMs'],
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
