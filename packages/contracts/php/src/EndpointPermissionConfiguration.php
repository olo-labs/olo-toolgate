<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Server-cached complete replacement of device permissions, acknowledged by digest on the next authenticated poll. */
final readonly class EndpointPermissionConfiguration implements \JsonSerializable {
    public function __construct(
        public string $serverId,
        public string $deviceId,
        public string $userId,
        public int $revision,
        public string $digest,
        public array $permissions
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['serverId', 'deviceId', 'userId', 'revision', 'digest', 'permissions']) || array_diff(['serverId', 'deviceId', 'userId', 'revision', 'digest', 'permissions'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['serverId'],
            $data['deviceId'],
            $data['userId'],
            $data['revision'],
            $data['digest'],
            array_map(static fn ($item) => EndpointPermissionRule::fromArray($item), $data['permissions'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
