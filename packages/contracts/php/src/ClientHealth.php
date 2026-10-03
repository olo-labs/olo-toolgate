<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class ClientHealth implements \JsonSerializable {
    public function __construct(
        public EndpointState $state,
        public bool $ready,
        public int $uptimeSeconds,
        public int $successfulCheckIns,
        public int $failedCheckIns,
        public int $reportSequence,
        public ?int $lastSuccessUnixMs = null
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['state', 'ready', 'uptimeSeconds', 'successfulCheckIns', 'failedCheckIns', 'reportSequence', 'lastSuccessUnixMs']) || array_diff(['state', 'ready', 'uptimeSeconds', 'successfulCheckIns', 'failedCheckIns', 'reportSequence'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            EndpointState::from($data['state']),
            $data['ready'],
            $data['uptimeSeconds'],
            $data['successfulCheckIns'],
            $data['failedCheckIns'],
            $data['reportSequence'],
            array_key_exists('lastSuccessUnixMs', $data) ? $data['lastSuccessUnixMs'] : null
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
