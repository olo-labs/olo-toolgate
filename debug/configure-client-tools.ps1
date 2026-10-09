# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Register existing diagnostic definitions; permissions remain in reviewed server groups.
param([Parameter(Mandatory=$true)][string]$DeviceId)
$ErrorActionPreference = 'Stop'
if ($DeviceId -notmatch '^device-[a-f0-9]{32}$') { throw 'Supply the enrolled device ID from the local debug console.' }
$configureRoot = Split-Path $PSScriptRoot -Parent
$configureResult = Join-Path $configureRoot '.dev/debug/client-tools-result.json'
$configureIdentity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$configureIdentity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    Write-Host 'Windows administrator approval is required to register the existing client tools and restart its service.'
    if (Test-Path -LiteralPath $configureResult) { Remove-Item -LiteralPath $configureResult }
    $configureProcess = Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -Verb RunAs -WindowStyle Hidden -PassThru -ArgumentList @('-NoProfile','-ExecutionPolicy','RemoteSigned','-File',"`"$PSCommandPath`"",'-DeviceId',$DeviceId)
    while (-not $configureProcess.WaitForExit(1000)) { }
    if (-not (Test-Path -LiteralPath $configureResult)) { throw 'Client tool setup did not finish. Check the administrator approval window.' }
    $configureOutcome = Get-Content -LiteralPath $configureResult -Raw | ConvertFrom-Json
    if (-not $configureOutcome.success) { throw $configureOutcome.message }
    Write-Host "Existing write/log tools configured for $DeviceId. No client executable was replaced."
    exit 0
}
try {
    $configurePath = 'C:\ProgramData\OLO\ToolGate\client.json'
    $configureBinary = 'C:\Program Files\OLO\ToolGate\olo-toolgate-client.exe'
    $configureOriginal = [IO.File]::ReadAllText($configurePath)
    $configureConfig = $configureOriginal | ConvertFrom-Json
    if ($configureConfig.serverUrl -ne 'https://localhost:18450') { throw 'This helper is limited to the installed local debug gateway.' }
    $configureJournal = Get-Content -LiteralPath (Join-Path $configureConfig.stateDirectory 'journal.json') -Raw | ConvertFrom-Json
    if ($configureJournal.identity.deviceId -ne $DeviceId) { throw 'The selected device does not match this installed client identity.' }
    $configureProfiles = @(& $configureBinary authorization-profiles | ConvertFrom-Json | ForEach-Object { $_ })
    if ($LASTEXITCODE -ne 0) { throw 'The installed client could not export its authorization profiles.' }
    $configureSelected = @($configureProfiles | Where-Object { $_.tool.id -in @('hotfolder.write_text','client.read_log_entry') })
    if ($configureSelected.Count -ne 2) { throw 'The installed client must already support file-write and diagnostic-log tools.' }
    $configureToken = [IO.File]::ReadAllText((Join-Path $configureRoot '.dev/debug/client-agent-token')).Trim()
    if ($configureToken -notmatch '^[a-zA-Z0-9_.~-]{32,256}$') { throw 'Configure the debug agent credential separately first.' }
    $configureTokenPath = Join-Path $configureConfig.stateDirectory 'debug-agent-token'
    if (-not (Test-Path -LiteralPath $configureTokenPath)) { [IO.File]::WriteAllText($configureTokenPath,'') }
    if ((Get-Item -LiteralPath $configureTokenPath).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Client credential must be a regular private file.' }
    $configureAcl = New-Object System.Security.AccessControl.FileSecurity
    $configureAcl.SetAccessRuleProtection($true,$false)
    $configureAcl.SetOwner((New-Object System.Security.Principal.SecurityIdentifier('S-1-5-32-544')))
    foreach ($configureSid in @('S-1-5-18','S-1-5-32-544')) {
        $configureAcl.AddAccessRule((New-Object System.Security.AccessControl.FileSystemAccessRule((New-Object System.Security.Principal.SecurityIdentifier($configureSid)),'FullControl','Allow')))
    }
    Set-Acl -LiteralPath $configureTokenPath -AclObject $configureAcl
    $configureEncoding = New-Object System.Text.UTF8Encoding($false)
    [IO.File]::WriteAllText($configureTokenPath,$configureToken,$configureEncoding)
    if ($null -eq $configureConfig.tools) {
        $configureTools = [pscustomobject]@{
            hotfolder = [pscustomobject]@{root=(Join-Path $configureConfig.stateDirectory 'hotfolder');maxFileBytes=65536;maxEntries=256;extensions=@('txt','md','json','csv')}
            gatewayUrl = $configureConfig.serverUrl
            gatewayTokenPath = $configureTokenPath
            gatewayCaPath = $configureConfig.caCertificatePath
            deviceId = $DeviceId
            authorizationProfiles = $configureSelected
            webSearchTokenPath = $null
            webSearchAllowedDomains = @()
        }
    } else {
        $configureTools = $configureConfig.tools
        $configureTools.authorizationProfiles = @($configureTools.authorizationProfiles | Where-Object { $_.tool.id -notin @('hotfolder.write_text','client.read_log_entry') }) + $configureSelected
        $configureTools.gatewayTokenPath = $configureTokenPath
        $configureTools.deviceId = $DeviceId
    }
    $configureConfig | Add-Member -MemberType NoteProperty -Name tools -Value $configureTools -Force
    $configureJson = $configureConfig | ConvertTo-Json -Depth 40 -Compress
    if ($configureEncoding.GetByteCount($configureJson) -gt 65536) { throw 'Client configuration exceeds the supported size.' }
    # Keep the enrollment, CA and IPC peers; preserve file ACLs and restore config on startup failure.
    if ((Get-Service OloToolGateClient).Status -ne 'Stopped') { Stop-Service OloToolGateClient }
    try {
        [IO.File]::WriteAllText($configurePath,$configureJson,$configureEncoding)
        Start-Service OloToolGateClient
        Start-Sleep -Seconds 2
        $configureHealth = & $configureBinary health 2>$null | ConvertFrom-Json
        if ($LASTEXITCODE -ne 0 -or -not $configureHealth.ready) { throw 'The configured client service did not become ready.' }
    } catch {
        if ((Get-Service OloToolGateClient).Status -ne 'Stopped') { Stop-Service OloToolGateClient }
        [IO.File]::WriteAllText($configurePath,$configureOriginal,$configureEncoding)
        Start-Service OloToolGateClient
        throw
    }
    $configureSelected | ConvertTo-Json -Depth 40 -Compress | Set-Content -LiteralPath (Join-Path $configureRoot '.dev/debug/configured-client-profiles.json') -Encoding UTF8
    @{success=$true;deviceId=$DeviceId} | ConvertTo-Json | Set-Content -LiteralPath $configureResult -Encoding UTF8
} catch {
    @{success=$false;message='Client tool configuration failed; existing configuration was preserved or restored. Check the installed client and local debug agent credential.'} | ConvertTo-Json | Set-Content -LiteralPath $configureResult -Encoding UTF8
    throw
}
