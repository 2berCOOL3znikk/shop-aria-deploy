@echo off
setlocal
cd /d "%~dp0"
call ARIA.cmd -NoBrowser
if errorlevel 1 exit /b %ERRORLEVEL%
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\desktop\woo-preview.ps1"
if errorlevel 1 pause
