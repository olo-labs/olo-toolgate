<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetRolloutRecord implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $packageId,
        public string $version,
        public bool $desiredPresence,
        public int $percentage,
        public int $revision,
        public int $createdAtUnixMs,
        public array $members
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'packageId', 'version', 'desiredPresence', 'percentage', 'revision', 'createdAtUnixMs', 'members']) || array_diff(['id', 'packageId', 'version', 'desiredPresence', 'percentage', 'revision', 'createdAtUnixMs', 'members'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['packageId'],
            $data['version'],
            $data['desiredPresence'],
            $data['percentage'],
            $data['revision'],
            $data['createdAtUnixMs'],
            array_map(static fn ($item) => FleetRolloutMember::fromArray($item), $data['members'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
