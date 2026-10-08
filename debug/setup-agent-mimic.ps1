# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# First-time native setup followed by the actual agent-facing mimic requests.
$ErrorActionPreference = 'Stop'
$setupRoot = Split-Path $PSScriptRoot -Parent
$setupLocal = Join-Path $setupRoot '.dev/debug'
$setupResult = Join-Path $setupLocal ("native-prepare-" + [Guid]::NewGuid().ToString('N') + '.json')
try {
    [void](New-Item -ItemType Directory -Path $setupLocal -Force)
    $setupCompose = @('compose', '--project-name', 'toolgate-debug', '--project-directory', $PSScriptRoot, '-f', "$PSScriptRoot/compose.yaml")
    $setupContainer = & docker @setupCompose ps -q quickstart
    if ($LASTEXITCODE -ne 0 -or -not $setupContainer) { throw 'Start the debug Gateway with debug/start.bat first.' }
    & docker cp "${setupContainer}:/data/keys/device-ca.crt" "$setupLocal/toolgate-quickstart-ca.crt"
    if ($LASTEXITCODE -ne 0) { throw 'Could not export the debug Gateway public CA.' }
    # Run the repair helper in a separate process: its elevation launcher exits after dispatch.
    & powershell -NoProfile -ExecutionPolicy RemoteSigned -File "$PSScriptRoot/prepare-local-client.ps1" -ResultFile $setupResult
    if ($LASTEXITCODE -ne 0) { throw 'Windows client repair did not start. Approve Windows elevation with an administrator account, then retry.' }
    $setupDeadline = [DateTime]::UtcNow.AddMinutes(3)
    while (-not (Test-Path -LiteralPath $setupResult)) {
        if ([DateTime]::UtcNow -ge $setupDeadline) { throw 'Windows client repair has not finished. Wait for the installer before retrying.' }
        Start-Sleep -Seconds 1
    }
    $setupRepair = Get-Content -LiteralPath $setupResult -Raw | ConvertFrom-Json
    if (-not $setupRepair.success) { throw $setupRepair.message }
    $setupTray = 'C:\Program Files\OLO\ToolGateSetup\packaging\toolgate-tray.ps1'
    Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @('-NoProfile', '-STA', '-ExecutionPolicy', 'RemoteSigned', '-File', "`"$setupTray`"") -WindowStyle Hidden
    $setupPython = Join-Path $setupLocal 'venv/Scripts/python.exe'
    if (-not (Test-Path -LiteralPath $setupPython)) { . "$PSScriptRoot/python.ps1"; $setupPython = Find-DebugPython }
    & $setupPython "$PSScriptRoot/enroll-mimic-client.py"
    if ($LASTEXITCODE -ne 0) { throw 'Enrollment or the exact agent/device tool grants could not be prepared.' }
    & powershell -NoProfile -ExecutionPolicy RemoteSigned -File "$PSScriptRoot/agent-mimic.ps1"
    if ($LASTEXITCODE -ne 0) { throw 'Agent mimic failed. Inspect client/server packet diagnostics.' }
    Write-Host 'PASS: agent discovery, client file creation and one client log entry.' -ForegroundColor Green
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
}
