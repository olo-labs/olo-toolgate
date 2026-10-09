<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Independent eligible checker decision against the exact durable reconciliation revision. */
final readonly class EnterpriseReconciliationDecision implements \JsonSerializable {
    public function __construct(
        public int $expectedRevision,
        public bool $approve
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['expectedRevision', 'approve']) || array_diff(['expectedRevision', 'approve'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['expectedRevision'],
            $data['approve']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
