<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Canonical bounded authoring protocol; declarations never grant execution privileges. */
final readonly class BuilderTestResult implements \JsonSerializable {
    public function __construct(
        public string $jobId,
        public string $leaseId,
        public string $definitionDigest,
        public bool $success,
        public ?ErrorCode $error = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['jobId', 'leaseId', 'definitionDigest', 'success', 'error']) || array_diff(['jobId', 'leaseId', 'definitionDigest', 'success'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['jobId'],
            $data['leaseId'],
            $data['definitionDigest'],
            $data['success'],
            array_key_exists('error', $data) ? ErrorCode::from($data['error']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
