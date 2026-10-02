<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Marketplace trust only; never organization or runtime authorization. */
final readonly class MarketplaceRelease implements \JsonSerializable {
    public function __construct(
        public string $packageId,
        public string $version,
        public string $manifestDigest,
        public string $marketplaceKeyId,
        public string $marketplaceSignature
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['packageId', 'version', 'manifestDigest', 'marketplaceKeyId', 'marketplaceSignature']) || array_diff(['packageId', 'version', 'manifestDigest', 'marketplaceKeyId', 'marketplaceSignature'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            $data['packageId'],
            $data['version'],
            $data['manifestDigest'],
            $data['marketplaceKeyId'],
            $data['marketplaceSignature']
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
