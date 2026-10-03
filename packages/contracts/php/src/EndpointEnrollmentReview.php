<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class EndpointEnrollmentReview implements \JsonSerializable {
    public function __construct(
        public string $enrollmentId,
        public string $userCode,
        public string $deviceId,
        public ClientPlatform $platform,
        public string $keyFingerprint,
        public EnrollmentState $state,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['enrollmentId', 'userCode', 'deviceId', 'platform', 'keyFingerprint', 'state', 'expiresAtUnixMs']) || array_diff(['enrollmentId', 'userCode', 'deviceId', 'platform', 'keyFingerprint', 'state', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['enrollmentId'],
            $data['userCode'],
            $data['deviceId'],
            ClientPlatform::from($data['platform']),
            $data['keyFingerprint'],
            EnrollmentState::from($data['state']),
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
