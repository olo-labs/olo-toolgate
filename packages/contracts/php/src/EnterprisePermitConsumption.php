<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Certificate-bound single-use consumption of an exact effect capability. */
final readonly class EnterprisePermitConsumption implements \JsonSerializable {
    public function __construct(
        public string $invocationId,
        public EnterpriseSignedPermit $permit,
        public string $argumentsDigest,
        public array $resources,
        public string $toolDigest,
        public string $packageDigest
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['invocationId', 'permit', 'argumentsDigest', 'resources', 'toolDigest', 'packageDigest']) || array_diff(['invocationId', 'permit', 'argumentsDigest', 'resources', 'toolDigest', 'packageDigest'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['invocationId'],
            EnterpriseSignedPermit::fromArray($data['permit']),
            $data['argumentsDigest'],
            array_map(static fn ($item) => ResourceDescriptor::fromArray($item), $data['resources']),
            $data['toolDigest'],
            $data['packageDigest']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
