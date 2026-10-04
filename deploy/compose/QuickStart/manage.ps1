# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param([ValidateSet('deploy','undeploy','update','status','logs','configure','token','policy')][string]$Operation = 'deploy', [switch]$SkipPull)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    Get-Command docker -ErrorAction Stop | Out-Null
    function Invoke-Docker { & docker @args; if ($LASTEXITCODE -ne 0) { throw "Docker operation failed ($LASTEXITCODE)." } }
    Invoke-Docker info | Out-Null
    Invoke-Docker compose version
    if (!(Test-Path .env)) { Copy-Item .env.example .env }
    New-Item -ItemType Directory -Force env | Out-Null
    $stack = Test-Path external-db.yaml
    $composeArgs = @('compose','-f','compose.yaml')
    if ($stack) {
        New-Item -ItemType Directory -Force env | Out-Null
        $modePath = 'env/mode.txt'
        if ($Operation -eq 'configure' -or (!(Test-Path $modePath) -and $Operation -eq 'deploy')) {
            $choice = Read-Host 'Deploy bundled database and dependency containers? [Y/n]'
            $bundled = $choice -notmatch '^(n|no)$'
            $proxy = $true
            if (!$bundled) {
                function Input-Value($Label,$Default) { $value=Read-Host "$Label [$Default]"; if (!$value) { $value=$Default }; if ($value.Contains("`n") -or $value.Contains("`r")) { throw 'Single-line values required.' }; return $value }
                $dbHost = Input-Value 'Existing PostgreSQL host (reachable from container)' 'host.docker.internal'
                if ($dbHost -notmatch '^[a-zA-Z0-9.-]+$') { throw 'Use a hostname or IPv4 address.' }
                $port = Input-Value 'PostgreSQL port' '5432'
                if ($port -notmatch '^\d+$' -or [int]$port -lt 1 -or [int]$port -gt 65535) { throw 'Invalid port.' }
                $dbName = Input-Value 'Database name' 'control'
                $appUser = Input-Value 'Restricted runtime database user' 'control_app'
                $migrationUser = Input-Value 'Migration database user (must differ)' 'control_migrator'
                foreach ($value in @($dbName,$appUser,$migrationUser)) { if ($value -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') { throw 'Use simple SQL identifiers.' } }
                if ($appUser -eq $migrationUser) { throw 'Separate runtime and migration roles required.' }
                function Password($label) {
                    $secret=Read-Host $label -AsSecureString
                    $value=[System.Net.NetworkCredential]::new('', $secret).Password
                    if (!$value -or $value.Contains("`n") -or $value.Contains("`r")) { throw 'Nonempty single-line password required.' }
                    return $value
                }
                $appPassword = Password 'Runtime database password'
                $migrationPassword = Password 'Migration database password'
                $ssl = Input-Value 'Database TLS mode: verify-full / require / disable' 'verify-full'
                if ($ssl -notin @('verify-full','require','disable')) { throw 'Invalid TLS mode.' }
                $url = "jdbc:postgresql://${dbHost}:${port}/${dbName}?sslmode=$ssl"
                if ($ssl -eq 'verify-full') {
                    $caPath = Read-Host 'Path to PostgreSQL CA certificate (PEM)'
                    if (!(Test-Path -LiteralPath $caPath -PathType Leaf)) { throw 'CA certificate file required.' }
                    Copy-Item -LiteralPath $caPath -Destination env/db-ca.crt
                    $url += '&sslrootcert=/external/db-ca.crt'
                }
                $lines = @("QUARKUS_DATASOURCE_JDBC_URL=$url","QUARKUS_DATASOURCE_USERNAME=$appUser","QUARKUS_DATASOURCE_PASSWORD=$appPassword","QUARKUS_FLYWAY_USERNAME=$migrationUser","QUARKUS_FLYWAY_PASSWORD=$migrationPassword","TOOLGATE_CONTROL_DEVELOPMENT_MODE=$($ssl -ne 'verify-full')".ToLower())
                [IO.File]::WriteAllText((Join-Path $PWD 'env/.env.db'), ($lines -join "`n")+"`n", [Text.UTF8Encoding]::new($false))
                $redisUrl = Input-Value 'Existing Redis URL (redis:// or rediss://)' 'redis://host.docker.internal:6379/0'
                if ($redisUrl -notmatch '^rediss?://[^\r\n]+$') { throw 'Redis URL required.' }
                $redisSecret = Read-Host 'Redis password (empty if none)' -AsSecureString
                $redisPassword = [System.Net.NetworkCredential]::new('', $redisSecret).Password
                if ($redisPassword.Contains("`n") -or $redisPassword.Contains("`r")) { throw 'Single-line password required.' }
                [IO.File]::WriteAllText((Join-Path $PWD 'env/.env.cache'), "TOOLGATE_CACHE_MODE=redis`nTOOLGATE_REDIS_URL=$redisUrl`nTOOLGATE_REDIS_PASSWORD=$redisPassword`n", [Text.UTF8Encoding]::new($false))
                $proxy = (Read-Host 'Deploy bundled HTTPS proxy? [Y/n]') -notmatch '^(n|no)$'
                if (!$proxy) {
                    $publicUrl = Input-Value 'Existing HTTPS proxy public origin' 'https://localhost:8444'
                    if ($publicUrl -notmatch '^https://[a-zA-Z0-9.-]+(:\d+)?$') { throw 'Exact HTTPS origin required.' }
                    [IO.File]::WriteAllText((Join-Path $PWD 'env/proxy-url.txt'), $publicUrl, [Text.UTF8Encoding]::new($false))
                }
            }
            $mode = @($(if ($bundled) {'bundled'} else {'external'}), $(if ($proxy) {'proxy'} else {'external-proxy'})) -join "`n"
            [IO.File]::WriteAllText((Join-Path $PWD $modePath), $mode+"`n", [Text.UTF8Encoding]::new($false))
        }
        if (!(Test-Path $modePath)) { throw 'Run deploy to configure this stack first.' }
        $mode = Get-Content $modePath
        if ($mode[0] -eq 'bundled') { $composeArgs += @('--profile','database') }
        elseif ($mode[0] -eq 'external') { $composeArgs += @('-f','external-db.yaml') }
        else { throw 'Invalid saved database mode.' }
        if ($mode[1] -eq 'proxy') { $composeArgs += @('--profile','proxy') }
        elseif ($mode[1] -eq 'external-proxy') { $composeArgs += @('-f','external-proxy.yaml') }
        else { throw 'Invalid saved proxy mode.' }
    }
    if ($Operation -eq 'configure') { Write-Host 'Configuration saved. Run deploy to apply it.'; return }
    if ($Operation -eq 'undeploy') { Invoke-Docker @composeArgs down; return }
    if ($Operation -eq 'status') { Invoke-Docker @composeArgs ps; return }
    if ($Operation -eq 'logs') { Invoke-Docker @composeArgs logs --tail 100; return }
    if ($Operation -eq 'token') { if (!$stack) { throw 'Token command is for GatewayControl.' }; Invoke-Docker @composeArgs run --rm --no-deps token; return }
    if ($Operation -eq 'policy') {
        if (!$stack) { throw 'Policy command is for GatewayControl.' }
        Invoke-Docker @composeArgs run --rm --no-deps setup apply-policy
        Invoke-Docker @composeArgs restart gateway
        Invoke-Docker @composeArgs run --rm --no-deps probe
        return
    }
    if ($stack) {
        if (!$SkipPull) { Invoke-Docker @composeArgs pull setup }
        Invoke-Docker @composeArgs run --rm --no-deps setup init
        if ($mode[0] -eq 'bundled') { Invoke-Docker @composeArgs run --rm --no-deps setup export-db }
    }
    Invoke-Docker @composeArgs config --quiet
    if (!$SkipPull) { Invoke-Docker @composeArgs pull }
    Invoke-Docker @composeArgs up -d --wait --wait-timeout 180
    if ($stack) { Invoke-Docker @composeArgs run --rm --no-deps probe }
    else { Invoke-Docker @composeArgs exec -T quickstart /opt/quickstart-python/bin/python /opt/quickstart/check-options.py }
    Write-Host 'Deployment ready. See README.md for URLs, credentials and operations.'
} finally { Pop-Location }
