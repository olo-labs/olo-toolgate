<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Durable tenant approval status; identity/resource and digest only, never raw arguments. */
final readonly class ApprovalRecord implements \JsonSerializable {
    public function __construct(
        public string $id,
        public int $revision,
        public ApprovalState $state,
        public PolicyInput $input,
        public string $policyVersion,
        public int $createdAtUnixMs,
        public int $expiresAtUnixMs,
        public ?string $decidedBy = null,
        public ?int $decidedAtUnixMs = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'revision', 'state', 'input', 'policyVersion', 'createdAtUnixMs', 'expiresAtUnixMs', 'decidedBy', 'decidedAtUnixMs']) || array_diff(['id', 'revision', 'state', 'input', 'policyVersion', 'createdAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['revision'],
            ApprovalState::from($data['state']),
            PolicyInput::fromArray($data['input']),
            $data['policyVersion'],
            $data['createdAtUnixMs'],
            $data['expiresAtUnixMs'],
            array_key_exists('decidedBy', $data) ? $data['decidedBy'] : null,
            array_key_exists('decidedAtUnixMs', $data) ? $data['decidedAtUnixMs'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
