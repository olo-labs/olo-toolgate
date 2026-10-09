<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Device-bound durable effect outcome and completed resource subset. */
final readonly class EnterpriseEffectReport implements \JsonSerializable {
    public function __construct(
        public string $invocationId,
        public int $expectedRevision,
        public EnterpriseInvocationState $state,
        public array $completedResources,
        public ?string $resultDigest = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['invocationId', 'expectedRevision', 'state', 'completedResources', 'resultDigest']) || array_diff(['invocationId', 'expectedRevision', 'state', 'completedResources'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['invocationId'],
            $data['expectedRevision'],
            EnterpriseInvocationState::from($data['state']),
            array_map(static fn ($item) => ResourceDescriptor::fromArray($item), $data['completedResources']),
            array_key_exists('resultDigest', $data) ? $data['resultDigest'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
