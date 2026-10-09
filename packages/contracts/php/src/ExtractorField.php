<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed JSON pointer to resources, including every batch member or source/destination. */
final readonly class ExtractorField implements \JsonSerializable {
    public function __construct(
        public string $pointer,
        public ResourceKind $kind,
        public bool $multiple
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['pointer', 'kind', 'multiple']) || array_diff(['pointer', 'kind', 'multiple'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['pointer'],
            ResourceKind::from($data['kind']),
            $data['multiple']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
