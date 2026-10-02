<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Exact request identity and argument digest supplied to authorization. */
final readonly class PolicyInput implements \JsonSerializable {
    public function __construct(
        public RequestContext $context,
        public string $toolId,
        public string $action,
        public ResourceDescriptor $resource,
        public string $argumentsDigest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['context', 'toolId', 'action', 'resource', 'argumentsDigest']) || array_diff(['context', 'toolId', 'action', 'resource', 'argumentsDigest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            RequestContext::fromArray($data['context']),
            $data['toolId'],
            $data['action'],
            ResourceDescriptor::fromArray($data['resource']),
            $data['argumentsDigest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
