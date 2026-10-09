<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Tenant-scoped tools configuration record. Not a runtime credential or policy grant. */
final readonly class ControlTool implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public ToolDefinition $definition,
        public string $extractorId,
        public string $version,
        public string $packageDigest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'definition', 'extractorId', 'version', 'packageDigest']) || array_diff(['id', 'name', 'enabled', 'revision', 'definition', 'extractorId', 'version', 'packageDigest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            ToolDefinition::fromArray($data['definition']),
            $data['extractorId'],
            $data['version'],
            $data['packageDigest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
