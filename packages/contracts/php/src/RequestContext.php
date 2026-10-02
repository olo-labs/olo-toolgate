<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Safe correlation and principal identifiers; contains no credentials. */
final readonly class RequestContext implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public string $tenantId,
        public string $userId,
        public string $agentId,
        public ?string $deviceId = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'tenantId', 'userId', 'agentId', 'deviceId']) || array_diff(['requestId', 'tenantId', 'userId', 'agentId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            $data['tenantId'],
            $data['userId'],
            $data['agentId'],
            array_key_exists('deviceId', $data) ? $data['deviceId'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
