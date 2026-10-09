<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** All present conditions accumulate. Missing trusted evidence denies. */
final readonly class EnterpriseConditions implements \JsonSerializable {
    public function __construct(
        public int $notBeforeUnixMs,
        public int $expiresAtUnixMs,
        public array $networkCidrs,
        public array $devicePosture,
        public array $regions,
        public array $hoursUtc,
        public bool $requireOnline,
        public bool $highRisk,
        public ?int $maxAmountMinorUnits = null,
        public ?int $maxInvocationsPerMinute = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['notBeforeUnixMs', 'expiresAtUnixMs', 'networkCidrs', 'devicePosture', 'regions', 'hoursUtc', 'requireOnline', 'highRisk', 'maxAmountMinorUnits', 'maxInvocationsPerMinute']) || array_diff(['notBeforeUnixMs', 'expiresAtUnixMs', 'networkCidrs', 'devicePosture', 'regions', 'hoursUtc', 'requireOnline', 'highRisk'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['notBeforeUnixMs'],
            $data['expiresAtUnixMs'],
            array_map(static fn ($item) => $item, $data['networkCidrs']),
            array_map(static fn ($item) => $item, $data['devicePosture']),
            array_map(static fn ($item) => $item, $data['regions']),
            array_map(static fn ($item) => EnterpriseHours::fromArray($item), $data['hoursUtc']),
            $data['requireOnline'],
            $data['highRisk'],
            array_key_exists('maxAmountMinorUnits', $data) ? $data['maxAmountMinorUnits'] : null,
            array_key_exists('maxInvocationsPerMinute', $data) ? $data['maxInvocationsPerMinute'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
