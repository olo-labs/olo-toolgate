<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Encrypted secret custody bound to explicit Tool and Device Groups; plaintext never appears in directory export or audit. */
final readonly class EnterpriseVaultWrite implements \JsonSerializable {
    public function __construct(
        public string $name,
        public string $value,
        public string $toolGroupId,
        public string $deviceGroupId
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['name', 'value', 'toolGroupId', 'deviceGroupId']) || array_diff(['name', 'value', 'toolGroupId', 'deviceGroupId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['name'],
            $data['value'],
            $data['toolGroupId'],
            $data['deviceGroupId']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
