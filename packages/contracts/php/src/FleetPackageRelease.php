<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetPackageRelease implements \JsonSerializable {
    public function __construct(
        public string $packageId,
        public string $version,
        public string $manifestDigest,
        public int $sizeBytes,
        public FleetSignedDocument $release
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['packageId', 'version', 'manifestDigest', 'sizeBytes', 'release']) || array_diff(['packageId', 'version', 'manifestDigest', 'sizeBytes', 'release'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['packageId'],
            $data['version'],
            $data['manifestDigest'],
            $data['sizeBytes'],
            FleetSignedDocument::fromArray($data['release'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
