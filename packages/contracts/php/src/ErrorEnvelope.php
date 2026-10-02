<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Machine-readable error without exception text or caller-controlled detail. */
final readonly class ErrorEnvelope implements \JsonSerializable {
    public function __construct(
        public ErrorCode $code,
        public string $requestId,
        public bool $retryable
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['code', 'requestId', 'retryable']) || array_diff(['code', 'requestId', 'retryable'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ErrorCode::from($data['code']),
            $data['requestId'],
            $data['retryable']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
