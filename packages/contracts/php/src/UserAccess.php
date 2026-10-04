<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Tenant-scoped role and privilege assignment. Device groups are directory teams containing device IDs. */
final readonly class UserAccess implements \JsonSerializable {
    public function __construct(
        public UserRole $role,
        public array $templateIds,
        public array $deviceGroupIds,
        public ?array $roleIds = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['role', 'templateIds', 'deviceGroupIds', 'roleIds']) || array_diff(['role', 'templateIds', 'deviceGroupIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            UserRole::from($data['role']),
            array_map(static fn ($item) => UserPrivilegeTemplate::from($item), $data['templateIds']),
            array_map(static fn ($item) => $item, $data['deviceGroupIds']),
            array_key_exists('roleIds', $data) ? array_map(static fn ($item) => $item, $data['roleIds']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
