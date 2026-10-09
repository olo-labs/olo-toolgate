<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Group/typed-role sourced complete runtime capability; never an individual ACL. */
final readonly class ControlAccessGrant implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public EnterpriseSourceType $sourceType,
        public string $sourceId,
        public EnterpriseGrantPurpose $purpose,
        public EnterpriseScope $scope
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'sourceType', 'sourceId', 'purpose', 'scope']) || array_diff(['id', 'name', 'enabled', 'revision', 'sourceType', 'sourceId', 'purpose', 'scope'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            EnterpriseSourceType::from($data['sourceType']),
            $data['sourceId'],
            EnterpriseGrantPurpose::from($data['purpose']),
            EnterpriseScope::fromArray($data['scope'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
