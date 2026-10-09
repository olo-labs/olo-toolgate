<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded downstream Agent Group delegation; every hop narrows capability. */
final readonly class ControlAgentDelegation implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $fromAgentGroupId,
        public string $toAgentGroupId,
        public EnterpriseScope $scope,
        public int $maximumDepth
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'fromAgentGroupId', 'toAgentGroupId', 'scope', 'maximumDepth']) || array_diff(['id', 'name', 'enabled', 'revision', 'fromAgentGroupId', 'toAgentGroupId', 'scope', 'maximumDepth'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['fromAgentGroupId'],
            $data['toAgentGroupId'],
            EnterpriseScope::fromArray($data['scope']),
            $data['maximumDepth']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
