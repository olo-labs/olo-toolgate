<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges. */
final readonly class BuilderDraftRequest implements \JsonSerializable {
    public function __construct(
        public string $id,
        public int $expectedRevision,
        public BuilderDefinition $definition
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'expectedRevision', 'definition']) || array_diff(['id', 'expectedRevision', 'definition'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['expectedRevision'],
            BuilderDefinition::fromArray($data['definition'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
