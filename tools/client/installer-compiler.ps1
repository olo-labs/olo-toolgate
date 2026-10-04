# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
$ErrorActionPreference = 'Stop'
$toolgateCompilerDirectory = Join-Path $env:RUNNER_TEMP 'toolgate-inno-6.7.1'
$toolgateCompilerSetup = Join-Path $env:RUNNER_TEMP 'innosetup-6.7.1.exe'
Invoke-WebRequest -Uri 'https://github.com/jrsoftware/issrc/releases/download/is-6_7_1/innosetup-6.7.1.exe' -OutFile $toolgateCompilerSetup
# Checksum published in Microsoft's winget package manifest for this exact release.
if ((Get-FileHash -LiteralPath $toolgateCompilerSetup -Algorithm SHA256).Hash.ToLowerInvariant() -ne '4d11e8050b6185e0d49bd9e8cc661a7a59f44959a621d31d11033124c4e8a7b0') {
    throw 'Installer compiler checksum mismatch'
}
$toolgateCompilerProcess = Start-Process -FilePath $toolgateCompilerSetup -ArgumentList @('/CURRENTUSER','/VERYSILENT','/SUPPRESSMSGBOXES','/NORESTART',('/DIR="' + $toolgateCompilerDirectory + '"'),'/NOICONS','/TASKS=""') -WindowStyle Hidden -Wait -PassThru
if ($toolgateCompilerProcess.ExitCode -ne 0) { throw 'Installer compiler installation failed' }
$toolgateCompilerPath = Join-Path $toolgateCompilerDirectory 'ISCC.exe'
if (!(Test-Path -LiteralPath $toolgateCompilerPath)) { throw 'Installer compiler missing' }
if ($env:GITHUB_ENV) { "CLIENT_ISCC_PATH=$toolgateCompilerPath" | Out-File -FilePath $env:GITHUB_ENV -Append -Encoding utf8 }
