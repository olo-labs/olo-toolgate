<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Versioned desired package assignments reconciled by an endpoint. */
final readonly class DesiredState implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public int $revision,
        public array $assignments
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'revision', 'assignments']) || array_diff(['deviceId', 'revision', 'assignments'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['revision'],
            array_map(static fn ($item) => DeploymentAssignment::fromArray($item), $data['assignments'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
