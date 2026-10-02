<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Validate and diff before applying a bounded configuration transaction. */
final readonly class ControlImportRequest implements \JsonSerializable {
    public function __construct(
        public ControlSnapshot $snapshot,
        public ControlImportMode $mode,
        public bool $dryRun
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['snapshot', 'mode', 'dryRun']) || array_diff(['snapshot', 'mode', 'dryRun'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ControlSnapshot::fromArray($data['snapshot']),
            ControlImportMode::from($data['mode']),
            $data['dryRun']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
