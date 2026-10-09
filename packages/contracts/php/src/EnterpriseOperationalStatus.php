<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Scoped operational health and configured hard bounds. Snapshot lag never authorizes a cached effect. */
final readonly class EnterpriseOperationalStatus implements \JsonSerializable {
    public function __construct(
        public int $pendingSnapshotEvents,
        public int $oldestUnpublishedUnixMs,
        public int $unknownOutcomes,
        public int $expiredRunningEffects,
        public int $permitLifetimeMs,
        public int $clockSkewMs,
        public int $authorityCacheGraceMs,
        public int $readyProbeFreshnessMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['pendingSnapshotEvents', 'oldestUnpublishedUnixMs', 'unknownOutcomes', 'expiredRunningEffects', 'permitLifetimeMs', 'clockSkewMs', 'authorityCacheGraceMs', 'readyProbeFreshnessMs']) || array_diff(['pendingSnapshotEvents', 'oldestUnpublishedUnixMs', 'unknownOutcomes', 'expiredRunningEffects', 'permitLifetimeMs', 'clockSkewMs', 'authorityCacheGraceMs', 'readyProbeFreshnessMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['pendingSnapshotEvents'],
            $data['oldestUnpublishedUnixMs'],
            $data['unknownOutcomes'],
            $data['expiredRunningEffects'],
            $data['permitLifetimeMs'],
            $data['clockSkewMs'],
            $data['authorityCacheGraceMs'],
            $data['readyProbeFreshnessMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
