<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Authenticated Control result for this exact Gateway attempt; lease fields occur only on a successful grant. */
final readonly class ApprovalResolution implements \JsonSerializable {
    public function __construct(
        public string $approvalId,
        public ApprovalState $state,
        public PolicyInput $input,
        public string $policyVersion,
        public ?string $permitId = null,
        public ?int $permitExpiresAtUnixMs = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['approvalId', 'state', 'input', 'policyVersion', 'permitId', 'permitExpiresAtUnixMs']) || array_diff(['approvalId', 'state', 'input', 'policyVersion'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['approvalId'],
            ApprovalState::from($data['state']),
            PolicyInput::fromArray($data['input']),
            $data['policyVersion'],
            array_key_exists('permitId', $data) ? $data['permitId'] : null,
            array_key_exists('permitExpiresAtUnixMs', $data) ? $data['permitExpiresAtUnixMs'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
