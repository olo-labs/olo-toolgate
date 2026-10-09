<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Group-scoped guardrails. ALLOW does not create a grant; BLOCK overrides and ASK accumulates. */
final readonly class ControlPolicy implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public Decision $decision,
        public EnterpriseScope $scope,
        public GroupSelection $teams,
        public GroupSelection $agentGroups,
        public GroupSelection $approverTeams
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'decision', 'scope', 'teams', 'agentGroups', 'approverTeams']) || array_diff(['id', 'name', 'enabled', 'revision', 'decision', 'scope', 'teams', 'agentGroups', 'approverTeams'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            Decision::from($data['decision']),
            EnterpriseScope::fromArray($data['scope']),
            GroupSelection::fromArray($data['teams']),
            GroupSelection::fromArray($data['agentGroups']),
            GroupSelection::fromArray($data['approverTeams'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
