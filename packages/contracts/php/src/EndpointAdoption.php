<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Authenticated device adoption metadata. No owner or per-agent permission cache; discovery and effects require current online authority. */
final readonly class EndpointAdoption implements \JsonSerializable {
    public function __construct(
        public string $serverId,
        public string $deviceId,
        public int $revision,
        public int $authorizationEpoch,
        public string $digest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['serverId', 'deviceId', 'revision', 'authorizationEpoch', 'digest']) || array_diff(['serverId', 'deviceId', 'revision', 'authorizationEpoch', 'digest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['serverId'],
            $data['deviceId'],
            $data['revision'],
            $data['authorizationEpoch'],
            $data['digest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
