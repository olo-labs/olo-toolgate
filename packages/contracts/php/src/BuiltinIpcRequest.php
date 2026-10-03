<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Fixed service tool boundary; validate schema before use. */
final readonly class BuiltinIpcRequest implements \JsonSerializable {
    public function __construct(
        public int $protocolVersion,
        public string $requestId,
        public BuiltinOperation $operation,
        public ?BuiltinInvocation $invocation = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['protocolVersion', 'requestId', 'operation', 'invocation']) || array_diff(['protocolVersion', 'requestId', 'operation'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['protocolVersion'],
            $data['requestId'],
            BuiltinOperation::from($data['operation']),
            array_key_exists('invocation', $data) ? BuiltinInvocation::fromArray($data['invocation']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
