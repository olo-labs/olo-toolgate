<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Certificate-authenticated current device acknowledgement. Acknowledgement is metadata, never an execution capability. */
final readonly class EnterpriseAdoptionStatus implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public int $directoryRevision,
        public int $authorizationEpoch,
        public string $graphDigest,
        public int $observedAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'directoryRevision', 'authorizationEpoch', 'graphDigest', 'observedAtUnixMs']) || array_diff(['deviceId', 'directoryRevision', 'authorizationEpoch', 'graphDigest', 'observedAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['directoryRevision'],
            $data['authorizationEpoch'],
            $data['graphDigest'],
            $data['observedAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
