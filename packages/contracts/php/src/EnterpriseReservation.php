<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Durable invocation reservation and its exact signed capability. */
final readonly class EnterpriseReservation implements \JsonSerializable {
    public function __construct(
        public EnterpriseInvocation $invocation,
        public EnterpriseSignedPermit $permit
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['invocation', 'permit']) || array_diff(['invocation', 'permit'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseInvocation::fromArray($data['invocation']),
            EnterpriseSignedPermit::fromArray($data['permit'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
