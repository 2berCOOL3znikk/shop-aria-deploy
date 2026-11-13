@echo off
cd /d "%~dp0"
node scripts\staging\open-preview.cjs
if errorlevel 1 pause
