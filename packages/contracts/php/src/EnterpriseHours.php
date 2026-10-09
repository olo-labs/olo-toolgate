<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Trusted UTC window; end must exceed start. */
final readonly class EnterpriseHours implements \JsonSerializable {
    public function __construct(
        public int $dayOfWeek,
        public int $startMinute,
        public int $endMinute
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['dayOfWeek', 'startMinute', 'endMinute']) || array_diff(['dayOfWeek', 'startMinute', 'endMinute'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['dayOfWeek'],
            $data['startMinute'],
            $data['endMinute']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
