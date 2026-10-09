<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Audited independent review of one bound obligation. */
final readonly class EnterpriseApprovalReview implements \JsonSerializable {
    public function __construct(
        public string $obligationId,
        public string $reviewerUserId,
        public EnterpriseReviewDecision $decision,
        public int $decidedAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['obligationId', 'reviewerUserId', 'decision', 'decidedAtUnixMs']) || array_diff(['obligationId', 'reviewerUserId', 'decision', 'decidedAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['obligationId'],
            $data['reviewerUserId'],
            EnterpriseReviewDecision::from($data['decision']),
            $data['decidedAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
