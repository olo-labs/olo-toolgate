<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Exact-operation capability; maximum ten seconds; authoritative consume boundary enforces replay. */
final readonly class ExecutionPermitClaims implements \JsonSerializable {
    public function __construct(
        public int $permitVersion,
        public string $issuer,
        public string $audience,
        public string $tenantId,
        public string $userId,
        public string $agentId,
        public string $toolId,
        public string $action,
        public ResourceDescriptor $resource,
        public string $argumentsDigest,
        public string $requestId,
        public string $policyVersion,
        public string $approvalId,
        public string $jti,
        public int $issuedAtUnixMs,
        public int $expiresAtUnixMs,
        public ?string $deviceId = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['permitVersion', 'issuer', 'audience', 'tenantId', 'userId', 'agentId', 'deviceId', 'toolId', 'action', 'resource', 'argumentsDigest', 'requestId', 'policyVersion', 'approvalId', 'jti', 'issuedAtUnixMs', 'expiresAtUnixMs']) || array_diff(['permitVersion', 'issuer', 'audience', 'tenantId', 'userId', 'agentId', 'toolId', 'action', 'resource', 'argumentsDigest', 'requestId', 'policyVersion', 'approvalId', 'jti', 'issuedAtUnixMs', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['permitVersion'],
            $data['issuer'],
            $data['audience'],
            $data['tenantId'],
            $data['userId'],
            $data['agentId'],
            $data['toolId'],
            $data['action'],
            ResourceDescriptor::fromArray($data['resource']),
            $data['argumentsDigest'],
            $data['requestId'],
            $data['policyVersion'],
            $data['approvalId'],
            $data['jti'],
            $data['issuedAtUnixMs'],
            $data['expiresAtUnixMs'],
            array_key_exists('deviceId', $data) ? $data['deviceId'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
