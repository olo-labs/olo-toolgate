<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Portable package metadata. Credential values are forbidden. */
final readonly class PackageManifest implements \JsonSerializable {
    public function __construct(
        public int $schemaVersion,
        public string $id,
        public string $version,
        public array $tools,
        public array $artifacts,
        public array $credentialReferences,
        public PackageCompatibility $compatibility
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['schemaVersion', 'id', 'version', 'tools', 'artifacts', 'credentialReferences', 'compatibility']) || array_diff(['schemaVersion', 'id', 'version', 'tools', 'artifacts', 'credentialReferences', 'compatibility'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['schemaVersion'],
            $data['id'],
            $data['version'],
            array_map(static fn ($item) => ToolDefinition::fromArray($item), $data['tools']),
            array_map(static fn ($item) => ArtifactDescriptor::fromArray($item), $data['artifacts']),
            array_map(static fn ($item) => $item, $data['credentialReferences']),
            PackageCompatibility::fromArray($data['compatibility'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
