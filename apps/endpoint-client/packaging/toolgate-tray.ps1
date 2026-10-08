# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Runs in the signed-in user's session; the service owns all credentials.
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
$trayMutex = New-Object System.Threading.Mutex($false, 'Local\OloToolGateTray')
if (-not $trayMutex.WaitOne(0)) { $trayMutex.Dispose(); exit }
$trayRoot = Split-Path $PSScriptRoot -Parent
$trayClient = Join-Path $trayRoot 'olo-toolgate-client.exe'
$trayConfig = 'C:\ProgramData\OLO\ToolGate\client.json'
$script:trayProcess = $null
$script:trayStarted = $null
$script:trayDetail = 'Checking the ToolGate service...'
$script:trayState = ''
. (Join-Path $PSScriptRoot 'toolgate-icons.ps1')
$trayLogo = Join-Path $PSScriptRoot 'olo.png'
$trayConnectedIcon = New-ToolGateStatusIcon -LogoPath $trayLogo -Connected $true
$trayOfflineIcon = New-ToolGateStatusIcon -LogoPath $trayLogo -Connected $false
$trayIcon = New-Object System.Windows.Forms.NotifyIcon
$trayIcon.Icon = $trayOfflineIcon
$trayIcon.Text = 'ToolGate: checking service'
$trayIcon.Visible = $true
$trayMenu = New-Object System.Windows.Forms.ContextMenuStrip
$trayStatus = $trayMenu.Items.Add('Show status')
$trayStatus.add_Click({
    [System.Windows.Forms.MessageBox]::Show($script:trayDetail, 'OLO ToolGate') | Out-Null
})
$trayConsole = $trayMenu.Items.Add('Open ToolGate console')
$trayEnroll = $trayMenu.Items.Add('Enroll this device')
$trayEnroll.add_Click({
    Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -ArgumentList @('-NoProfile', '-STA', '-ExecutionPolicy', 'RemoteSigned', '-WindowStyle', 'Hidden', '-File', "`"$(Join-Path $PSScriptRoot 'toolgate-enroll.ps1')`"") -WindowStyle Hidden
})
$trayConsole.add_Click({
    $trayServer = (Get-ItemProperty -LiteralPath 'HKLM:\Software\OLO\ToolGate' -ErrorAction SilentlyContinue).ServerUrl
    if ($trayServer -match '^https://[a-zA-Z0-9.\[\]:/-]+$') {
        if ($trayServer -eq 'https://localhost:18450') { $trayServer = 'http://127.0.0.1:18090' }
        $trayRoute = if ($script:trayState -in @('UNENROLLED', 'PENDING')) { '/console/#enroll' } else { '/console/' }
        Start-Process ($trayServer.TrimEnd('/') + $trayRoute)
    }
})
$trayPackets = $trayMenu.Items.Add('View messages sent / received')
$trayPackets.add_Click({
    $trayPacketScript = Join-Path $PSScriptRoot 'toolgate-packets.ps1'
    try {
        Start-Process -FilePath "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" -Verb RunAs -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'RemoteSigned', '-File', "`"$trayPacketScript`"")
    } catch { $script:trayDetail = 'Packet viewer was cancelled or could not start.' }
})
$trayUninstall = $trayMenu.Items.Add('Uninstall ToolGate...')
$trayUninstall.add_Click({
    $trayUninstaller = Join-Path $trayRoot 'unins000.exe'
    try { Start-Process -FilePath $trayUninstaller -Verb RunAs }
    catch { $script:trayDetail = 'Uninstall was cancelled or could not start.' }
})
$trayExit = $trayMenu.Items.Add('Exit tray icon')
$trayExit.add_Click({ [System.Windows.Forms.Application]::ExitThread() })
$trayIcon.ContextMenuStrip = $trayMenu
$trayIcon.add_DoubleClick({ $trayStatus.PerformClick() })
$trayTimer = New-Object System.Windows.Forms.Timer
$trayTimer.Interval = 2000
$trayTimer.add_Tick({
    if (-not (Test-Path -LiteralPath $trayConfig)) {
        [System.Windows.Forms.Application]::ExitThread()
        return
    }
    if ($script:trayProcess) {
        if (-not $script:trayProcess.HasExited) {
            if (((Get-Date) - $script:trayStarted).TotalSeconds -lt 22) { return }
            $script:trayProcess.Kill()
        }
        try {
            $trayHealth = $script:trayProcess.StandardOutput.ReadToEnd() | ConvertFrom-Json
            if ($trayHealth.error -or -not $trayHealth.state) { throw 'Health unavailable' }
            $trayState = [string]$trayHealth.state
            $script:trayState = $trayState
            $trayStatusText = switch ($trayState) {
                'UNENROLLED' { 'Enrollment required' }
                'PENDING' { 'Waiting for enrollment approval' }
                'REVOKED' { 'Enrollment revoked' }
                'OFFLINE' { 'Offline - waiting for gateway' }
                'ACTIVE' { if ($trayHealth.ready) { 'Connected' } else { 'Waiting for gateway check-in' } }
                default { 'Checking connection' }
            }
            $trayGuidance = switch ($trayState) {
                'UNENROLLED' { 'Choose Enroll this device from the tray menu, then approve it in the console.' }
                'PENDING' { 'Approve the enrollment code and fingerprint on Enroll Device.' }
                'REVOKED' { 'Contact your administrator to enroll this device again.' }
                default { if ($trayHealth.ready) { 'Protected tools are ready.' } else { 'Protected tools will be ready after enrollment and a successful gateway check-in.' } }
            }
            $script:trayDetail = "Service running`r`nStatus: $trayStatusText`r`n$trayGuidance`r`nSuccessful check-ins: $($trayHealth.successfulCheckIns)`r`nFailed check-ins: $($trayHealth.failedCheckIns)"
            $trayIcon.Text = 'ToolGate: ' + $trayStatusText
            $trayIcon.Icon = if ($trayHealth.ready -and $trayState -eq 'ACTIVE') { $trayConnectedIcon } else { $trayOfflineIcon }
        } catch {
            $trayService = Get-Service -Name OloToolGateClient -ErrorAction SilentlyContinue
            if ($trayService.Status -eq 'Running') {
                $script:trayDetail = 'ToolGate service is running. Its status is busy or unavailable to this Windows account.'
                $trayIcon.Text = 'ToolGate: service running, status pending'
                $trayIcon.Icon = $trayOfflineIcon
            } else {
                $script:trayDetail = 'ToolGate service is unavailable.'
                $trayIcon.Text = 'ToolGate: service unavailable'
                $trayIcon.Icon = $trayOfflineIcon
            }
        } finally {
            $script:trayProcess.Dispose()
            $script:trayProcess = $null
        }
    }
    $trayStart = New-Object System.Diagnostics.ProcessStartInfo
    $trayStart.FileName = $trayClient
    $trayStart.Arguments = 'health'
    $trayStart.UseShellExecute = $false
    $trayStart.CreateNoWindow = $true
    $trayStart.RedirectStandardOutput = $true
    $trayStart.RedirectStandardError = $true
    try {
        $script:trayProcess = [System.Diagnostics.Process]::Start($trayStart)
        $script:trayStarted = Get-Date
    } catch { $script:trayDetail = 'Could not contact the ToolGate service.'; $trayIcon.Icon = $trayOfflineIcon }
})
try {
    $trayTimer.Start()
    [System.Windows.Forms.Application]::Run()
} finally {
    $trayTimer.Stop()
    $trayTimer.Dispose()
    if ($script:trayProcess) {
        if (-not $script:trayProcess.HasExited) { $script:trayProcess.Kill() }
        $script:trayProcess.Dispose()
    }
    $trayIcon.Visible = $false
    $trayIcon.Dispose()
    $trayConnectedIcon.Dispose()
    $trayOfflineIcon.Dispose()
    $trayMenu.Dispose()
    $trayMutex.ReleaseMutex()
    $trayMutex.Dispose()
}
