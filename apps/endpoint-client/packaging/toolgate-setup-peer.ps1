# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Bind the installed client to the desktop user even when UAC uses another administrator account.
$ErrorActionPreference = 'Stop'
$setupSession = (Get-Process -Id $PID).SessionId
$setupDesktop = @(Get-CimInstance Win32_Process -Filter "Name = 'explorer.exe'" | Where-Object { $_.SessionId -eq $setupSession })
$setupPeers = @($setupDesktop | ForEach-Object {
    $setupOwner = Invoke-CimMethod -InputObject $_ -MethodName GetOwnerSid
    if ($setupOwner.ReturnValue -ne 0) { throw 'Cannot identify the Windows desktop account.' }
    $setupOwner.Sid
} | Select-Object -Unique)
if ($setupPeers.Count -gt 1) { throw 'Multiple desktop accounts found. Use /PEER to select the intended account.' }
if ($setupPeers.Count -eq 1) { Write-Output $setupPeers[0] }
else { Write-Output ([Security.Principal.WindowsIdentity]::GetCurrent().User.Value) }
