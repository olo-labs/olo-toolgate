<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Gateway-only atomic lease consumption; repeat use never grants again. */
final readonly class ApprovalPermitUse implements \JsonSerializable {
    public function __construct(
        public string $approvalId,
        public string $permitId,
        public PolicyInput $input,
        public string $policyVersion
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['approvalId', 'permitId', 'input', 'policyVersion']) || array_diff(['approvalId', 'permitId', 'input', 'policyVersion'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['approvalId'],
            $data['permitId'],
            PolicyInput::fromArray($data['input']),
            $data['policyVersion']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
