# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param(
    [string]$Gateway = 'https://localhost:18450',
    [string]$TokenFile = '',
    [string]$CaFile = '',
    [string]$File = 'rahul-nigam.txt'
)
$ErrorActionPreference = 'Stop'
$mimicRoot = Split-Path $PSScriptRoot -Parent
. "$PSScriptRoot/console-log.ps1"
$mimicLogging = $false
$mimicExitCode = 1
try {
    $mimicLog = Start-DebugConsoleLog -Name 'agent-mimic'
    $mimicLogging = $true
    $mimicPython = Join-Path $mimicRoot '.dev/debug/venv/Scripts/python.exe'
    if (-not (Test-Path -LiteralPath $mimicPython)) { . "$PSScriptRoot/python.ps1"; $mimicPython = Find-DebugPython }
    $mimicArguments = @("$PSScriptRoot/agent-mimic.py", '--gateway', $Gateway, '--file', $File)
    if ($TokenFile) { $mimicArguments += @('--token-file', $TokenFile) }
    if ($CaFile) { $mimicArguments += @('--ca-file', $CaFile) }
    $mimicExitCode = Invoke-LoggedDebugCommand -FilePath $mimicPython -Arguments $mimicArguments
} catch {
    Write-Host $_ -ForegroundColor Red
} finally {
    if ($mimicLogging) { Stop-Transcript | Out-Null }
}
exit $mimicExitCode
