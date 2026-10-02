<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Exact tenant-scoped compiled policy; empty identity dimensions are unrestricted. BLOCK has precedence. */
final readonly class BundleRule implements \JsonSerializable {
    public function __construct(
        public string $policyId,
        public array $userIds,
        public array $agentIds,
        public array $deviceIds,
        public string $toolId,
        public string $action,
        public ResourceDescriptor $resource,
        public bool $graceAllowed,
        public BundleEffect $effect
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['policyId', 'userIds', 'agentIds', 'deviceIds', 'toolId', 'action', 'resource', 'graceAllowed', 'effect']) || array_diff(['policyId', 'userIds', 'agentIds', 'deviceIds', 'toolId', 'action', 'resource', 'graceAllowed', 'effect'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['policyId'],
            array_map(static fn ($item) => $item, $data['userIds']),
            array_map(static fn ($item) => $item, $data['agentIds']),
            array_map(static fn ($item) => $item, $data['deviceIds']),
            $data['toolId'],
            $data['action'],
            ResourceDescriptor::fromArray($data['resource']),
            $data['graceAllowed'],
            BundleEffect::from($data['effect'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
