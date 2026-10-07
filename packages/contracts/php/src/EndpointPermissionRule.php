<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Device-owner permission scope used only for local discovery; protected calls still require online authorization. */
final readonly class EndpointPermissionRule implements \JsonSerializable {
    public function __construct(
        public string $toolId,
        public string $action,
        public array $agentIds,
        public ResourceDescriptor $resource,
        public Decision $decision
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['toolId', 'action', 'agentIds', 'resource', 'decision']) || array_diff(['toolId', 'action', 'agentIds', 'resource', 'decision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['toolId'],
            $data['action'],
            array_map(static fn ($item) => $item, $data['agentIds']),
            ResourceDescriptor::fromArray($data['resource']),
            Decision::from($data['decision'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
