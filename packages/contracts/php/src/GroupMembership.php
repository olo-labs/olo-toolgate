<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Atomic complete mandatory membership replacement guarded by directory revision. */
final readonly class GroupMembership implements \JsonSerializable {
    public function __construct(
        public EnterpriseMemberType $entityType,
        public string $entityId,
        public array $groupIds,
        public int $revision
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['entityType', 'entityId', 'groupIds', 'revision']) || array_diff(['entityType', 'entityId', 'groupIds', 'revision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseMemberType::from($data['entityType']),
            $data['entityId'],
            array_map(static fn ($item) => $item, $data['groupIds']),
            $data['revision']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
