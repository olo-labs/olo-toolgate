<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Tenant-scoped control-plane settings shown in the administrative Configuration page. Auto-approval issues bounded device identities without a human decision and is off unless an administrator enables it. */
final readonly class ControlServerSettings implements \JsonSerializable {
    public function __construct(
        public int $formatVersion,
        public int $revision,
        public bool $autoApproveDevices,
        public int $autoApproveDurationDays,
        public ?string $autoApproveOwnerUserId = null,
        public ?string $gatewayName = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['formatVersion', 'revision', 'autoApproveDevices', 'autoApproveDurationDays', 'autoApproveOwnerUserId', 'gatewayName']) || array_diff(['formatVersion', 'revision', 'autoApproveDevices', 'autoApproveDurationDays'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['formatVersion'],
            $data['revision'],
            $data['autoApproveDevices'],
            $data['autoApproveDurationDays'],
            array_key_exists('autoApproveOwnerUserId', $data) ? $data['autoApproveOwnerUserId'] : null,
            array_key_exists('gatewayName', $data) ? $data['gatewayName'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
