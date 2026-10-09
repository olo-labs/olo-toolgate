<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Delegated human operation requires grant and delegation through this same Team and capability through this same Agent Group. */
final readonly class ControlDelegation implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $teamId,
        public string $agentGroupId,
        public EnterpriseScope $scope
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'teamId', 'agentGroupId', 'scope']) || array_diff(['id', 'name', 'enabled', 'revision', 'teamId', 'agentGroupId', 'scope'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['teamId'],
            $data['agentGroupId'],
            EnterpriseScope::fromArray($data['scope'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
