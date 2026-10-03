<?php
// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
declare(strict_types=1);

spl_autoload_register(static function (string $class): void {
    $prefix = 'OloLabs\\ToolGate\\Contracts\\';
    if (str_starts_with($class, $prefix)) {
        require __DIR__ . '/../src/' . substr($class, strlen($prefix)) . '.php';
    }
});

$fixtures = json_decode(file_get_contents(__DIR__ . '/../../../../tests/fixtures/contracts/v1/valid.json'), true, 512, JSON_THROW_ON_ERROR);
$count = 0;
foreach ($fixtures as $name => $fixture) {
    if (in_array($name, ['Identifier', 'SemanticVersion', 'Sha256', 'SecretReference'], true)) {
        continue;
    }
    $class = 'OloLabs\\ToolGate\\Contracts\\' . $name;
    $model = is_array($fixture) ? $class::fromArray($fixture) : $class::from($fixture);
    $wire = json_decode(json_encode($model, JSON_THROW_ON_ERROR), true, 512, JSON_THROW_ON_ERROR);
    if ($wire != $fixture) {
        throw new RuntimeException('Wire drift: ' . $name);
    }
    $count++;
}
if ($count !== 129) { throw new RuntimeException('Incomplete fixture coverage'); }
foreach ([['decision' => 'UNKNOWN'], ['bypass' => true], ['decision' => null]] as $change) {
    try {
        $bad = array_replace($fixtures['PolicyDecision'], $change);
        \OloLabs\ToolGate\Contracts\PolicyDecision::fromArray($bad);
    } catch (\ValueError | \TypeError | \InvalidArgumentException $expected) {
        continue;
    }
    throw new RuntimeException('Invalid policy accepted');
}
echo "PHP: 129 model round trips and security negatives passed\n";
