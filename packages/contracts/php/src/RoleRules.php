<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Fixed capability templates narrowed by device scope and optional tool allowlist. GROUPS requires nonempty group IDs; NONE and ALL require empty groups, validated by Control. Policies authorize each call. */
final readonly class RoleRules implements \JsonSerializable {
    public function __construct(
        public array $templateIds,
        public RoleDeviceScope $deviceScope,
        public array $deviceGroupIds,
        public array $toolIds
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['templateIds', 'deviceScope', 'deviceGroupIds', 'toolIds']) || array_diff(['templateIds', 'deviceScope', 'deviceGroupIds', 'toolIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            array_map(static fn ($item) => UserPrivilegeTemplate::from($item), $data['templateIds']),
            RoleDeviceScope::from($data['deviceScope']),
            array_map(static fn ($item) => $item, $data['deviceGroupIds']),
            array_map(static fn ($item) => $item, $data['toolIds'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
