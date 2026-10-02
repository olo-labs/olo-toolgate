<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Import validation/diff result with the current tenant revision. */
final readonly class ControlImportResult implements \JsonSerializable {
    public function __construct(
        public bool $applied,
        public int $revision,
        public array $changes
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['applied', 'revision', 'changes']) || array_diff(['applied', 'revision', 'changes'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['applied'],
            $data['revision'],
            array_map(static fn ($item) => ControlChange::fromArray($item), $data['changes'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
