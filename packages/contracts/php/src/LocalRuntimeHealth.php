<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded per-service execution counters and sandbox state. */
final readonly class LocalRuntimeHealth implements \JsonSerializable {
    public function __construct(
        public bool $ready,
        public int $successfulExecutions,
        public int $failedExecutions,
        public array $runtimes
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['ready', 'successfulExecutions', 'failedExecutions', 'runtimes']) || array_diff(['ready', 'successfulExecutions', 'failedExecutions', 'runtimes'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['ready'],
            $data['successfulExecutions'],
            $data['failedExecutions'],
            array_map(static fn ($item) => LocalRuntimeStatus::fromArray($item), $data['runtimes'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
