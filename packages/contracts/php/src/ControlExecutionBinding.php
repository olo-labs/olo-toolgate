<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Explicit Tool Group to Device Group binding; one binding is selected per invocation. */
final readonly class ControlExecutionBinding implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $toolGroupId,
        public string $deviceGroupId,
        public array $actions,
        public array $allowedPackageDigests,
        public bool $requireOnline,
        public bool $ownerDependency
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'toolGroupId', 'deviceGroupId', 'actions', 'allowedPackageDigests', 'requireOnline', 'ownerDependency']) || array_diff(['id', 'name', 'enabled', 'revision', 'toolGroupId', 'deviceGroupId', 'actions', 'allowedPackageDigests', 'requireOnline', 'ownerDependency'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['toolGroupId'],
            $data['deviceGroupId'],
            array_map(static fn ($item) => $item, $data['actions']),
            array_map(static fn ($item) => $item, $data['allowedPackageDigests']),
            $data['requireOnline'],
            $data['ownerDependency']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
