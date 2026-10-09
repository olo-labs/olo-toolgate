<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Initial tenant installation has one pinned installation authority. Existing tenant recovery requires at least two distinct pinned independent signing authorities. */
final readonly class EnterpriseReviewedRecovery implements \JsonSerializable {
    public function __construct(
        public EnterpriseRecoveryAuthorization $authorization,
        public array $proofs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['authorization', 'proofs']) || array_diff(['authorization', 'proofs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseRecoveryAuthorization::fromArray($data['authorization']),
            array_map(static fn ($item) => EnterpriseRecoveryProof::fromArray($item), $data['proofs'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
