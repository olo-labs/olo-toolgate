<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class ClientIpcResponse implements \JsonSerializable {
    public function __construct(
        public string $requestId,
        public ?ClientHealth $health = null,
        public ?EndpointEnrollmentPrompt $challenge = null,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['requestId', 'health', 'challenge', 'error']) || array_diff(['requestId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['requestId'],
            array_key_exists('health', $data) ? ClientHealth::fromArray($data['health']) : null,
            array_key_exists('challenge', $data) ? EndpointEnrollmentPrompt::fromArray($data['challenge']) : null,
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
