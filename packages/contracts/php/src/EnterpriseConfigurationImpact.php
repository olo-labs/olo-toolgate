<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed configuration workflow bound to the exact group graph revision. */
final readonly class EnterpriseConfigurationImpact implements \JsonSerializable {
    public function __construct(
        public ControlEntityKind $kind,
        public string $entityId,
        public EnterpriseConfigurationImpactOperation $operation,
        public string $beforeDigest,
        public string $afterDigest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['kind', 'entityId', 'operation', 'beforeDigest', 'afterDigest']) || array_diff(['kind', 'entityId', 'operation', 'beforeDigest', 'afterDigest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ControlEntityKind::from($data['kind']),
            $data['entityId'],
            EnterpriseConfigurationImpactOperation::from($data['operation']),
            $data['beforeDigest'],
            $data['afterDigest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
