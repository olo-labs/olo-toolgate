<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Dedicated Gateway-authenticated request for one device-local tool. */
final readonly class RemoteToolSubmission implements \JsonSerializable {
    public function __construct(
        public PolicyInput $input,
        public AuthorizationRequest $request,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['input', 'request', 'expiresAtUnixMs']) || array_diff(['input', 'request', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            PolicyInput::fromArray($data['input']),
            AuthorizationRequest::fromArray($data['request']),
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
