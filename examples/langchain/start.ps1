# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param(
    [ValidateSet('tickets','operations','inventory')][string]$UseCase = 'tickets',
    [ValidateSet('any','linux','windows')][string]$Target = 'any',
    [switch]$Win,
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
    if ($Win) {
        if ($PSBoundParameters.ContainsKey('Target') -and $Target -ne 'windows') {
            throw '-Win selects Windows; use it alone or with -Target windows.'
        }
        $Target = 'windows'
    }
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
    if ($Target -eq 'windows') {
        if (-not $Profiles) { $Profiles = $env:TOOLGATE_WINDOWS_PROFILES }
    }
    if ($Profiles -and -not (Test-Path -LiteralPath $Profiles -PathType Leaf)) {
        throw "Device profiles not found: $Profiles. Run prepare-windows.ps1 on the Windows device to register its public profiles."
    }
    if (-not $Smoke -and -not $env:OPENROUTER_API_KEY) {
        throw 'Set OPENROUTER_API_KEY in examples/langchain/.env (https://openrouter.ai/settings/keys). The free model still needs a key. Use start.bat -Smoke to test real device operations without a model key.'
    }
    if (-not $Python) {
        . (Join-Path $PSScriptRoot '../../debug/python.ps1')
        $Python = Find-DebugPython
    }
    Invoke-ExampleCommand 'docker' @('compose','version')
    Invoke-ExampleCommand 'docker' @('compose','config','--quiet')
    # An existing .env (or a machine environment variable) wins over .env.example and may pin an old build.
    Write-Host "Quickstart image: $env:TOOLGATE_QUICKSTART_IMAGE"
    if ($env:TOOLGATE_QUICKSTART_IMAGE -match ':dev-[0-9a-f]{12}') {
        Write-Host 'This is a pinned development build, not the latest. Set TOOLGATE_QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev in .env to follow dev.' -ForegroundColor Yellow
    }
    if (-not $SkipBuild) {
        # Mutable tags such as `dev` must be re-pulled; a cached local copy keeps the old
        # server and, through the device build below, the old Linux client.
        Write-Host 'Pulling the published Quickstart image.'
        Invoke-ImageCommand @('compose','pull','quickstart')
        Write-Host 'Building the Linux endpoint from the published client package and installing LangChain.'
        Invoke-ImageCommand @('compose','build','--pull','linux-device','agent')
    } else {
        $ErrorActionPreference = 'Continue'
        & docker image inspect $env:TOOLGATE_QUICKSTART_IMAGE *> $null
        $imageExists = $LASTEXITCODE -eq 0
        $ErrorActionPreference = 'Stop'
        if (-not $imageExists) { Invoke-ImageCommand @('compose','pull','quickstart') }
    }
    Invoke-ExampleCommand 'docker' @('compose','up','-d','--wait','--wait-timeout','180','quickstart','linux-device')
    $setupArguments = @('manage.py','setup','--bootstrap-local','--approve-linux-device','--target',$Target)
    if ($DeviceId) { $setupArguments += @('--device-id',$DeviceId) }
    if ($Profiles) { $setupArguments += @('--profiles',$Profiles) }
    Invoke-ExampleCommand $Python $setupArguments
    Write-Host "Console: http://127.0.0.1:$($env:TOOLGATE_HTTP_PORT)/console/"
    $agentArguments = @('compose','run','--rm','-T','agent','--target',$Target,'--use-case',$UseCase)
    if ($Smoke) { $agentArguments += '--smoke' }
    Invoke-ExampleCommand 'docker' $agentArguments
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally { Pop-Location }
