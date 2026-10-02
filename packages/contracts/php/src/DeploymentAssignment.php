<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Organization assignment with independent Marketplace evidence; grants no runtime permission. */
final readonly class DeploymentAssignment implements \JsonSerializable {
    public function __construct(
        public string $assignmentId,
        public string $deviceId,
        public MarketplaceRelease $release,
        public string $organizationKeyId,
        public string $organizationSignature,
        public bool $desiredPresence
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['assignmentId', 'deviceId', 'release', 'organizationKeyId', 'organizationSignature', 'desiredPresence']) || array_diff(['assignmentId', 'deviceId', 'release', 'organizationKeyId', 'organizationSignature', 'desiredPresence'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['assignmentId'],
            $data['deviceId'],
            MarketplaceRelease::fromArray($data['release']),
            $data['organizationKeyId'],
            $data['organizationSignature'],
            $data['desiredPresence']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
