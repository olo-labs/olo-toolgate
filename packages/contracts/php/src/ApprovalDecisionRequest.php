<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Optimistic human decision; duration is required only for temporary approval. */
final readonly class ApprovalDecisionRequest implements \JsonSerializable {
    public function __construct(
        public ApprovalChoice $decision,
        public int $expectedRevision,
        public ?int $durationMs = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['decision', 'expectedRevision', 'durationMs']) || array_diff(['decision', 'expectedRevision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ApprovalChoice::from($data['decision']),
            $data['expectedRevision'],
            array_key_exists('durationMs', $data) ? $data['durationMs'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
