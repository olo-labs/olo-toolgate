<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Independent decision for one exact operation obligation and approval revision. */
final readonly class EnterpriseApprovalDecisionRequest implements \JsonSerializable {
    public function __construct(
        public int $expectedRevision,
        public string $obligationId,
        public EnterpriseReviewDecision $decision
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['expectedRevision', 'obligationId', 'decision']) || array_diff(['expectedRevision', 'obligationId', 'decision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['expectedRevision'],
            $data['obligationId'],
            EnterpriseReviewDecision::from($data['decision'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
