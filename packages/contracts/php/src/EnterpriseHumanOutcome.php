<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Pending approvals carry no queued task or effect capability. Dispatched operations include the durable relay status. */
final readonly class EnterpriseHumanOutcome implements \JsonSerializable {
    public function __construct(
        public EnterpriseInvocation $invocation,
        public ?RemoteToolResponse $dispatch = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['invocation', 'dispatch']) || array_diff(['invocation'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseInvocation::fromArray($data['invocation']),
            array_key_exists('dispatch', $data) ? RemoteToolResponse::fromArray($data['dispatch']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
