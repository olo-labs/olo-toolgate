# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Presentation only: activity is obtained through the authenticated service pipe.
function New-ToolGateWindow {
    param([string]$Title, [string]$LogoPath)
    $form = New-Object System.Windows.Forms.Form
    $form.Text = $Title
    $form.ClientSize = New-Object System.Drawing.Size(620, 480)
    $form.MinimumSize = New-Object System.Drawing.Size(540, 500)
    $form.StartPosition = 'CenterScreen'
    $form.AutoScaleMode = 'Dpi'
    $form.Font = New-Object System.Drawing.Font('Segoe UI', 10)
    $form.BackColor = [System.Drawing.Color]::White
    $layout = New-Object System.Windows.Forms.TableLayoutPanel
    $layout.Dock = 'Fill'
    $layout.Padding = New-Object System.Windows.Forms.Padding(20)
    $layout.ColumnCount = 1
    $layout.RowCount = 2
    $layout.RowStyles.Add((New-Object System.Windows.Forms.RowStyle('Absolute', 90))) | Out-Null
    $layout.RowStyles.Add((New-Object System.Windows.Forms.RowStyle('Percent', 100))) | Out-Null
    $header = New-Object System.Windows.Forms.TableLayoutPanel
    $header.Dock = 'Fill'
    $header.ColumnCount = 2
    $header.ColumnStyles.Add((New-Object System.Windows.Forms.ColumnStyle('Absolute', 115))) | Out-Null
    $header.ColumnStyles.Add((New-Object System.Windows.Forms.ColumnStyle('Percent', 100))) | Out-Null
    $logo = New-Object System.Windows.Forms.PictureBox
    $logo.Dock = 'Fill'
    $logo.SizeMode = 'Zoom'
    if (Test-Path -LiteralPath $LogoPath) { $logo.Image = [System.Drawing.Image]::FromFile($LogoPath) }
    $heading = New-Object System.Windows.Forms.Label
    $heading.Text = "OLO ToolGate`r`nDevice client"
    $heading.Font = New-Object System.Drawing.Font('Segoe UI', 17, ([System.Drawing.FontStyle]::Bold))
    $heading.Dock = 'Fill'
    $heading.TextAlign = 'MiddleLeft'
    $header.Controls.Add($logo, 0, 0)
    $header.Controls.Add($heading, 1, 0)
    $layout.Controls.Add($header, 0, 0)
    $form.Controls.Add($layout)
    $form.add_FormClosed({ if ($this.Tag.Logo.Image) { $this.Tag.Logo.Image.Dispose() } })
    $form.Tag = @{ Layout = $layout; Logo = $logo }
    return $form
}
function New-ToolGateStatusWindow {
    param([string]$LogoPath)
    $form = New-ToolGateWindow -Title 'OLO ToolGate - Device status' -LogoPath $LogoPath
    $tabs = New-Object System.Windows.Forms.TabControl
    $tabs.Dock = 'Fill'
    $status = New-Object System.Windows.Forms.TabPage('Status')
    $activity = New-Object System.Windows.Forms.TabPage('Activity log')
    $tabs.TabPages.AddRange(@($status, $activity))
    $body = New-Object System.Windows.Forms.TableLayoutPanel
    $body.Dock = 'Fill'
    $body.Padding = New-Object System.Windows.Forms.Padding(12)
    $body.ColumnCount = 1
    $body.RowCount = 5
    foreach ($height in @(110, 60, 28, 40)) { $body.RowStyles.Add((New-Object System.Windows.Forms.RowStyle('Absolute', $height))) | Out-Null }
    $body.RowStyles.Add((New-Object System.Windows.Forms.RowStyle('Percent', 100))) | Out-Null
    $detail = New-Object System.Windows.Forms.Label
    $detail.Dock = 'Fill'
    $detail.AutoEllipsis = $true
    $command = New-Object System.Windows.Forms.Label
    $command.Dock = 'Fill'
    $command.AutoEllipsis = $true
    $command.Font = New-Object System.Drawing.Font('Segoe UI', 11, ([System.Drawing.FontStyle]::Bold))
    $progress = New-Object System.Windows.Forms.ProgressBar
    $progress.Dock = 'Fill'
    $progress.MarqueeAnimationSpeed = 30
    $caption = New-Object System.Windows.Forms.Label
    $caption.Dock = 'Fill'
    $caption.AutoEllipsis = $true
    $close = New-Object System.Windows.Forms.Button
    $close.Text = 'Close'
    $close.Anchor = 'Bottom,Right'
    $close.AutoSize = $true
    $close.DialogResult = 'Cancel'
    $form.CancelButton = $close
    $close.add_Click({ $this.FindForm().Close() })
    $body.Controls.Add($detail, 0, 0)
    $body.Controls.Add($command, 0, 1)
    $body.Controls.Add($progress, 0, 2)
    $body.Controls.Add($caption, 0, 3)
    $body.Controls.Add($close, 0, 4)
    $status.Controls.Add($body)
    $logs = New-Object System.Windows.Forms.TextBox
    $logs.Multiline = $true
    $logs.ReadOnly = $true
    $logs.ScrollBars = 'Both'
    $logs.WordWrap = $false
    $logs.Dock = 'Fill'
    $logs.Font = New-Object System.Drawing.Font('Consolas', 9)
    $logs.AccessibleName = 'Recent device activity log'
    $activity.Controls.Add($logs)
    $form.Tag.Layout.Controls.Add($tabs, 0, 1)
    $form.Tag.Detail = $detail
    $form.Tag.Command = $command
    $form.Tag.Progress = $progress
    $form.Tag.Caption = $caption
    $form.Tag.Logs = $logs
    $form.Tag.Tabs = $tabs
    return $form
}
function Update-ToolGateStatusWindow {
    param($Form, [string]$Detail, $Activity, [bool]$Available)
    $ui = $Form.Tag
    $ui.Detail.Text = $Detail
    $ui.Progress.Visible = $false
    if (-not $Available) {
        $ui.Command.Text = 'Command status unavailable'
        $ui.Caption.Text = 'Waiting for the device service. Previously received activity may be stale.'
        return
    }
    $running = @($Activity.active)
    if ($running.Count -gt 0) {
        $current = $running[0]
        $ui.Command.Text = 'Executing: ' + $current.name
        if ($running.Count -gt 1) { $ui.Command.Text += " (+$($running.Count - 1) more)" }
        $ui.Progress.Visible = $true
        $elapsed = [Math]::Max(0, [Math]::Floor(([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - $current.startedAtUnixMs) / 1000))
        if ($null -ne $current.progressPercent) {
            $ui.Progress.Style = 'Continuous'
            $ui.Progress.Value = [Math]::Max(0, [Math]::Min(100, [int]$current.progressPercent))
            $ui.Caption.Text = "$($ui.Progress.Value)% complete - $elapsed seconds elapsed"
        } else {
            $ui.Progress.Style = 'Marquee'
            $ui.Caption.Text = "In progress - $elapsed seconds elapsed"
        }
    } elseif ($Activity.lastCommand) {
        $ui.Command.Text = 'Last command: ' + $Activity.lastCommand.name
        $finished = [DateTimeOffset]::FromUnixTimeMilliseconds([long]$Activity.lastCommand.finishedAtUnixMs).LocalDateTime
        $ui.Caption.Text = "$($Activity.lastCommand.state) - $($finished.ToString('g'))"
    } else {
        $ui.Command.Text = 'No command has executed yet'
        $ui.Caption.Text = 'The device is idle.'
    }
    $lines = @('Recent device activity (latest 100 events; arguments and secrets excluded)', '')
    if (-not $Activity.logAvailable) { $lines += 'Device log could not be saved. Contact your administrator.' }
    foreach ($entry in $Activity.events) {
        $when = [DateTimeOffset]::FromUnixTimeMilliseconds([long]$entry.timestampUnixMs).LocalDateTime.ToString('yyyy-MM-dd HH:mm:ss')
        $lines += "$when  $($entry.state)  $($entry.name)"
    }
    $ui.Logs.Text = $lines -join "`r`n"
}
function New-ToolGateAboutWindow {
    param([string]$LogoPath, [string]$Version)
    $form = New-ToolGateWindow -Title 'About OLO ToolGate' -LogoPath $LogoPath
    $text = New-Object System.Windows.Forms.Label
    $text.Dock = 'Fill'
    $text.Text = "OLO ToolGate Client`r`nVersion: $Version`r`n`r`nOLO Labs`r`nEnterprise tools and device access`r`n`r`nCopyright 2026 OLO Labs`r`nLicensed under Apache-2.0"
    $form.Tag.Layout.Controls.Add($text, 0, 1)
    return $form
}
