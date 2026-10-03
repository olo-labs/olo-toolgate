<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class EndpointEnrollmentChallenge implements \JsonSerializable {
    public function __construct(
        public string $enrollmentId,
        public string $deviceCode,
        public string $userCode,
        public string $verificationUri,
        public int $expiresAtUnixMs,
        public int $pollIntervalSeconds
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['enrollmentId', 'deviceCode', 'userCode', 'verificationUri', 'expiresAtUnixMs', 'pollIntervalSeconds']) || array_diff(['enrollmentId', 'deviceCode', 'userCode', 'verificationUri', 'expiresAtUnixMs', 'pollIntervalSeconds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['enrollmentId'],
            $data['deviceCode'],
            $data['userCode'],
            $data['verificationUri'],
            $data['expiresAtUnixMs'],
            $data['pollIntervalSeconds']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
