<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class EndpointCheckInAck implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public int $sequence,
        public int $serverTimeUnixMs,
        public int $nextIntervalSeconds,
        public ?int $nextIntervalMs = null,
        public ?DeviceIdentity $identity = null,
        public ?EndpointPermissionConfiguration $configuration = null,
        public ?RemoteToolTask $task = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'sequence', 'serverTimeUnixMs', 'nextIntervalSeconds', 'nextIntervalMs', 'identity', 'configuration', 'task']) || array_diff(['deviceId', 'sequence', 'serverTimeUnixMs', 'nextIntervalSeconds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['sequence'],
            $data['serverTimeUnixMs'],
            $data['nextIntervalSeconds'],
            array_key_exists('nextIntervalMs', $data) ? $data['nextIntervalMs'] : null,
            array_key_exists('identity', $data) ? DeviceIdentity::fromArray($data['identity']) : null,
            array_key_exists('configuration', $data) ? EndpointPermissionConfiguration::fromArray($data['configuration']) : null,
            array_key_exists('task', $data) ? RemoteToolTask::fromArray($data['task']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
