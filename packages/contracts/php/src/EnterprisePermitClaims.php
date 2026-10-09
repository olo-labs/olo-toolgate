<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Short-lived audience-bound permit for one exact invocation and target. */
final readonly class EnterprisePermitClaims implements \JsonSerializable {
    public function __construct(
        public string $issuer,
        public string $audience,
        public string $nonce,
        public string $invocationId,
        public string $requestDigest,
        public string $evaluationDigest,
        public int $authorizationEpoch,
        public int $directoryRevision,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public string $toolDigest,
        public string $packageDigest,
        public string $bindingId,
        public string $deviceId,
        public array $approvalIds
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['issuer', 'audience', 'nonce', 'invocationId', 'requestDigest', 'evaluationDigest', 'authorizationEpoch', 'directoryRevision', 'issuedAtUnixMs', 'expiresAtUnixMs', 'toolDigest', 'packageDigest', 'bindingId', 'deviceId', 'approvalIds']) || array_diff(['issuer', 'audience', 'nonce', 'invocationId', 'requestDigest', 'evaluationDigest', 'authorizationEpoch', 'directoryRevision', 'issuedAtUnixMs', 'expiresAtUnixMs', 'toolDigest', 'packageDigest', 'bindingId', 'deviceId', 'approvalIds'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['issuer'],
            $data['audience'],
            $data['nonce'],
            $data['invocationId'],
            $data['requestDigest'],
            $data['evaluationDigest'],
            $data['authorizationEpoch'],
            $data['directoryRevision'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['toolDigest'],
            $data['packageDigest'],
            $data['bindingId'],
            $data['deviceId'],
            array_map(static fn ($item) => $item, $data['approvalIds'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
