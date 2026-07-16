@echo off
rem #20260716_kpopmodder: Delegate CUDA setup to the canonical project-local installer.
setlocal
cd /D "%~dp0"

powershell -NoProfile -ExecutionPolicy Bypass -File ".\scripts\install_windows.ps1" -Profile Full -Accelerator cu130 %*
set "LAVI_EXIT=%ERRORLEVEL%"

if not "%LAVI_EXIT%"=="0" (
    echo [LAVI] CUDA installer failed with exit code %LAVI_EXIT%.
    exit /b %LAVI_EXIT%
)

echo [LAVI] Full cu130 environment is ready.
exit /b 0
