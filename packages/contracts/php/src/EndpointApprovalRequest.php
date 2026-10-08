<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reversible approval decision for an already enrolled key, preserving its owner and directory status. */
final readonly class EndpointApprovalRequest implements \JsonSerializable {
    public function __construct(
        public int $expectedApprovalRevision,
        public bool $approved,
        public ?int $connectionExpiresAtUnixMs = null,
        public ?bool $unlimitedConnection = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['expectedApprovalRevision', 'approved', 'connectionExpiresAtUnixMs', 'unlimitedConnection']) || array_diff(['expectedApprovalRevision', 'approved'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['expectedApprovalRevision'],
            $data['approved'],
            array_key_exists('connectionExpiresAtUnixMs', $data) ? $data['connectionExpiresAtUnixMs'] : null,
            array_key_exists('unlimitedConnection', $data) ? $data['unlimitedConnection'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
