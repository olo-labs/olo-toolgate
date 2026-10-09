@echo off
rem Copyright 2026 OLO Labs
rem SPDX-License-Identifier: Apache-2.0
powershell -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0configure-client-tools.ps1" %*
exit /b %errorlevel%
