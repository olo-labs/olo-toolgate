@echo off
powershell -NoProfile -ExecutionPolicy RemoteSigned -File "%~dp0setup-agent-mimic.ps1"
pause
