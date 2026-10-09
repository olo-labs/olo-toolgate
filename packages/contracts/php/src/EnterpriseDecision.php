<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Deterministic decision, safe reasons, complete witnesses and accumulated obligations. */
final readonly class EnterpriseDecision implements \JsonSerializable {
    public function __construct(
        public Decision $decision,
        public EnterpriseDecisionReason $reason,
        public array $witnesses,
        public array $obligations,
        public int $revision,
        public int $authorizationEpoch,
        public int $validUntilUnixMs,
        public string $diagnosticId
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['decision', 'reason', 'witnesses', 'obligations', 'revision', 'authorizationEpoch', 'validUntilUnixMs', 'diagnosticId']) || array_diff(['decision', 'reason', 'witnesses', 'obligations', 'revision', 'authorizationEpoch', 'validUntilUnixMs', 'diagnosticId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            Decision::from($data['decision']),
            EnterpriseDecisionReason::from($data['reason']),
            array_map(static fn ($item) => EnterpriseWitness::fromArray($item), $data['witnesses']),
            array_map(static fn ($item) => $item, $data['obligations']),
            $data['revision'],
            $data['authorizationEpoch'],
            $data['validUntilUnixMs'],
            $data['diagnosticId']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
