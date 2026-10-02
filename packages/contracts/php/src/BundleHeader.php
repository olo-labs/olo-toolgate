<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Strict JWS protected header; no remote or embedded keys and no algorithm negotiation. */
final readonly class BundleHeader implements \JsonSerializable {
    public function __construct(
        public string $alg,
        public string $typ,
        public string $kid
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['alg', 'typ', 'kid']) || array_diff(['alg', 'typ', 'kid'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['alg'],
            $data['typ'],
            $data['kid']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
