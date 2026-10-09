<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical complete resource constraint; ANY is explicit privileged scope. */
final readonly class EnterpriseResourceRule implements \JsonSerializable {
    public function __construct(
        public ResourceKind $kind,
        public string $locator,
        public EnterpriseResourceMatch $match
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['kind', 'locator', 'match']) || array_diff(['kind', 'locator', 'match'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ResourceKind::from($data['kind']),
            $data['locator'],
            EnterpriseResourceMatch::from($data['match'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
