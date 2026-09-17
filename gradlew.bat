@echo off
setlocal enabledelayedexpansion

set "GRADLE_VERSION=9.7.1"
set "EXPECTED_SHA256=acd53f1edaf02f1a8ff99879f8a34b302661a057d9b063ae9e35b552f804d20a"
set "DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip"

if "%GRADLE_USER_HOME%"=="" (
  set "GRADLE_HOME_BASE=%USERPROFILE%\.gradle\olo-toolgate-bootstrap"
) else (
  set "GRADLE_HOME_BASE=%GRADLE_USER_HOME%\olo-toolgate-bootstrap"
)

set "INSTALL_DIR=%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%"
set "ZIP_FILE=%GRADLE_HOME_BASE%\gradle-%GRADLE_VERSION%-bin.zip"

if exist "%INSTALL_DIR%\bin\gradle.bat" (
  call "%INSTALL_DIR%\bin\gradle.bat" %*
  exit /b %ERRORLEVEL%
)

if not exist "%GRADLE_HOME_BASE%" mkdir "%GRADLE_HOME_BASE%"

echo Bootstrapping Gradle %GRADLE_VERSION%...

powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$ErrorActionPreference='Stop';" ^
  "$zip='%ZIP_FILE%';" ^
  "if (!(Test-Path $zip)) { Invoke-WebRequest -UseBasicParsing -Uri '%DIST_URL%' -OutFile $zip };" ^
  "$hash=(Get-FileHash -Algorithm SHA256 $zip).Hash.ToLowerInvariant();" ^
  "if ($hash -ne '%EXPECTED_SHA256%') { Remove-Item -Force $zip; throw ('Gradle checksum mismatch: '+$hash) };" ^
  "$tmp=Join-Path '%GRADLE_HOME_BASE%' ('.extract-%GRADLE_VERSION%-'+[guid]::NewGuid());" ^
  "New-Item -ItemType Directory -Force -Path $tmp | Out-Null;" ^
  "Expand-Archive -Force -Path $zip -DestinationPath $tmp;" ^
  "if (Test-Path '%INSTALL_DIR%') { Remove-Item -Recurse -Force '%INSTALL_DIR%' };" ^
  "Move-Item -Path (Join-Path $tmp 'gradle-%GRADLE_VERSION%') -Destination '%INSTALL_DIR%';" ^
  "Remove-Item -Recurse -Force $tmp;"

if ERRORLEVEL 1 exit /b %ERRORLEVEL%

call "%INSTALL_DIR%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
