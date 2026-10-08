<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Directory identity, enrolled approval and pending request combined for administrative management. */
final readonly class EndpointManagedDevice implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public bool $systemExecutor,
        public ?ControlDevice $directoryDevice = null,
        public ?EndpointDeviceRecord $endpointDevice = null,
        public ?EndpointEnrollmentReview $enrollment = null,
        public ?ControlUser $registeredUser = null,
        public ?SystemExecutorKind $systemExecutorKind = null,
        public ?bool $systemAvailable = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'systemExecutor', 'directoryDevice', 'endpointDevice', 'enrollment', 'registeredUser', 'systemExecutorKind', 'systemAvailable']) || array_diff(['deviceId', 'systemExecutor'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['systemExecutor'],
            array_key_exists('directoryDevice', $data) ? ControlDevice::fromArray($data['directoryDevice']) : null,
            array_key_exists('endpointDevice', $data) ? EndpointDeviceRecord::fromArray($data['endpointDevice']) : null,
            array_key_exists('enrollment', $data) ? EndpointEnrollmentReview::fromArray($data['enrollment']) : null,
            array_key_exists('registeredUser', $data) ? ControlUser::fromArray($data['registeredUser']) : null,
            array_key_exists('systemExecutorKind', $data) ? SystemExecutorKind::from($data['systemExecutorKind']) : null,
            array_key_exists('systemAvailable', $data) ? $data['systemAvailable'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
