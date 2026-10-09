<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** No grant is applied and no approval, quota or permit is consumed. */
final readonly class EnterpriseShadowResult implements \JsonSerializable {
    public function __construct(
        public EnterpriseDecision $decision,
        public Decision $observedLegacyDecision,
        public string $legacyEvidenceDigest,
        public string $proposedSnapshotDigest,
        public bool $accessExpansion
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['decision', 'observedLegacyDecision', 'legacyEvidenceDigest', 'proposedSnapshotDigest', 'accessExpansion']) || array_diff(['decision', 'observedLegacyDecision', 'legacyEvidenceDigest', 'proposedSnapshotDigest', 'accessExpansion'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseDecision::fromArray($data['decision']),
            Decision::from($data['observedLegacyDecision']),
            $data['legacyEvidenceDigest'],
            $data['proposedSnapshotDigest'],
            $data['accessExpansion']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
