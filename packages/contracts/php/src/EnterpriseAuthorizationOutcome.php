<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Online authoritative invocation status. Only a reserved signed permit can proceed to device-authenticated consumption. */
final readonly class EnterpriseAuthorizationOutcome implements \JsonSerializable {
    public function __construct(
        public EnterpriseInvocation $invocation,
        public ?EnterpriseReservation $reservation = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['invocation', 'reservation']) || array_diff(['invocation'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseInvocation::fromArray($data['invocation']),
            array_key_exists('reservation', $data) ? EnterpriseReservation::fromArray($data['reservation']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
