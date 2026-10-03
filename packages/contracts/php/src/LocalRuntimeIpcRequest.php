<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** OS-authorized additive IPC revision; callers cannot select runtime images, paths or launch arguments. */
final readonly class LocalRuntimeIpcRequest implements \JsonSerializable {
    public function __construct(
        public int $protocolVersion,
        public string $requestId,
        public LocalRuntimeOperation $operation,
        public ?LocalToolInput $invocation = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['protocolVersion', 'requestId', 'operation', 'invocation']) || array_diff(['protocolVersion', 'requestId', 'operation'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['protocolVersion'],
            $data['requestId'],
            LocalRuntimeOperation::from($data['operation']),
            array_key_exists('invocation', $data) ? LocalToolInput::fromArray($data['invocation']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
