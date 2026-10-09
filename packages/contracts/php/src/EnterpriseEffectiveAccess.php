<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Read-only inherited access provenance at the current authority revision. No direct individual access mapping is created. Bindings and candidate grants do not constitute execution authorization. */
final readonly class EnterpriseEffectiveAccess implements \JsonSerializable {
    public function __construct(
        public string $entityId,
        public ControlEntityKind $kind,
        public int $directoryRevision,
        public int $authorizationEpoch,
        public array $memberships,
        public array $bindings,
        public array $managementRoles
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['entityId', 'kind', 'directoryRevision', 'authorizationEpoch', 'memberships', 'bindings', 'managementRoles']) || array_diff(['entityId', 'kind', 'directoryRevision', 'authorizationEpoch', 'memberships', 'bindings', 'managementRoles'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['entityId'],
            ControlEntityKind::from($data['kind']),
            $data['directoryRevision'],
            $data['authorizationEpoch'],
            array_map(static fn ($item) => EnterpriseEffectiveMembership::fromArray($item), $data['memberships']),
            array_map(static fn ($item) => ControlExecutionBinding::fromArray($item), $data['bindings']),
            array_map(static fn ($item) => ControlRole::fromArray($item), $data['managementRoles'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
