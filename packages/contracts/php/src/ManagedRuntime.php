<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Administrator-selected immutable tool/runtime image; runtime provisioning is not execution authorization. */
final readonly class ManagedRuntime implements \JsonSerializable {
    public function __construct(
        public string $id,
        public LocalRuntimeKind $kind,
        public string $image,
        public string $version
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'kind', 'image', 'version']) || array_diff(['id', 'kind', 'image', 'version'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            LocalRuntimeKind::from($data['kind']),
            $data['image'],
            $data['version']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
