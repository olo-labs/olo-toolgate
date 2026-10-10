<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** One gateway connection held by the device service; the focused connection serves local tool commands. */
final readonly class ClientGatewayConnection implements \JsonSerializable {
    public function __construct(
        public string $serverUrl,
        public bool $focused,
        public ClientHealth $health,
        public ?string $serverName = null,
        public ?ClientActivity $activity = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['serverUrl', 'serverName', 'focused', 'health', 'activity']) || array_diff(['serverUrl', 'focused', 'health'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['serverUrl'],
            $data['focused'],
            ClientHealth::fromArray($data['health']),
            array_key_exists('serverName', $data) ? $data['serverName'] : null,
            array_key_exists('activity', $data) ? ClientActivity::fromArray($data['activity']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
