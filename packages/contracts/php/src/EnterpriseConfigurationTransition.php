<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed configuration workflow bound to the exact group graph revision. */
final readonly class EnterpriseConfigurationTransition implements \JsonSerializable {
    public function __construct(
        public int $expectedRevision,
        public EnterpriseConfigurationAction $action
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['expectedRevision', 'action']) || array_diff(['expectedRevision', 'action'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['expectedRevision'],
            EnterpriseConfigurationAction::from($data['action'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
