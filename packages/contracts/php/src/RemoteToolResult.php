<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Exact leased execution response. Failed execution carries a sanitized error code. */
final readonly class RemoteToolResult implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public string $leaseId,
        public ?\stdClass $output = null,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'leaseId', 'output', 'error']) || array_diff(['requestId', 'leaseId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            $data['leaseId'],
            array_key_exists('output', $data) ? (object) $data['output'] : null,
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
