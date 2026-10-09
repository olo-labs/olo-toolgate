<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Protected operator review for group-only bootstrap or bounded recovery. */
final readonly class EnterpriseRecoveryAuthorization implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $tenantId,
        public int $expectedRevision,
        public ControlSnapshot $snapshot,
        public string $reasonDigest,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'tenantId', 'expectedRevision', 'snapshot', 'reasonDigest', 'issuedAtUnixMs', 'expiresAtUnixMs']) || array_diff(['formatVersion', 'tenantId', 'expectedRevision', 'snapshot', 'reasonDigest', 'issuedAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['tenantId'],
            $data['expectedRevision'],
            ControlSnapshot::fromArray($data['snapshot']),
            $data['reasonDigest'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
