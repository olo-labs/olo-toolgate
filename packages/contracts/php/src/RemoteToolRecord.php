<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Request progress visible to an administrator without arguments or results. */
final readonly class RemoteToolRecord implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public string $deviceId,
        public string $toolId,
        public RemoteToolState $state,
        public int $receivedAtUnixMs,
        public int $expiresAtUnixMs,
        public ?string $agentId = null,
        public ?int $submittedAtUnixMs = null,
        public ?int $responseAtUnixMs = null,
        public ?int $completedAtUnixMs = null,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'deviceId', 'agentId', 'toolId', 'state', 'receivedAtUnixMs', 'expiresAtUnixMs', 'submittedAtUnixMs', 'responseAtUnixMs', 'completedAtUnixMs', 'error']) || array_diff(['requestId', 'deviceId', 'toolId', 'state', 'receivedAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            $data['deviceId'],
            $data['toolId'],
            RemoteToolState::from($data['state']),
            $data['receivedAtUnixMs'],
            $data['expiresAtUnixMs'],
            array_key_exists('agentId', $data) ? $data['agentId'] : null,
            array_key_exists('submittedAtUnixMs', $data) ? $data['submittedAtUnixMs'] : null,
            array_key_exists('responseAtUnixMs', $data) ? $data['responseAtUnixMs'] : null,
            array_key_exists('completedAtUnixMs', $data) ? $data['completedAtUnixMs'] : null,
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
