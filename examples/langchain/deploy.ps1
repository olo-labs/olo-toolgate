# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Bring up the stack, select a device and issue its agent credential. execute.ps1 runs the test.
param(
    [ValidateSet('any','linux','windows')][string]$Target = 'any',
    [switch]$Win,
    [string]$DeviceId = '',
    [string]$Profiles = '',
    [string]$Python = '',
    [switch]$SkipBuild
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
# manage.py exits with this when the Windows client is connected but not reporting its example tools.
$ToolsNotReportedExit = 3
$LocalClient = if ($env:ProgramFiles) { Join-Path $env:ProgramFiles 'OLO/ToolGate/olo-toolgate-client.exe' } else { '' }
function Test-WindowsRegistrationCurrent([string]$Gateway) {
    # A registration is current when it pins the installed executable's bytes.
    $digest = (Get-FileHash -LiteralPath $LocalClient -Algorithm SHA256).Hash.ToLowerInvariant()
    foreach ($file in Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot '.state/devices') -Filter 'device-*.json' -File -ErrorAction SilentlyContinue) {
        $record = Get-Content -LiteralPath $file.FullName -Raw | ConvertFrom-Json
        if ($record.platform -eq 'windows' -and $record.gateway.TrimEnd('/') -eq $Gateway -and
                @($record.profiles | Where-Object { $_.tool.packageDigest -eq $digest }).Count -gt 0) { return $true }
    }
    return $false
}
function Repair-WindowsTools([string]$Gateway, [string]$Reason) {
    # Self-healing: re-run the same reviewed preparation an administrator would run by hand.
    Write-Host "$Reason Repairing this machine's Windows client with prepare-windows.ps1; approve the administrator prompt." -ForegroundColor Yellow
    $log = Join-Path $PSScriptRoot '.state/prepare-windows.log'
    [void](New-Item -ItemType Directory -Force -Path (Split-Path $log -Parent))
    Remove-Item -LiteralPath $log -ErrorAction SilentlyContinue
    $quote = { param($value) "'" + $value.Replace("'", "''") + "'" }
    $command = "try { & $(& $quote (Join-Path $PSScriptRoot 'prepare-windows.ps1')) -Gateway $(& $quote $Gateway) } " +
        "catch { `$_.Exception.Message | Out-File -LiteralPath $(& $quote $log) -Encoding utf8; exit 1 }"
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    try {
        $process = Start-Process -FilePath 'powershell.exe' -Verb RunAs -Wait -PassThru `
            -ArgumentList @('-NoProfile','-ExecutionPolicy','RemoteSigned','-EncodedCommand',$encoded)
    } catch {
        throw 'The administrator prompt was declined. Run prepare-windows.ps1 in an Administrator PowerShell, then deploy.bat -Win.'
    }
    if ($process.ExitCode -ne 0) {
        $detail = if (Test-Path -LiteralPath $log) { (Get-Content -LiteralPath $log -Raw).Trim() } else { "exit $($process.ExitCode)" }
        throw "prepare-windows.ps1 could not repair the client: $detail"
    }
    Write-Host 'Windows client tool profiles repaired.'
}
Push-Location $PSScriptRoot
try {
    $Target = Resolve-ExampleTarget $PSBoundParameters $Target $Win.IsPresent
    Import-ExampleEnvironment
    if ($Target -eq 'windows') {
        if (-not $Profiles) { $Profiles = $env:TOOLGATE_WINDOWS_PROFILES }
    }
    if ($Profiles -and -not (Test-Path -LiteralPath $Profiles -PathType Leaf)) {
        throw "Device profiles not found: $Profiles. Run prepare-windows.ps1 on the Windows device to register its public profiles."
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
    $gateway = "https://localhost:$($env:TOOLGATE_TLS_PORT)"
    $canRepair = $LocalClient -and (Test-Path -LiteralPath $LocalClient -PathType Leaf)
    if ($Target -eq 'windows' -and $canRepair -and -not $Profiles -and -not (Test-WindowsRegistrationCurrent $gateway)) {
        Repair-WindowsTools $gateway 'No registration matches the installed Windows client (new install, update or reinstall).'
        $canRepair = $false
    }
    $setupArguments = @('manage.py','setup','--bootstrap-local','--approve-linux-device','--target',$Target)
    if ($DeviceId) { $setupArguments += @('--device-id',$DeviceId) }
    $firstArguments = if ($Profiles) { $setupArguments + @('--profiles',$Profiles) } else { $setupArguments }
    & $Python @firstArguments
    if ($LASTEXITCODE -eq $ToolsNotReportedExit -and $canRepair) {
        Repair-WindowsTools $gateway 'The Windows client is connected but not reporting its example tools.'
        # The repair rewrote this machine's registration in .state/devices; an imported copy would be stale.
        Invoke-ExampleCommand $Python $setupArguments
    } elseif ($LASTEXITCODE -ne 0) {
        throw "Example command failed (exit $LASTEXITCODE). See the output above."
    }
    Write-Host "Console: http://127.0.0.1:$($env:TOOLGATE_HTTP_PORT)/console/"
    $next = if ($Target -eq 'windows') { 'execute.bat -Win' } elseif ($Target -eq 'any') { 'execute.bat' } else { "execute.bat -Target $Target" }
    Write-Host "Deployed. Run the test with: $next (add -Smoke to skip the model)"
} catch {
    Write-Host $_.Exception.Message -ForegroundColor Red
    exit 1
} finally { Pop-Location }
