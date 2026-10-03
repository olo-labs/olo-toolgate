<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetRolloutRequest implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $packageId,
        public string $version,
        public array $deviceIds,
        public bool $desiredPresence,
        public int $percentage
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'packageId', 'version', 'deviceIds', 'desiredPresence', 'percentage']) || array_diff(['id', 'packageId', 'version', 'deviceIds', 'desiredPresence', 'percentage'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['packageId'],
            $data['version'],
            array_map(static fn ($item) => $item, $data['deviceIds']),
            $data['desiredPresence'],
            $data['percentage']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
