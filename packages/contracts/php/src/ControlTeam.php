<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Tenant-scoped teams configuration record. Not a runtime credential or policy grant. */
final readonly class ControlTeam implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public array $userIds,
        public ?array $deviceIds = null,
        public ?array $roleIds = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'userIds', 'deviceIds', 'roleIds']) || array_diff(['id', 'name', 'enabled', 'revision', 'userIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            array_map(static fn ($item) => $item, $data['userIds']),
            array_key_exists('deviceIds', $data) ? array_map(static fn ($item) => $item, $data['deviceIds']) : null,
            array_key_exists('roleIds', $data) ? array_map(static fn ($item) => $item, $data['roleIds']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
