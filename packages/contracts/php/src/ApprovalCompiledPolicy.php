<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Format 2 adds human approval without weakening default deny. */
final readonly class ApprovalCompiledPolicy implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public array $rules
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'rules']) || array_diff(['formatVersion', 'rules'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            array_map(static fn ($item) => ApprovalBundleRule::fromArray($item), $data['rules'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
