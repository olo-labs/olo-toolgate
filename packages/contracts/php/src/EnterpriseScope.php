<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** A complete group/action/device/resource tuple; independent scopes are never multiplied. */
final readonly class EnterpriseScope implements \JsonSerializable {
    public function __construct(
        public GroupSelection $toolGroups,
        public GroupSelection $deviceGroups,
        public array $actions,
        public bool $allActions,
        public array $resources,
        public EnterpriseConditions $conditions
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['toolGroups', 'deviceGroups', 'actions', 'allActions', 'resources', 'conditions']) || array_diff(['toolGroups', 'deviceGroups', 'actions', 'allActions', 'resources', 'conditions'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            GroupSelection::fromArray($data['toolGroups']),
            GroupSelection::fromArray($data['deviceGroups']),
            array_map(static fn ($item) => $item, $data['actions']),
            $data['allActions'],
            array_map(static fn ($item) => EnterpriseResourceRule::fromArray($item), $data['resources']),
            EnterpriseConditions::fromArray($data['conditions'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
