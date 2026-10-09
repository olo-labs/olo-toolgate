<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Maker and independent eligible checker resolve an unknown effect; evidence stays external. */
final readonly class EnterpriseReconciliation implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $invocationId,
        public string $requesterUserId,
        public EnterpriseReconciliationRequest $request,
        public string $requestDigest,
        public int $revision,
        public EnterpriseReconciliationState $state,
        public ?string $reviewerUserId = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'invocationId', 'requesterUserId', 'request', 'requestDigest', 'revision', 'reviewerUserId', 'state']) || array_diff(['id', 'invocationId', 'requesterUserId', 'request', 'requestDigest', 'revision', 'state'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['invocationId'],
            $data['requesterUserId'],
            EnterpriseReconciliationRequest::fromArray($data['request']),
            $data['requestDigest'],
            $data['revision'],
            EnterpriseReconciliationState::from($data['state']),
            array_key_exists('reviewerUserId', $data) ? $data['reviewerUserId'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
