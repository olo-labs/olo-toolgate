# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Run explicitly as Administrator on a Windows device enrolled to THIS example.
param([string]$Gateway = 'https://localhost:18451')
$ErrorActionPreference = 'Stop'
$identity = [Security.Principal.WindowsIdentity]::GetCurrent()
if (-not ([Security.Principal.WindowsPrincipal]$identity).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw 'Run prepare-windows.ps1 in an Administrator PowerShell.'
}
$configPath = Join-Path $env:ProgramData 'OLO/ToolGate/client.json'
$binary = Join-Path $env:ProgramFiles 'OLO/ToolGate/olo-toolgate-client.exe'
if ((Get-Item -LiteralPath $configPath).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Use a regular protected client configuration.' }
$original = [IO.File]::ReadAllText($configPath)
$config = $original | ConvertFrom-Json
if ($config.serverUrl.TrimEnd('/') -ne $Gateway.TrimEnd('/')) { throw 'Enroll this device to the example Gateway first. This helper does not switch gateways or install clients.' }
$journal = Get-Content -LiteralPath (Join-Path $config.stateDirectory 'journal.json') -Raw | ConvertFrom-Json
$deviceId = $journal.identity.deviceId
if ($deviceId -notmatch '^device-[a-f0-9]{32}$') { throw 'Complete enrollment on this example Gateway before preparing its tools.' }
$allProfiles = & $binary authorization-profiles | ConvertFrom-Json
if ($LASTEXITCODE -ne 0) { throw 'Could not export profiles from the installed executable.' }
$names = @('hotfolder.list','hotfolder.read_text','hotfolder.write_text','client.read_log_entry')
$selected = @($allProfiles | Where-Object { $_.tool.id -in $names })
if ($selected.Count -ne 4) { throw 'The installed client does not contain the four example tools.' }
$folder = if ($config.tools) { $config.tools.hotfolder.root } else { Join-Path $config.stateDirectory 'hotfolder' }
if (-not (Test-Path -LiteralPath $folder)) { [void](New-Item -ItemType Directory -Path $folder) }
if ((Get-Item -LiteralPath $folder).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Use a regular protected HotFolder.' }
$tokenPath = Join-Path $config.stateDirectory 'langchain-local-ipc-disabled-token'
if (-not (Test-Path -LiteralPath $tokenPath)) {
    # An inert local IPC credential; remote operations use the enrolled device's mTLS identity.
    $bytes = New-Object byte[] 48
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    [IO.File]::WriteAllText($tokenPath,[Convert]::ToBase64String($bytes).Replace('+','-').Replace('/','_').TrimEnd('='))
}
if ((Get-Item -LiteralPath $tokenPath).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Use a regular token file.' }
$acl = New-Object Security.AccessControl.FileSecurity
$acl.SetAccessRuleProtection($true,$false)
foreach ($sid in @('S-1-5-18','S-1-5-32-544')) {
    $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule((New-Object Security.Principal.SecurityIdentifier($sid)),'FullControl','Allow')))
}
Set-Acl -LiteralPath $tokenPath -AclObject $acl
if (-not $config.tools) {
    $tools = [pscustomobject]@{
        hotfolder=[pscustomobject]@{root=$folder;maxFileBytes=65536;maxEntries=256;extensions=@('txt','md','json','csv')}
        gatewayUrl=$config.serverUrl;gatewayTokenPath=$tokenPath;gatewayCaPath=$config.caCertificatePath
        deviceId='remote';authorizationProfiles=$selected;webSearchTokenPath=$null;webSearchAllowedDomains=@()
    }
    $config | Add-Member -MemberType NoteProperty -Name tools -Value $tools -Force
} else {
    $config.tools.authorizationProfiles = @($config.tools.authorizationProfiles | Where-Object { $_.tool.id -notin $names }) + $selected
    $config.tools.hotfolder.extensions = @(@($config.tools.hotfolder.extensions) + @('txt','md','json','csv') | Sort-Object -Unique)
}
$encoding = New-Object Text.UTF8Encoding($false)
$json = $config | ConvertTo-Json -Depth 40 -Compress
if ($encoding.GetByteCount($json) -gt 65536) { throw 'Client configuration exceeds the supported size.' }
Stop-Service OloToolGateClient
try {
    [IO.File]::WriteAllText($configPath,$json,$encoding)
    Start-Service OloToolGateClient
    $deadline = [DateTime]::UtcNow.AddSeconds(30)
    do {
        Start-Sleep -Seconds 1
        $health = & $binary health 2>$null | ConvertFrom-Json
        $ready = $LASTEXITCODE -eq 0 -and $health.ready
    } until ($ready -or [DateTime]::UtcNow -ge $deadline)
    if (-not $ready) { throw 'Configured client is not ready.' }
} catch {
    if ((Get-Service OloToolGateClient).Status -ne 'Stopped') { Stop-Service OloToolGateClient }
    [IO.File]::WriteAllText($configPath,$original,$encoding)
    Start-Service OloToolGateClient
    throw
}
foreach ($fixture in Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'fixtures') -File) {
    $destination = Join-Path $folder $fixture.Name
    if (-not (Test-Path -LiteralPath $destination)) { Copy-Item -LiteralPath $fixture.FullName -Destination $destination }
}
$output = Join-Path $PSScriptRoot '.state/windows-profiles.json'
[void](New-Item -ItemType Directory -Force -Path (Split-Path $output -Parent))
function Write-PublicRegistration([string]$Path, [string]$Json) {
    if ((Test-Path -LiteralPath $Path) -and ((Get-Item -LiteralPath $Path).Attributes -band [IO.FileAttributes]::ReparsePoint)) {
        throw 'Use a regular public registration file.'
    }
    [IO.File]::WriteAllText($Path,$Json,$encoding)
    # Elevated files may be owned by Administrators. The ordinary launcher must
    # still read its public exports without requiring another elevated shell.
    $publicAcl = New-Object Security.AccessControl.FileSecurity
    $publicAcl.SetAccessRuleProtection($true,$false)
    foreach ($sid in @($identity.User.Value,'S-1-5-18','S-1-5-32-544')) {
        $publicAcl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule((New-Object Security.Principal.SecurityIdentifier($sid)),'FullControl','Allow')))
    }
    Set-Acl -LiteralPath $Path -AclObject $publicAcl
}
Write-PublicRegistration $output ($selected | ConvertTo-Json -Depth 40 -Compress)
$registration = Join-Path $PSScriptRoot ".state/devices/$deviceId.json"
[void](New-Item -ItemType Directory -Force -Path (Split-Path $registration -Parent))
# host lets setup on this machine follow the client's own profiles after an update.
$publicDevice = [pscustomobject]@{deviceId=$deviceId;platform='windows';gateway=$config.serverUrl.TrimEnd('/');host=$env:COMPUTERNAME;profiles=$selected}
Write-PublicRegistration $registration ($publicDevice | ConvertTo-Json -Depth 40 -Compress)
Write-Host "Prepared existing tools and fictional inputs. Generated reports will be in: $folder"
Write-Host "Public installed profiles: $output"
Write-Host "Public group-selection registration: $registration"
Write-Host 'Approve this device in the example console, then run deploy.bat -Win and execute.bat -Win. Setup assigns both prepared devices to ReadAndWriteDeviceGroup through independent review.'
