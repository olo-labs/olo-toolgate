<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Immutable artifact identity; digest must be verified by consumers. */
final readonly class ArtifactDescriptor implements \JsonSerializable {
    public function __construct(
        public string $uri,
        public string $sha256,
        public int $sizeBytes
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['uri', 'sha256', 'sizeBytes']) || array_diff(['uri', 'sha256', 'sizeBytes'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['uri'],
            $data['sha256'],
            $data['sizeBytes']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
