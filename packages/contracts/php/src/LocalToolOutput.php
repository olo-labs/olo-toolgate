<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** One bounded JSON stdout document; request binding and output schema are verified. */
final readonly class LocalToolOutput implements \JsonSerializable {
    public function __construct(
        public int $protocolVersion,
        public string $requestId,
        public \stdClass $output
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['protocolVersion', 'requestId', 'output']) || array_diff(['protocolVersion', 'requestId', 'output'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['protocolVersion'],
            $data['requestId'],
            (object) $data['output']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
