<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection. */
final readonly class ClientCommandActivity implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public int $startedAtUnixMs,
        public ClientCommandState $state,
        public ?int $finishedAtUnixMs = null,
        public ?int $progressPercent = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'startedAtUnixMs', 'finishedAtUnixMs', 'state', 'progressPercent']) || array_diff(['id', 'name', 'startedAtUnixMs', 'state'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['startedAtUnixMs'],
            ClientCommandState::from($data['state']),
            array_key_exists('finishedAtUnixMs', $data) ? $data['finishedAtUnixMs'] : null,
            array_key_exists('progressPercent', $data) ? $data['progressPercent'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
