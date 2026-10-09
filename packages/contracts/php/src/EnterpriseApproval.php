<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Separate exact-operation and configuration approval records. Reviewers cannot expand the approved binding. */
final readonly class EnterpriseApproval implements \JsonSerializable {
    public function __construct(
        public string $id,
        public EnterpriseApprovalType $approvalType,
        public string $invocationId,
        public string $requestDigest,
        public int $authorizationEpoch,
        public int $directoryRevision,
        public array $obligationIds,
        public array $reviews,
        public EnterpriseApprovalState $state,
        public int $revision,
        public int $expiresAtUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'approvalType', 'invocationId', 'requestDigest', 'authorizationEpoch', 'directoryRevision', 'obligationIds', 'reviews', 'state', 'revision', 'expiresAtUnixMs']) || array_diff(['id', 'approvalType', 'invocationId', 'requestDigest', 'authorizationEpoch', 'directoryRevision', 'obligationIds', 'reviews', 'state', 'revision', 'expiresAtUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            EnterpriseApprovalType::from($data['approvalType']),
            $data['invocationId'],
            $data['requestDigest'],
            $data['authorizationEpoch'],
            $data['directoryRevision'],
            array_map(static fn ($item) => $item, $data['obligationIds']),
            array_map(static fn ($item) => EnterpriseApprovalReview::fromArray($item), $data['reviews']),
            EnterpriseApprovalState::from($data['state']),
            $data['revision'],
            $data['expiresAtUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
