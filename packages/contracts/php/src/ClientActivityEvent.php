<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection. */
final readonly class ClientActivityEvent implements \JsonSerializable {
    public function __construct(
        public int $timestampUnixMs,
        public string $name,
        public string $state
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['timestampUnixMs', 'name', 'state']) || array_diff(['timestampUnixMs', 'name', 'state'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['timestampUnixMs'],
            $data['name'],
            $data['state']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
