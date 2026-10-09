# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param(
    [ValidateSet('tickets','operations','inventory')][string]$UseCase = 'tickets',
    [ValidateSet('linux','windows')][string]$Target = 'linux',
    [string]$DeviceId = '',
    [string]$Profiles = '',
    [string]$Python = '',
    [switch]$Smoke,
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
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
    throw 'Image operation failed after 3 attempts. Docker Hub may be unavailable. Once images have built successfully, rerun with -SkipBuild.'
}
Push-Location $PSScriptRoot
try {
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
    if (-not $Smoke -and -not $env:OPENAI_API_KEY) {
        throw 'Set OPENAI_API_KEY in examples/langchain/.env before the AI run. Use start.bat -Smoke to test real device operations without a model key.'
    }
    if ($Target -eq 'windows' -and (-not $DeviceId -or -not $Profiles)) {
        throw 'For Windows, enroll/approve the device and run prepare-windows.ps1, then supply -DeviceId and -Profiles.'
    }
    if (-not $Python) {
        . (Join-Path $PSScriptRoot '../../debug/python.ps1')
        $Python = Find-DebugPython
    }
    Invoke-ExampleCommand 'docker' @('compose','version')
    Invoke-ExampleCommand 'docker' @('compose','config','--quiet')
    if (-not $SkipBuild) {
        Write-Host 'Building the Linux endpoint from the published client package and installing LangChain.'
        Invoke-ImageCommand @('compose','build','linux-device','agent')
    }
    $ErrorActionPreference = 'Continue'
    & docker image inspect $env:TOOLGATE_QUICKSTART_IMAGE *> $null
    $imageExists = $LASTEXITCODE -eq 0
    $ErrorActionPreference = 'Stop'
    if (-not $imageExists) { Invoke-ImageCommand @('compose','pull','quickstart') }
    Invoke-ExampleCommand 'docker' @('compose','up','-d','--wait','--wait-timeout','180','quickstart','linux-device')
    $setupArguments = @('manage.py','setup','--bootstrap-local','--target',$Target)
    if ($Target -eq 'linux') { $setupArguments += '--approve-linux-device' }
    else { $setupArguments += @('--device-id',$DeviceId,'--profiles',$Profiles) }
    Invoke-ExampleCommand $Python $setupArguments
    Write-Host "Console: http://127.0.0.1:$($env:TOOLGATE_HTTP_PORT)/console/"
    $agentArguments = @('compose','run','--rm','-T','agent','--target',$Target,'--use-case',$UseCase)
    if ($Smoke) { $agentArguments += '--smoke' }
    Invoke-ExampleCommand 'docker' $agentArguments
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally { Pop-Location }
