<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** V2 ASK outcome; pending ASK grants no execution; ALLOW for approved ASK requires a signed permit. */
final readonly class AuthorizationOutcome implements \JsonSerializable {
    public function __construct(
        public PolicyDecision $decision,
        public ?string $approvalId = null,
        public ?SignedExecutionPermit $permit = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['decision', 'approvalId', 'permit']) || array_diff(['decision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            PolicyDecision::fromArray($data['decision']),
            array_key_exists('approvalId', $data) ? $data['approvalId'] : null,
            array_key_exists('permit', $data) ? SignedExecutionPermit::fromArray($data['permit']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
