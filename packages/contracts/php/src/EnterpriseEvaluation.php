<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Side-effect-free complete request for simulation or trusted enforcement. */
final readonly class EnterpriseEvaluation implements \JsonSerializable {
    public function __construct(
        public EnterpriseContext $context,
        public string $toolId,
        public string $action,
        public string $argumentsDigest,
        public array $resources,
        public string $toolDigest,
        public string $packageDigest,
        public int $nowUnixMs,
        public int $authorityRevision,
        public bool $online,
        public ?int $amountMinorUnits = null,
        public ?string $operation = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['context', 'toolId', 'action', 'argumentsDigest', 'resources', 'toolDigest', 'packageDigest', 'nowUnixMs', 'authorityRevision', 'online', 'amountMinorUnits', 'operation']) || array_diff(['context', 'toolId', 'action', 'argumentsDigest', 'resources', 'toolDigest', 'packageDigest', 'nowUnixMs', 'authorityRevision', 'online'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EnterpriseContext::fromArray($data['context']),
            $data['toolId'],
            $data['action'],
            $data['argumentsDigest'],
            array_map(static fn ($item) => ResourceDescriptor::fromArray($item), $data['resources']),
            $data['toolDigest'],
            $data['packageDigest'],
            $data['nowUnixMs'],
            $data['authorityRevision'],
            $data['online'],
            array_key_exists('amountMinorUnits', $data) ? $data['amountMinorUnits'] : null,
            array_key_exists('operation', $data) ? $data['operation'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
