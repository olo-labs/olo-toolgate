<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetRolloutStatus implements \JsonSerializable {
    public function __construct(
        public FleetRolloutRecord $rollout,
        public int $ready,
        public int $failed,
        public int $offline,
        public int $waiting,
        public int $pending,
        public int $superseded
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['rollout', 'ready', 'failed', 'offline', 'waiting', 'pending', 'superseded']) || array_diff(['rollout', 'ready', 'failed', 'offline', 'waiting', 'pending', 'superseded'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            FleetRolloutRecord::fromArray($data['rollout']),
            $data['ready'],
            $data['failed'],
            $data['offline'],
            $data['waiting'],
            $data['pending'],
            $data['superseded']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
