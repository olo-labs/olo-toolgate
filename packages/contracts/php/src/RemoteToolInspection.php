<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Same-tenant administrator inspection of one completed response; excludes arguments and private lease credentials. */
final readonly class RemoteToolInspection implements \JsonSerializable {
    public function __construct(
        public RemoteToolRecord $record,
        public ?\stdClass $output = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['record', 'output']) || array_diff(['record'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            RemoteToolRecord::fromArray($data['record']),
            array_key_exists('output', $data) ? (object) $data['output'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
