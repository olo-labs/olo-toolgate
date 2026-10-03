<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded local sandbox budget; limits never grant host access. */
final readonly class LocalRuntimeLimits implements \JsonSerializable {
    public function __construct(
        public int $timeoutMs,
        public int $memoryMiB,
        public int $maxInputBytes,
        public int $maxOutputBytes
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['timeoutMs', 'memoryMiB', 'maxInputBytes', 'maxOutputBytes']) || array_diff(['timeoutMs', 'memoryMiB', 'maxInputBytes', 'maxOutputBytes'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['timeoutMs'],
            $data['memoryMiB'],
            $data['maxInputBytes'],
            $data['maxOutputBytes']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
