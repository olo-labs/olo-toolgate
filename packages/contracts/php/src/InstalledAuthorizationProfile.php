<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// GENERATED FILE — DO NOT EDIT DIRECTLY; tools/contracts/generate.py
declare(strict_types=1);
namespace OloLabs\ToolGate\Contracts;

/** Reviewed deployment metadata, never a grant. Local executors independently verify the installed package and resource extraction definition. */
final readonly class InstalledAuthorizationProfile implements \JsonSerializable {
    public function __construct(
        public ControlTool $tool,
        public ControlResourceExtractor $extractor
    ) {}

    /** Decode a structural model; canonical schema validation is also required. */
    public static function fromArray(array $data): self {
        if (array_diff(array_keys($data), ['tool', 'extractor']) || array_diff(['tool', 'extractor'], array_keys($data))) {
            throw new \InvalidArgumentException('Unknown or missing contract fields');
        }
        return new self(
            ControlTool::fromArray($data['tool']),
            ControlResourceExtractor::fromArray($data['extractor'])
        );
    }

    public function jsonSerialize(): object {
        return (object) array_filter(get_object_vars($this), static fn ($v) => $v !== null);
    }
}
