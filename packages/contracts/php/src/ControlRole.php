<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Typed role assigned only to compatible groups. Management rules cannot confer runtime access. */
final readonly class ControlRole implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public UserRole $portalRole,
        public EnterpriseRoleType $roleType,
        public array $managementRules
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'portalRole', 'roleType', 'managementRules']) || array_diff(['id', 'name', 'enabled', 'revision', 'portalRole', 'roleType', 'managementRules'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            UserRole::from($data['portalRole']),
            EnterpriseRoleType::from($data['roleType']),
            array_map(static fn ($item) => EnterpriseManagementRule::fromArray($item), $data['managementRules'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
