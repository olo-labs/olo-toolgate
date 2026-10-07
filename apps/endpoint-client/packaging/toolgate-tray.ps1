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
$trayIcon = New-Object System.Windows.Forms.NotifyIcon
$trayIcon.Icon = [System.Drawing.SystemIcons]::Information
$trayIcon.Text = 'ToolGate: checking service'
$trayIcon.Visible = $true
$trayMenu = New-Object System.Windows.Forms.ContextMenuStrip
$trayStatus = $trayMenu.Items.Add('Show status')
$trayStatus.add_Click({
    [System.Windows.Forms.MessageBox]::Show($script:trayDetail, 'OLO ToolGate') | Out-Null
})
$trayConsole = $trayMenu.Items.Add('Open ToolGate console')
$trayConsole.add_Click({
    $trayServer = (Get-ItemProperty -LiteralPath 'HKLM:\Software\OLO\ToolGate' -ErrorAction SilentlyContinue).ServerUrl
    if ($trayServer -match '^https://[a-zA-Z0-9.\[\]:/-]+$') {
        Start-Process ($trayServer.TrimEnd('/') + '/console/')
    }
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
            $script:trayDetail = "Service running`r`nState: $trayState`r`nReady: $($trayHealth.ready)`r`nSuccessful check-ins: $($trayHealth.successfulCheckIns)`r`nFailed check-ins: $($trayHealth.failedCheckIns)"
            $trayIcon.Text = 'ToolGate: ' + $trayState.ToLowerInvariant()
            $trayIcon.Icon = if ($trayHealth.ready) { [System.Drawing.SystemIcons]::Information } else { [System.Drawing.SystemIcons]::Warning }
        } catch {
            $script:trayDetail = 'ToolGate service is unavailable or this Windows account cannot access its status.'
            $trayIcon.Text = 'ToolGate: service unavailable'
            $trayIcon.Icon = [System.Drawing.SystemIcons]::Error
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
    } catch { $script:trayDetail = 'Could not contact the ToolGate service.' }
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
    $trayMenu.Dispose()
    $trayMutex.ReleaseMutex()
    $trayMutex.Dispose()
}
