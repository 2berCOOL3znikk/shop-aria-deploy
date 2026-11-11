@echo off
setlocal
cd /d "%~dp0"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\desktop\start.ps1" %*
set "ARIA_EXIT_CODE=%ERRORLEVEL%"
if not "%ARIA_EXIT_CODE%"=="0" pause
exit /b %ARIA_EXIT_CODE%
