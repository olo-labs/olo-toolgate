<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetDesiredDocument implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $tenantId,
        public string $serverId,
        public string $deviceId,
        public int $generation,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public array $assignments
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'tenantId', 'serverId', 'deviceId', 'generation', 'issuedAtUnixMs', 'expiresAtUnixMs', 'assignments']) || array_diff(['formatVersion', 'tenantId', 'serverId', 'deviceId', 'generation', 'issuedAtUnixMs', 'expiresAtUnixMs', 'assignments'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['tenantId'],
            $data['serverId'],
            $data['deviceId'],
            $data['generation'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            array_map(static fn ($item) => FleetAssignment::fromArray($item), $data['assignments'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
