<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges. */
final readonly class BuilderDefinition implements \JsonSerializable {
    public function __construct(
        public string $packageId,
        public string $version,
        public string $name,
        public string $description,
        public string $useWhen,
        public string $doNotUseWhen,
        public ManagedRuntime $runtime,
        public LocalToolRegistration $tool,
        public array $platforms,
        public array $architectures,
        public array $examples,
        public array $permissions,
        public ResourceDescriptor $resource,
        public array $credentialRequirements
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['packageId', 'version', 'name', 'description', 'useWhen', 'doNotUseWhen', 'runtime', 'tool', 'platforms', 'architectures', 'examples', 'permissions', 'resource', 'credentialRequirements']) || array_diff(['packageId', 'version', 'name', 'description', 'useWhen', 'doNotUseWhen', 'runtime', 'tool', 'platforms', 'architectures', 'examples', 'permissions', 'resource', 'credentialRequirements'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['packageId'],
            $data['version'],
            $data['name'],
            $data['description'],
            $data['useWhen'],
            $data['doNotUseWhen'],
            ManagedRuntime::fromArray($data['runtime']),
            LocalToolRegistration::fromArray($data['tool']),
            array_map(static fn ($item) => ClientPlatform::from($item), $data['platforms']),
            array_map(static fn ($item) => FleetArchitecture::from($item), $data['architectures']),
            array_map(static fn ($item) => FleetSelfTest::fromArray($item), $data['examples']),
            array_map(static fn ($item) => BuilderPermission::from($item), $data['permissions']),
            ResourceDescriptor::fromArray($data['resource']),
            array_map(static fn ($item) => $item, $data['credentialRequirements'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
