<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Saved authority, explicitly published signed snapshot and independently acknowledged device adoption are distinct states. */
final readonly class EnterpriseAuthorityStatus implements \JsonSerializable {
    public function __construct(
        public int $directoryRevision,
        public int $authorizationEpoch,
        public int $snapshotSequence,
        public array $adoptions,
        public ?int $publishedRevision = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['directoryRevision', 'authorizationEpoch', 'snapshotSequence', 'publishedRevision', 'adoptions']) || array_diff(['directoryRevision', 'authorizationEpoch', 'snapshotSequence', 'adoptions'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['directoryRevision'],
            $data['authorizationEpoch'],
            $data['snapshotSequence'],
            array_map(static fn ($item) => EnterpriseAdoptionStatus::fromArray($item), $data['adoptions']),
            array_key_exists('publishedRevision', $data) ? $data['publishedRevision'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
