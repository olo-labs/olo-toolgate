# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param(
    [ValidateSet('start', 'stop', 'restart')][string]$Operation = 'start',
    [switch]$FullCheck,
    [switch]$ValidateOnly
)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
function Invoke-Checked([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "$Executable failed with exit code $LASTEXITCODE. Deployment was not continued." }
}
$compose = @('compose', '--project-name', 'toolgate-debug', '--project-directory', $PSScriptRoot, '-f', "$PSScriptRoot/compose.yaml")
Push-Location $root
try {
    Invoke-Checked docker @('info', '--format', '{{.OSType}}')
    Invoke-Checked docker ($compose + @('config', '--quiet'))
    if ($ValidateOnly) { return }
    if ($Operation -eq 'stop') {
        Invoke-Checked docker ($compose + @('stop'))
        return
    }
    . "$PSScriptRoot/python.ps1"
    $python = Find-DebugPython
    Write-Host "Using Python: $python"
    foreach ($command in @('git', 'node', 'npm')) {
        if (-not (Get-Command $command -ErrorAction SilentlyContinue)) { throw "Install $command and add it to PATH." }
    }
    Invoke-Checked $python @('-m', 'venv', '.dev/debug/venv')
    $python = Join-Path $root '.dev/debug/venv/Scripts/python.exe'
    Invoke-Checked $python @('-m', 'pip', 'install', '-r', 'tools/requirements.txt')
    Invoke-Checked npm @('ci', '--ignore-scripts')
    Invoke-Checked npx @('--no-install', 'playwright', 'install', 'chromium')
    Invoke-Checked $python @('tools/ci/preflight.py')
    Invoke-Checked $python @('-m', 'unittest', 'discover', '-s', 'tests/ci', '-v')
    Invoke-Checked $python @('tools/quality.py')
    Invoke-Checked $python @('-m', 'unittest', 'discover', '-s', 'tests/contracts', '-v')
    Invoke-Checked npm @('run', 'ui:check')
    Invoke-Checked npm @('--workspace', '@olo-labs/toolgate-admin-ui', 'test')
    Invoke-Checked node @('--test', 'apps/browser-extension/tests/protocol.mjs', 'apps/browser-extension/tests/worker.mjs')
    Invoke-Checked node @('tools/ci/browser-downloads.mjs')
    Invoke-Checked node @('tools/ci/linux-browser-downloads.mjs')
    if ($FullCheck) {
        Invoke-Checked $python @('tools/check.py')
        Invoke-Checked $python @('tools/check.py', '--scans')
    }
    # Smoke-test the candidate before replacing the persistent debug container.
    Invoke-Checked $python @('tools/quickstart/check.py', '--build', '--image', 'olo-toolgate-quickstart:debug')
    Invoke-Checked docker ($compose + @('up', '-d', '--wait', '--wait-timeout', '150', '--force-recreate', '--pull', 'never'))
    Write-Host 'ToolGate debug console: http://127.0.0.1:18090/console/'
    Write-Host 'Bootstrap credentials: docker compose -p toolgate-debug -f debug/compose.yaml logs quickstart'
    Start-Process 'http://127.0.0.1:18090/console/'
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
} finally { Pop-Location }
