# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# First-time native setup followed by the actual agent-facing mimic requests.
$ErrorActionPreference = 'Stop'
$setupRoot = Split-Path $PSScriptRoot -Parent
$setupLocal = Join-Path $setupRoot '.dev/debug'
$setupResult = Join-Path $setupLocal ("native-prepare-" + [Guid]::NewGuid().ToString('N') + '.json')
. "$PSScriptRoot/console-log.ps1"
$setupLogging = $false
function Test-CurrentLocalClient {
    # A completed repair can resume without another administrator prompt. Compare the actual
    # service and packaged binaries with the checksummed native release, and the configured CA.
    try {
        $setupVersion = (Get-Content -LiteralPath "$setupRoot/VERSION" -Raw).Trim()
        $setupArchive = "$setupRoot/deploy/client-assets/release/olo-toolgate-client-$setupVersion-x86_64-pc-windows-gnu.zip"
        $setupArchiveHash = (Get-Content -LiteralPath ($setupArchive + '.sha256') -Raw).Trim().Split(' ')[0]
        if ((Get-FileHash -LiteralPath $setupArchive -Algorithm SHA256).Hash -ne $setupArchiveHash) { return $false }
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        $setupZip = [IO.Compression.ZipFile]::OpenRead($setupArchive)
        try {
            $setupEntry = $setupZip.GetEntry('olo-toolgate-client.exe')
            if (-not $setupEntry) { return $false }
            $setupStream = $setupEntry.Open()
            $setupHasher = [Security.Cryptography.SHA256]::Create()
            try { $setupBinaryHash = [BitConverter]::ToString($setupHasher.ComputeHash($setupStream)).Replace('-', '') }
            finally { $setupStream.Dispose(); $setupHasher.Dispose() }
        } finally { $setupZip.Dispose() }
        foreach ($setupBinary in @('C:\Program Files\OLO\ToolGateSetup\olo-toolgate-client.exe', 'C:\Program Files\OLO\ToolGate\olo-toolgate-client.exe')) {
            if ((Get-FileHash -LiteralPath $setupBinary -Algorithm SHA256).Hash -ne $setupBinaryHash) { return $false }
        }
        $setupConfig = Get-Content -LiteralPath 'C:\ProgramData\OLO\ToolGate\client.json' -Raw | ConvertFrom-Json
        if ($setupConfig.serverUrl -ne 'https://localhost:18450' -or -not $setupConfig.caCertificatePath) { return $false }
        $setupInstalledCa = New-Object Security.Cryptography.X509Certificates.X509Certificate2($setupConfig.caCertificatePath)
        $setupGatewayCa = New-Object Security.Cryptography.X509Certificates.X509Certificate2("$setupLocal/toolgate-quickstart-ca.crt")
        try {
            if ($setupInstalledCa.Thumbprint -ne $setupGatewayCa.Thumbprint -or (Get-Service OloToolGateClient).Status -ne 'Running') { return $false }
            $setupHealth = & 'C:\Program Files\OLO\ToolGateSetup\olo-toolgate-client.exe' health | ConvertFrom-Json
            if ($LASTEXITCODE -ne 0 -or $setupHealth.error -or -not $setupHealth.state) { return $false }
            # Matching binary/config CA alone cannot prove enrollment survived a fresh data volume.
            return $setupHealth.ready -or $setupHealth.state -in @('UNENROLLED', 'PENDING')
        } finally { $setupInstalledCa.Dispose(); $setupGatewayCa.Dispose() }
    } catch { return $false }
}
try {
    $setupLog = Start-DebugConsoleLog -Name 'setup-agent-mimic'
    $setupLogging = $true
    [void](New-Item -ItemType Directory -Path $setupLocal -Force)
    $setupCompose = @('compose', '--project-name', 'toolgate-debug', '--project-directory', $PSScriptRoot, '-f', "$PSScriptRoot/compose.yaml")
    $setupContainer = & docker @setupCompose ps -q quickstart
    if ($LASTEXITCODE -ne 0 -or -not $setupContainer) { throw 'Start the debug Gateway with debug/start.bat first.' }
    $setupExitCode = Invoke-LoggedDebugCommand -FilePath 'docker' -Arguments @('cp', "${setupContainer}:/data/keys/device-ca.crt", "$setupLocal/toolgate-quickstart-ca.crt")
    if ($setupExitCode -ne 0) { throw 'Could not export the debug Gateway public CA.' }
    if (Test-CurrentLocalClient) {
        Write-Host 'The current client and Gateway CA are already installed. Continuing enrollment and agent setup.'
    } else {
        $setupExitCode = Invoke-LoggedDebugCommand -FilePath 'powershell' -Arguments @('-NoProfile', '-ExecutionPolicy', 'RemoteSigned', '-File', "$PSScriptRoot/prepare-local-client.ps1", '-ResultFile', $setupResult)
        if ($setupExitCode -ne 0) { throw 'Windows client repair failed. Inspect the repair result and .dev/debug/native-installer.log.' }
        $setupRepair = Get-Content -LiteralPath $setupResult -Raw | ConvertFrom-Json
        if (-not $setupRepair.success) { throw $setupRepair.message }
    }
    $setupTray = 'C:\Program Files\OLO\ToolGateSetup\packaging\toolgate-tray.ps1'
    Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @('-NoProfile', '-STA', '-ExecutionPolicy', 'RemoteSigned', '-File', "`"$setupTray`"") -WindowStyle Hidden
    $setupPython = Join-Path $setupLocal 'venv/Scripts/python.exe'
    if (-not (Test-Path -LiteralPath $setupPython)) { . "$PSScriptRoot/python.ps1"; $setupPython = Find-DebugPython }
    $setupExitCode = Invoke-LoggedDebugCommand -FilePath $setupPython -Arguments @("$PSScriptRoot/enroll-mimic-client.py")
    if ($setupExitCode -ne 0) { throw 'Enrollment or the exact agent/device tool grants could not be prepared.' }
    $setupExitCode = Invoke-LoggedDebugCommand -FilePath 'powershell' -Arguments @('-NoProfile', '-ExecutionPolicy', 'RemoteSigned', '-File', "$PSScriptRoot/agent-mimic.ps1")
    if ($setupExitCode -ne 0) { throw 'Agent mimic failed. Inspect client/server packet diagnostics.' }
    Write-Host 'PASS: agent discovery, client file creation and one client log entry.' -ForegroundColor Green
} catch {
    Write-Host $_ -ForegroundColor Red
    exit 1
} finally {
    if ($setupLogging) { Stop-Transcript | Out-Null }
}
