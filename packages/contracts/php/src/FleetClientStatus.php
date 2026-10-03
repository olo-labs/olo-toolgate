<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed fleet lifecycle contract; organization deployment trust never grants runtime permission. */
final readonly class FleetClientStatus implements \JsonSerializable {
    public function __construct(
        public int $generation,
        public bool $ready,
        public array $packages,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['generation', 'ready', 'packages', 'error']) || array_diff(['generation', 'ready', 'packages'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['generation'],
            $data['ready'],
            array_map(static fn ($item) => ReportedPackage::fromArray($item), $data['packages']),
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
