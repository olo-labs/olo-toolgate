<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed configuration workflow bound to the exact group graph revision. */
final readonly class EnterpriseConfigurationCommand implements \JsonSerializable {
    public function __construct(
        public EnterpriseConfigurationOperation $operation,
        public ControlEntityKind $kind,
        public string $entityId,
        public string $document,
        public int $expectedRevision
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['operation', 'kind', 'entityId', 'document', 'expectedRevision']) || array_diff(['operation', 'kind', 'entityId', 'document', 'expectedRevision'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseConfigurationOperation::from($data['operation']),
            ControlEntityKind::from($data['kind']),
            $data['entityId'],
            $data['document'],
            $data['expectedRevision']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
