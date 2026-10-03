<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Redacted runtime readiness and capability, never engine output or credential contents. */
final readonly class LocalRuntimeStatus implements \JsonSerializable {
    public function __construct(
        public string $runtimeId,
        public LocalRuntimeKind $kind,
        public LocalRuntimeState $state,
        public string $version
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['runtimeId', 'kind', 'state', 'version']) || array_diff(['runtimeId', 'kind', 'state', 'version'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['runtimeId'],
            LocalRuntimeKind::from($data['kind']),
            LocalRuntimeState::from($data['state']),
            $data['version']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
