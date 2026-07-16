@echo off
rem #20260716_kpopmodder: Delegate CPU setup to the canonical project-local installer.
setlocal
cd /D "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\install_windows.ps1" -Profile Core -Accelerator CPU %*
set "LAVI_EXIT=%ERRORLEVEL%"

if not "%LAVI_EXIT%"=="0" (
    echo [LAVI] CPU installer failed with exit code %LAVI_EXIT%.
    exit /b %LAVI_EXIT%
)

echo [LAVI] Core CPU environment is ready.
exit /b 0
