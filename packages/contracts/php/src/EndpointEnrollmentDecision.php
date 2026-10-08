<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class EndpointEnrollmentDecision implements \JsonSerializable {
    public function __construct(
        public string $userCode,
        public string $keyFingerprint,
        public EnrollmentChoice $choice,
        public ?int $connectionExpiresAtUnixMs = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['userCode', 'keyFingerprint', 'choice', 'connectionExpiresAtUnixMs']) || array_diff(['userCode', 'keyFingerprint', 'choice'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['userCode'],
            $data['keyFingerprint'],
            EnrollmentChoice::from($data['choice']),
            array_key_exists('connectionExpiresAtUnixMs', $data) ? $data['connectionExpiresAtUnixMs'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
