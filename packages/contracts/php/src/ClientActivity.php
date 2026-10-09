<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Bounded redacted device activity for OS-authenticated local status inspection. */
final readonly class ClientActivity implements \JsonSerializable {
    public function __construct(
        public array $active,
        public array $events,
        public bool $logAvailable,
        public ?ClientCommandActivity $lastCommand = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['active', 'lastCommand', 'events', 'logAvailable']) || array_diff(['active', 'events', 'logAvailable'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            array_map(static fn ($item) => ClientCommandActivity::fromArray($item), $data['active']),
            array_map(static fn ($item) => ClientActivityEvent::fromArray($item), $data['events']),
            $data['logAvailable'],
            array_key_exists('lastCommand', $data) ? ClientCommandActivity::fromArray($data['lastCommand']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
