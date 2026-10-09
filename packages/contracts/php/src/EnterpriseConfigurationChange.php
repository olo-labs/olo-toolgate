<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed configuration workflow bound to the exact group graph revision. */
final readonly class EnterpriseConfigurationChange implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $requesterUserId,
        public EnterpriseConfigurationCommand $command,
        public string $requestDigest,
        public int $directoryRevision,
        public int $authorizationEpoch,
        public EnterpriseConfigurationState $state,
        public int $revision,
        public int $createdAtUnixMs,
        public int $expiresAtUnixMs,
        public int $requiredReviews,
        public array $impact,
        public array $affectedGroups,
        public array $affectedIndividuals,
        public array $reviews
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'requesterUserId', 'command', 'requestDigest', 'directoryRevision', 'authorizationEpoch', 'state', 'revision', 'createdAtUnixMs', 'expiresAtUnixMs', 'requiredReviews', 'impact', 'affectedGroups', 'affectedIndividuals', 'reviews']) || array_diff(['id', 'requesterUserId', 'command', 'requestDigest', 'directoryRevision', 'authorizationEpoch', 'state', 'revision', 'createdAtUnixMs', 'expiresAtUnixMs', 'requiredReviews', 'impact', 'affectedGroups', 'affectedIndividuals', 'reviews'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['requesterUserId'],
            EnterpriseConfigurationCommand::fromArray($data['command']),
            $data['requestDigest'],
            $data['directoryRevision'],
            $data['authorizationEpoch'],
            EnterpriseConfigurationState::from($data['state']),
            $data['revision'],
            $data['createdAtUnixMs'],
            $data['expiresAtUnixMs'],
            $data['requiredReviews'],
            array_map(static fn ($item) => EnterpriseConfigurationImpact::fromArray($item), $data['impact']),
            array_map(static fn ($item) => $item, $data['affectedGroups']),
            array_map(static fn ($item) => $item, $data['affectedIndividuals']),
            array_map(static fn ($item) => EnterpriseConfigurationReview::fromArray($item), $data['reviews'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
