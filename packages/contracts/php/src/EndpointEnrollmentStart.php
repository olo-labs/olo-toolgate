<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Endpoint identity foundation wire model. */
final readonly class EndpointEnrollmentStart implements \JsonSerializable {
    public function __construct(
        public string $deviceId,
        public string $clientVersion,
        public ClientPlatform $platform,
        public string $csrPem,
        public array $capabilities
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['deviceId', 'clientVersion', 'platform', 'csrPem', 'capabilities']) || array_diff(['deviceId', 'clientVersion', 'platform', 'csrPem', 'capabilities'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['deviceId'],
            $data['clientVersion'],
            ClientPlatform::from($data['platform']),
            $data['csrPem'],
            array_map(static fn ($item) => $item, $data['capabilities'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
