# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Shared helpers for deploy.ps1, execute.ps1 and start.ps1. Dot-source; not a script to run.
function Invoke-ExampleCommand([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Example command failed (exit $LASTEXITCODE). See the output above." }
}
function Invoke-ImageCommand([string[]]$Arguments) {
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        # Image builds/pulls are safe to retry; never retry configuration or tool effects.
        & docker @Arguments
        if ($LASTEXITCODE -eq 0) { return }
        if ($attempt -lt 3) {
            Write-Host "Image operation failed; retry $($attempt + 1) of 3 in $($attempt * 3) seconds."
            Start-Sleep -Seconds ($attempt * 3)
        }
    }
    throw 'Image operation failed after 3 attempts. Docker Hub may be unavailable. Once images have built successfully, rerun deploy.bat with -SkipBuild.'
}
function Import-ExampleEnvironment {
    if (-not (Test-Path -LiteralPath '.env')) { Copy-Item -LiteralPath '.env.example' -Destination '.env' }
    foreach ($line in Get-Content -LiteralPath '.env') {
        if ($line.Trim() -and -not $line.TrimStart().StartsWith('#')) {
            $parts = $line.Split('=',2)
            if ($parts.Length -ne 2) { throw 'Invalid .env line; use NAME=value.' }
            $name = $parts[0].Trim()
            if ($null -eq [Environment]::GetEnvironmentVariable($name,'Process')) {
                [Environment]::SetEnvironmentVariable($name,$parts[1].Trim().Trim('"').Trim("'"),'Process')
            }
        }
    }
}
function Resolve-ExampleTarget([hashtable]$Bound, [string]$Target, [bool]$Win) {
    if (-not $Win) { return $Target }
    if ($Bound.ContainsKey('Target') -and $Target -ne 'windows') {
        throw '-Win selects Windows; use it alone or with -Target windows.'
    }
    return 'windows'
}
