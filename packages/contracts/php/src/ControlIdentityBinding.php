<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Stable verified tenant/issuer/subject identity; email never determines identity. */
final readonly class ControlIdentityBinding implements \JsonSerializable {
    public function __construct(
        public string $id,
        public string $name,
        public bool $enabled,
        public int $revision,
        public string $userId,
        public string $issuer,
        public string $subject,
        public int $sessionEpoch,
        public int $firstSeenUnixMs,
        public int $lastAttemptUnixMs,
        public int $attemptCount,
        public string $registrationReason,
        public int $sessionsValidAfterUnixMs
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['id', 'name', 'enabled', 'revision', 'userId', 'issuer', 'subject', 'sessionEpoch', 'firstSeenUnixMs', 'lastAttemptUnixMs', 'attemptCount', 'registrationReason', 'sessionsValidAfterUnixMs']) || array_diff(['id', 'name', 'enabled', 'revision', 'userId', 'issuer', 'subject', 'sessionEpoch', 'firstSeenUnixMs', 'lastAttemptUnixMs', 'attemptCount', 'registrationReason', 'sessionsValidAfterUnixMs'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['id'],
            $data['name'],
            $data['enabled'],
            $data['revision'],
            $data['userId'],
            $data['issuer'],
            $data['subject'],
            $data['sessionEpoch'],
            $data['firstSeenUnixMs'],
            $data['lastAttemptUnixMs'],
            $data['attemptCount'],
            $data['registrationReason'],
            $data['sessionsValidAfterUnixMs']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
