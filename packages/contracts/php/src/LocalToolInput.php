<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** One JSON stdin document; arguments never become process command strings. */
final readonly class LocalToolInput implements \JsonSerializable {
    public function __construct(
        public int $protocolVersion,
        public string $requestId,
        public string $toolId,
        public \stdClass $arguments
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['protocolVersion', 'requestId', 'toolId', 'arguments']) || array_diff(['protocolVersion', 'requestId', 'toolId', 'arguments'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['protocolVersion'],
            $data['requestId'],
            $data['toolId'],
            (object) $data['arguments']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
