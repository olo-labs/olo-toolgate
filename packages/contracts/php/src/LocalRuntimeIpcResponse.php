<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical redacted execution/status response for IPC revision 3. */
final readonly class LocalRuntimeIpcResponse implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public ?LocalRuntimeHealth $health = null,
        public ?LocalToolOutput $result = null,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'health', 'result', 'error']) || array_diff(['requestId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            array_key_exists('health', $data) ? LocalRuntimeHealth::fromArray($data['health']) : null,
            array_key_exists('result', $data) ? LocalToolOutput::fromArray($data['result']) : null,
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
