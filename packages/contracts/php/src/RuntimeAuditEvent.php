<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Sanitized authorization evaluation event; hashes replace raw arguments and resource locators. A decision is not execution success. */
final readonly class RuntimeAuditEvent implements \JsonSerializable {
    public function __construct(
        public int $timestampUnixMs,
        public RequestContext $context,
        public string $toolId,
        public string $action,
        public string $resourceDigest,
        public string $argumentsDigest,
        public PolicyDecision $decision,
        public string $traceId
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['timestampUnixMs', 'context', 'toolId', 'action', 'resourceDigest', 'argumentsDigest', 'decision', 'traceId']) || array_diff(['timestampUnixMs', 'context', 'toolId', 'action', 'resourceDigest', 'argumentsDigest', 'decision', 'traceId'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['timestampUnixMs'],
            RequestContext::fromArray($data['context']),
            $data['toolId'],
            $data['action'],
            $data['resourceDigest'],
            $data['argumentsDigest'],
            PolicyDecision::fromArray($data['decision']),
            $data['traceId']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
