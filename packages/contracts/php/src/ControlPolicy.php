<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Tenant-scoped policies configuration record. Not a runtime credential or policy grant. */
final readonly class ControlPolicy implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $toolId,
        public string $action,
        public ResourceDescriptor $resource,
        public Decision $decision,
        public array $userIds,
        public array $teamIds,
        public array $agentIds,
        public array $deviceIds
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'toolId', 'action', 'resource', 'decision', 'userIds', 'teamIds', 'agentIds', 'deviceIds']) || array_diff(['id', 'name', 'enabled', 'revision', 'toolId', 'action', 'resource', 'decision', 'userIds', 'teamIds', 'agentIds', 'deviceIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['toolId'],
            $data['action'],
            ResourceDescriptor::fromArray($data['resource']),
            Decision::from($data['decision']),
            array_map(static fn ($item) => $item, $data['userIds']),
            array_map(static fn ($item) => $item, $data['teamIds']),
            array_map(static fn ($item) => $item, $data['agentIds']),
            array_map(static fn ($item) => $item, $data['deviceIds'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
