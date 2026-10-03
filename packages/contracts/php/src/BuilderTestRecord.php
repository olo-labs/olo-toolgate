<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges. */
final readonly class BuilderTestRecord implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $draftId,
        public string $definitionDigest,
        public string $deviceId,
        public BuilderTestState $state,
        public int $revision,
        public int $createdAtUnixMs,
        public int $expiresAtUnixMs,
        public int $attempt,
        public string $leaseId,
        public int $exampleIndex,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'draftId', 'definitionDigest', 'deviceId', 'state', 'revision', 'createdAtUnixMs', 'expiresAtUnixMs', 'attempt', 'leaseId', 'exampleIndex', 'error']) || array_diff(['id', 'draftId', 'definitionDigest', 'deviceId', 'state', 'revision', 'createdAtUnixMs', 'expiresAtUnixMs', 'attempt', 'leaseId', 'exampleIndex'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['draftId'],
            $data['definitionDigest'],
            $data['deviceId'],
            BuilderTestState::from($data['state']),
            $data['revision'],
            $data['createdAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['attempt'],
            $data['leaseId'],
            $data['exampleIndex'],
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
