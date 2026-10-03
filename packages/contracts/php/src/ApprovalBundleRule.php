<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Format 2 exact rule; BLOCK > ASK > ALLOW. ASK never uses grace. */
final readonly class ApprovalBundleRule implements \JsonSerializable {
    public function __construct(
        public string $policyId,
        public array $userIds,
        public array $agentIds,
        public array $deviceIds,
        public string $toolId,
        public string $action,
        public ResourceDescriptor $resource,
        public bool $graceAllowed,
        public Decision $effect
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
            Decision::from($data['effect'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
