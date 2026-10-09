<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Verified human selects an exact execution binding and target. User and session facts are always supplied by the authenticated adapter. */
final readonly class EnterpriseHumanRequest implements \JsonSerializable {
    public function __construct(
        public string $bindingId,
        public string $deviceId,
        public AuthorizationRequest $request
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['bindingId', 'deviceId', 'request']) || array_diff(['bindingId', 'deviceId', 'request'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['bindingId'],
            $data['deviceId'],
            AuthorizationRequest::fromArray($data['request'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
