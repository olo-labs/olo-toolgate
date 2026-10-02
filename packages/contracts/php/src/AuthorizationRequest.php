<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Runtime request; principal context and resource identity are derived by the gateway, never asserted by the caller. */
final readonly class AuthorizationRequest implements \JsonSerializable {
    public function __construct(
        public string $toolId,
        public string $action,
        public \stdClass $arguments
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['toolId', 'action', 'arguments']) || array_diff(['toolId', 'action', 'arguments'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['toolId'],
            $data['action'],
            (object) $data['arguments']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
