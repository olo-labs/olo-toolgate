# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param([ValidateSet('deploy','undeploy','update','status','logs')][string]$Operation='deploy',[switch]$SkipPull)
$ErrorActionPreference='Stop'
Push-Location $PSScriptRoot
try {
    Get-Command docker -ErrorAction Stop | Out-Null
    function Invoke-Docker { & docker @args; if ($LASTEXITCODE -ne 0) { throw "Docker operation failed ($LASTEXITCODE)." } }
    Invoke-Docker info | Out-Null
    if (!(Test-Path .env)) { Copy-Item .env.example .env }
    if ($Operation -eq 'undeploy') { Invoke-Docker compose down; return }
    if ($Operation -eq 'status') { Invoke-Docker compose ps; return }
    if ($Operation -eq 'logs') { Invoke-Docker compose logs --tail 100; return }
    Invoke-Docker compose config --quiet
    if (!$SkipPull) {
        $images=@(& docker compose config --images)
        if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve Compose images.' }
        foreach ($image in $images) {
            if (!$image.Contains('/')) {
                & docker image inspect $image *> $null
                if ($LASTEXITCODE -ne 0) { throw "Local-only image '$image' is missing. Build it first, or set QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev in .env." }
            } else { Invoke-Docker pull $image }
        }
    }
    Invoke-Docker compose up -d --wait --wait-timeout 180
    if (Test-Path check-options.py) { Invoke-Docker compose exec -T quickstart /opt/quickstart-python/bin/python /opt/quickstart/check-options.py }
    Write-Host 'Deployment ready. See README.md for external configuration and reviewed access.'
} finally { Pop-Location }
