<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Gateway-private queued request/result response. */
final readonly class RemoteToolResponse implements \JsonSerializable {
    public function __construct(
        public RemoteToolRecord $record,
        public ?RemoteToolResult $result = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['record', 'result']) || array_diff(['record'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            RemoteToolRecord::fromArray($data['record']),
            array_key_exists('result', $data) ? RemoteToolResult::fromArray($data['result']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
