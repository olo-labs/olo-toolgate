<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Exactly one primary membership for each Tool; execution bindings are group-level. */
final readonly class ControlToolGroup implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public array $toolIds
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'toolIds']) || array_diff(['id', 'name', 'enabled', 'revision', 'toolIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            array_map(static fn ($item) => $item, $data['toolIds'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
