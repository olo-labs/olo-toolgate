<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges. */
final readonly class BuilderTestTask implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $tenantId,
        public string $serverId,
        public string $deviceId,
        public BuilderTestRecord $job,
        public BuilderDefinition $definition,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'tenantId', 'serverId', 'deviceId', 'job', 'definition', 'expiresAtUnixMs']) || array_diff(['formatVersion', 'tenantId', 'serverId', 'deviceId', 'job', 'definition', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['tenantId'],
            $data['serverId'],
            $data['deviceId'],
            BuilderTestRecord::fromArray($data['job']),
            BuilderDefinition::fromArray($data['definition']),
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
