@echo off
rem #20260716_kpopmodder: Safe runtime launcher only. Installation lives in scripts\install_windows.ps1.
setlocal

cd /D "%~dp0" || exit /b 1

set "LAVI_ROOT=%CD%"
set "LAVI_PYTHON=%LAVI_ROOT%\venv\Scripts\python.exe"

set "PYTHONNOUSERSITE=1"
set "PYTHONPATH="
set "PYTHONHOME="

if not exist "%LAVI_PYTHON%" (
    echo [LAVI] ERROR: Repository venv Python was not found:
    echo [LAVI]        %LAVI_PYTHON%
    echo [LAVI] Run scripts\install_windows.ps1 first.
    exit /b 10
)

"%LAVI_PYTHON%" "%LAVI_ROOT%\scripts\preflight.py"
if errorlevel 1 (
    set "LAVI_EXIT=%ERRORLEVEL%"
    echo [LAVI] ERROR: preflight failed with exit code %LAVI_EXIT%.
    exit /b %LAVI_EXIT%
)

"%LAVI_PYTHON%" "%LAVI_ROOT%\main.py"
set "LAVI_EXIT=%ERRORLEVEL%"
if not "%LAVI_EXIT%"=="0" (
    echo [LAVI] ERROR: main.py exited with code %LAVI_EXIT%.
)
exit /b %LAVI_EXIT%
