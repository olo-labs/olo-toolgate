<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed group graph for discovery and adoption. It is never an execution permit. */
final readonly class EnterpriseSnapshotPayload implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $issuer,
        public string $audience,
        public string $tenantId,
        public int $sequence,
        public string $policyVersion,
        public int $directoryRevision,
        public int $authorizationEpoch,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public string $graphSha256,
        public string $graphBase64,
        public ?int $rollbackOf = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'issuer', 'audience', 'tenantId', 'sequence', 'policyVersion', 'directoryRevision', 'authorizationEpoch', 'issuedAtUnixMs', 'expiresAtUnixMs', 'graphSha256', 'graphBase64', 'rollbackOf']) || array_diff(['formatVersion', 'issuer', 'audience', 'tenantId', 'sequence', 'policyVersion', 'directoryRevision', 'authorizationEpoch', 'issuedAtUnixMs', 'expiresAtUnixMs', 'graphSha256', 'graphBase64'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['issuer'],
            $data['audience'],
            $data['tenantId'],
            $data['sequence'],
            $data['policyVersion'],
            $data['directoryRevision'],
            $data['authorizationEpoch'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['graphSha256'],
            $data['graphBase64'],
            array_key_exists('rollbackOf', $data) ? $data['rollbackOf'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
