@echo off
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-preview.ps1"
if errorlevel 1 (echo Preview could not start. Read the message above.)
pause
