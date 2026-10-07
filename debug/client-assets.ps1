# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
param([Parameter(Mandatory=$true)][string]$Python)
$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$version = (Get-Content "$root/VERSION" -Raw).Trim()
$releases = Invoke-RestMethod 'https://api.github.com/repos/olo-labs/olo-toolgate/releases?per_page=30'
$release = $releases | Where-Object {
    -not $_.draft -and ($_.assets.name -contains 'installers.json') -and
    ($_.assets.name -contains "olo-toolgate-client-$version-x86_64-pc-windows-msvc.setup.exe")
} | Select-Object -First 1
if (-not $release) { throw "No published client installer bundle found for $version." }
$source = Join-Path $root '.dev/debug/client-assets'
New-Item -ItemType Directory -Force $source | Out-Null
foreach ($asset in $release.assets) {
    if ($asset.name -notmatch ('^olo-toolgate-client-' + [regex]::Escape($version) + '-[a-z0-9_-]+\.(zip|tar\.gz|setup\.exe|dmg|run)(\.sha256)?$')) { continue }
    Write-Host "Preparing $($asset.name)"
    & curl.exe --fail --location --silent --show-error --retry 2 --connect-timeout 20 --max-time 180 --output (Join-Path $source $asset.name) $asset.browser_download_url
    if ($LASTEXITCODE -ne 0) { throw "Could not download $($asset.name)." }
}
if (-not (Get-ChildItem $source -Filter '*windows-msvc.zip')) {
    throw 'Internal Windows build archive is missing. Supply the CI public-client-bundle artifact in .dev/debug/client-assets; public releases expose only Windows installers.'
}
# Preserve the separately packaged Chrome extension; native installers are checked
# against their SHA-256 sidecars and binary formats by the canonical manifest tool.
foreach ($file in Get-ChildItem "$root/deploy/client-assets/release" -File) {
    if ($file.Name -eq 'extension.json' -or $file.Name -like 'olo-toolgate-chrome-*') {
        Copy-Item -LiteralPath $file.FullName -Destination $source -Force
    }
}
# Repackage Windows installers from verified native inputs with the current
# installer and Chrome sources, so debug exercises local packaging changes too.
$compiler = Join-Path $root '.dev/debug/toolgate-inno-6.7.1/ISCC.exe'
if (-not (Test-Path -LiteralPath $compiler)) {
    $previousTemp = $env:RUNNER_TEMP
    try {
        $env:RUNNER_TEMP = Join-Path $root '.dev/debug'
        & "$root/tools/client/installer-compiler.ps1"
    } finally { $env:RUNNER_TEMP = $previousTemp }
}
$previousCompiler = $env:CLIENT_ISCC_PATH
try {
    $env:CLIENT_ISCC_PATH = $compiler
    foreach ($target in @('x86_64-pc-windows-msvc', 'aarch64-pc-windows-msvc')) {
        & $Python "$root/tools/client/installer.py" --target $target --output $source
        if ($LASTEXITCODE -ne 0) { throw "Windows installer build failed for $target." }
    }
    & $Python "$root/tools/client/extension.py" --output $source --build 0
    if ($LASTEXITCODE -ne 0) { throw 'Chrome extension metadata build failed.' }
} finally { $env:CLIENT_ISCC_PATH = $previousCompiler }
& $Python "$root/tools/client/manifest.py" --source $source --output "$root/deploy/client-assets/release"
if ($LASTEXITCODE -ne 0) { throw 'Client release bundle verification failed.' }
Write-Host "Verified native installers from $($release.tag_name)"
