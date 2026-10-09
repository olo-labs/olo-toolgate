<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Read-only comparison to a captured immutable legacy result; never a second runtime authority. */
final readonly class EnterpriseShadowRequest implements \JsonSerializable {
    public function __construct(
        public ControlSnapshot $snapshot,
        public EnterpriseEvaluation $evaluation,
        public Decision $observedLegacyDecision,
        public string $legacyEvidenceDigest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['snapshot', 'evaluation', 'observedLegacyDecision', 'legacyEvidenceDigest']) || array_diff(['snapshot', 'evaluation', 'observedLegacyDecision', 'legacyEvidenceDigest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ControlSnapshot::fromArray($data['snapshot']),
            EnterpriseEvaluation::fromArray($data['evaluation']),
            Decision::from($data['observedLegacyDecision']),
            $data['legacyEvidenceDigest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
