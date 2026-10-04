<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Versioned configuration data. Contains no credentials or executable code. */
final readonly class ControlSnapshot implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public string $tenantId,
        public int $revision,
        public array $users,
        public array $teams,
        public array $agents,
        public array $tools,
        public array $policies,
        public array $devices,
        public ?array $roles = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'tenantId', 'revision', 'users', 'teams', 'agents', 'tools', 'policies', 'devices', 'roles']) || array_diff(['formatVersion', 'tenantId', 'revision', 'users', 'teams', 'agents', 'tools', 'policies', 'devices'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['tenantId'],
            $data['revision'],
            array_map(static fn ($item) => ControlUser::fromArray($item), $data['users']),
            array_map(static fn ($item) => ControlTeam::fromArray($item), $data['teams']),
            array_map(static fn ($item) => ControlAgent::fromArray($item), $data['agents']),
            array_map(static fn ($item) => ControlTool::fromArray($item), $data['tools']),
            array_map(static fn ($item) => ControlPolicy::fromArray($item), $data['policies']),
            array_map(static fn ($item) => ControlDevice::fromArray($item), $data['devices']),
            array_key_exists('roles', $data) ? array_map(static fn ($item) => ControlRole::fromArray($item), $data['roles']) : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
