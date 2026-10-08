# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Compatibility entry point: call the already configured device and log its responses.
param(
    [string]$Gateway = 'https://localhost:18450',
    [string]$TokenFile = '',
    [string]$CaFile = '',
    [string]$File = 'rahul-nigam.txt'
)
$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/agent-mimic.ps1" @PSBoundParameters
exit $LASTEXITCODE
