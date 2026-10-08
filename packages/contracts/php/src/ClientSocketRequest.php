<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Correlated bounded request; body is validated again as the selected operation contract. */
final readonly class ClientSocketRequest implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public ClientSocketOperation $operation,
        public ?\stdClass $body = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'operation', 'body']) || array_diff(['requestId', 'operation'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            ClientSocketOperation::from($data['operation']),
            array_key_exists('body', $data) ? (object) $data['body'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
