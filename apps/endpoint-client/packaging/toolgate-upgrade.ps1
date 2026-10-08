# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Setup pauses the native manifest first, so Chrome cannot reopen files during replacement.
param([Parameter(Mandatory=$true)][string]$InstalledDirectory)
$ErrorActionPreference = 'Stop'
$upgradeRoot = [IO.Path]::GetFullPath($InstalledDirectory).TrimEnd('\')
$upgradeExpected = Join-Path ([Environment]::GetFolderPath('ProgramFiles')) 'OLO\ToolGateSetup'
if ($upgradeRoot -ne $upgradeExpected) { throw 'Unexpected client setup directory.' }
$upgradeTray = Join-Path $upgradeRoot 'packaging\toolgate-tray.ps1'
$upgradeExecutables = @((Join-Path $upgradeRoot 'olo-toolgate-browser-host.exe'), (Join-Path $upgradeRoot 'olo-toolgate-client.exe'))
Get-CimInstance Win32_Process | Where-Object {
    $_.ExecutablePath -in $upgradeExecutables -or
    ($_.Name -eq 'powershell.exe' -and $_.CommandLine -and $_.CommandLine.Contains($upgradeTray))
} | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
}
