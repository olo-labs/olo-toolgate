@echo off
rem Copyright 2026 OLO Labs
rem SPDX-License-Identifier: Apache-2.0
powershell.exe -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0start.ps1" %*
exit /b %errorlevel%
