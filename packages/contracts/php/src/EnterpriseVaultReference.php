<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Visible secret name and group boundary; no plaintext or verifier bytes. */
final readonly class EnterpriseVaultReference implements \JsonSerializable {
    public function __construct(
        public string $name,
        public string $toolGroupId,
        public string $deviceGroupId
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['name', 'toolGroupId', 'deviceGroupId']) || array_diff(['name', 'toolGroupId', 'deviceGroupId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['name'],
            $data['toolGroupId'],
            $data['deviceGroupId']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
