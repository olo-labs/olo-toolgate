# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Read the service's bounded, redacted journal. Never hold a file handle across a poll.
$ErrorActionPreference = 'Stop'
$packetConfig = Get-Content -LiteralPath 'C:\ProgramData\OLO\ToolGate\client.json' -Raw | ConvertFrom-Json
$packetPath = Join-Path $packetConfig.stateDirectory 'packets.jsonl'
$packetSeen = New-Object 'System.Collections.Generic.HashSet[string]'
Write-Host 'OLO ToolGate client messages (SEND / RECEIVE). Credentials and tool content are redacted.'
Write-Host 'Press Ctrl+C to close.'
while ($true) {
    if (Test-Path -LiteralPath $packetPath) {
        $packetLines = @(Get-Content -LiteralPath $packetPath -Encoding UTF8)
        foreach ($packetLine in $packetLines) {
            if (-not $packetLine -or -not $packetSeen.Add($packetLine)) { continue }
            $packet = $packetLine | ConvertFrom-Json
            $packetColor = if ($packet.direction -eq 'SEND') { 'Cyan' } else { 'Green' }
            if ($packet.status -ge 400 -or $packet.message.code -eq 'DEPENDENCY_UNAVAILABLE') { $packetColor = 'Red' }
            Write-Host $packetLine -ForegroundColor $packetColor
        }
        # Retention follows the service file, so the viewer also remains bounded.
        $packetCurrent = New-Object 'System.Collections.Generic.HashSet[string]'
        foreach ($packetLine in $packetLines) { [void]$packetCurrent.Add($packetLine) }
        $packetSeen = $packetCurrent
    }
    Start-Sleep -Seconds 2
}
