<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Publish a consistent directory snapshot or roll back into a new sequence. Requires Idempotency-Key. */
final readonly class BundlePublishRequest implements \JsonSerializable {
    public function __construct(
        public int $directoryRevision,
        public int $expectedSequence,
        public int $lifetimeMs,
        public int $graceMs,
        public ?array $gracePolicyIds = null,
        public ?int $rollbackOf = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['directoryRevision', 'expectedSequence', 'lifetimeMs', 'graceMs', 'gracePolicyIds', 'rollbackOf']) || array_diff(['directoryRevision', 'expectedSequence', 'lifetimeMs', 'graceMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['directoryRevision'],
            $data['expectedSequence'],
            $data['lifetimeMs'],
            $data['graceMs'],
            array_key_exists('gracePolicyIds', $data) ? array_map(static fn ($item) => $item, $data['gracePolicyIds']) : null,
            array_key_exists('rollbackOf', $data) ? $data['rollbackOf'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
