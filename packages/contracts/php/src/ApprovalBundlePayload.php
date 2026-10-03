<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Signed format 2 envelope; compilation adds ASK; old readers reject safely. */
final readonly class ApprovalBundlePayload implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $issuer,
        public string $audience,
        public string $tenantId,
        public int $sequence,
        public string $version,
        public int $directoryRevision,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public int $graceMs,
        public string $policySha256,
        public string $policy,
        public ?int $rollbackOf = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'issuer', 'audience', 'tenantId', 'sequence', 'version', 'directoryRevision', 'issuedAtUnixMs', 'expiresAtUnixMs', 'graceMs', 'policySha256', 'policy', 'rollbackOf']) || array_diff(['formatVersion', 'issuer', 'audience', 'tenantId', 'sequence', 'version', 'directoryRevision', 'issuedAtUnixMs', 'expiresAtUnixMs', 'graceMs', 'policySha256', 'policy'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['issuer'],
            $data['audience'],
            $data['tenantId'],
            $data['sequence'],
            $data['version'],
            $data['directoryRevision'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['graceMs'],
            $data['policySha256'],
            $data['policy'],
            array_key_exists('rollbackOf', $data) ? $data['rollbackOf'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
