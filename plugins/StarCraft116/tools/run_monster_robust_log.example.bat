@echo off
setlocal EnableExtensions

REM 20260705_kpopmodder: Example Monster launcher template for standalone BWAPI client reuse.
REM Copy this file beside Monster.exe and rename it to run_monster_robust_log.bat.
REM It keeps Monster.exe alive across games by restarting after disconnects.

set "MONSTER_DIR=%~dp0"
if defined LAV_STARCRAFT116_MONSTER_DIR set "MONSTER_DIR=%LAV_STARCRAFT116_MONSTER_DIR%"
if "%MONSTER_DIR:~-1%"=="\" set "MONSTER_DIR=%MONSTER_DIR:~0,-1%"

REM #20260718_kpopmodder: Prefer project-local BWAPI_APP so Monster events stay inside LAVI.
set "STAR_DIR="
if defined LAV_STARCRAFT116_STARCRAFT_DIR set "STAR_DIR=%LAV_STARCRAFT116_STARCRAFT_DIR%"
if not defined STAR_DIR if exist "%MONSTER_DIR%\..\..\BWAPI_APP\BWAPI_420\Starcraft" set "STAR_DIR=%MONSTER_DIR%\..\..\BWAPI_APP\BWAPI_420\Starcraft"
if not defined STAR_DIR set "STAR_DIR=%MONSTER_DIR%\..\StarCraft"
set "BWAPI_DATA_DIR=%STAR_DIR%\bwapi-data"
if defined LAV_STARCRAFT116_BWAPI_DATA_DIR set "BWAPI_DATA_DIR=%LAV_STARCRAFT116_BWAPI_DATA_DIR%"
set "BWAPI_PROXY_EVENTS_PATH=%BWAPI_DATA_DIR%\bwapi_proxy_events.jsonl"
if defined LAV_STARCRAFT116_BWAPI_PROXY_EVENTS_PATH set "BWAPI_PROXY_EVENTS_PATH=%LAV_STARCRAFT116_BWAPI_PROXY_EVENTS_PATH%"
set "LOG_FILE=%MONSTER_DIR%\monster_log.txt"
set "MONSTER_RESTART_DELAY_SEC=15"
set "MONSTER_RUN_ONCE=%~1"

pushd "%MONSTER_DIR%" || (
    echo Failed to enter Monster folder: "%MONSTER_DIR%"
    pause
    exit /b 1
)

echo.
echo ===============================================
echo Monster launcher with robust log and restart loop
echo MonsterDir: %MONSTER_DIR%
echo StarDir:    %STAR_DIR%
echo BwapiData:  %BWAPI_DATA_DIR%
echo BwapiEvents:%BWAPI_PROXY_EVENTS_PATH%
echo LogFile:    %LOG_FILE%
echo ===============================================
echo.

>> "%LOG_FILE%" echo ===============================
>> "%LOG_FILE%" echo Start: %date% %time%
>> "%LOG_FILE%" echo MonsterDir: %MONSTER_DIR%
>> "%LOG_FILE%" echo WorkingDir: %CD%
>> "%LOG_FILE%" echo StarDir: %STAR_DIR%
>> "%LOG_FILE%" echo BwapiDataDir: %BWAPI_DATA_DIR%
>> "%LOG_FILE%" echo BwapiEvents: %BWAPI_PROXY_EVENTS_PATH%
>> "%LOG_FILE%" echo ===============================

if not exist "%MONSTER_DIR%\Monster.exe" (
    echo ERROR: Monster.exe not found in "%MONSTER_DIR%"
    >> "%LOG_FILE%" echo ERROR: Monster.exe not found.
    popd
    pause
    exit /b 1
)

if not exist "%MONSTER_DIR%\sc.dat" (
    echo ERROR: sc.dat not found beside Monster.exe.
    echo Put this bat in the folder that contains Monster.exe and sc.dat.
    >> "%LOG_FILE%" echo ERROR: sc.dat not found beside Monster.exe.
    popd
    pause
    exit /b 1
)

REM Create likely BWAPI data folders. Some bots change/search paths after BWAPI connects.
if exist "%STAR_DIR%" if not exist "%BWAPI_DATA_DIR%" mkdir "%BWAPI_DATA_DIR%" >nul 2>nul
if exist "%BWAPI_DATA_DIR%" (
    if not exist "%BWAPI_DATA_DIR%\AI" mkdir "%BWAPI_DATA_DIR%\AI" >nul 2>nul
    if not exist "%BWAPI_DATA_DIR%\read" mkdir "%BWAPI_DATA_DIR%\read" >nul 2>nul

    echo Copying Monster data files to StarCraft fallback folders...
    >> "%LOG_FILE%" echo Copying Monster data files to fallback folders...

    for %%F in (sc.dat fp.dat wt_*.dat) do (
        if exist "%MONSTER_DIR%\%%F" (
            if exist "%STAR_DIR%" copy /Y "%MONSTER_DIR%\%%F" "%STAR_DIR%\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%BWAPI_DATA_DIR%\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%BWAPI_DATA_DIR%\AI\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%BWAPI_DATA_DIR%\read\" >nul 2>nul
        )
    )

    >> "%LOG_FILE%" echo --- sc.dat locations after copy ---
    if exist "%MONSTER_DIR%\sc.dat" >> "%LOG_FILE%" echo OK MonsterDir sc.dat
    if exist "%STAR_DIR%\sc.dat" >> "%LOG_FILE%" echo OK StarDir sc.dat
    if exist "%BWAPI_DATA_DIR%\sc.dat" >> "%LOG_FILE%" echo OK BWAPI_DATA_DIR sc.dat
    if exist "%BWAPI_DATA_DIR%\AI\sc.dat" >> "%LOG_FILE%" echo OK BWAPI_DATA_DIR\AI sc.dat
    if exist "%BWAPI_DATA_DIR%\read\sc.dat" >> "%LOG_FILE%" echo OK BWAPI_DATA_DIR\read sc.dat
) else (
    echo WARNING: BWAPI data folder was not found: "%BWAPI_DATA_DIR%"
    if not exist "%STAR_DIR%" echo WARNING: StarCraft folder was not found: "%STAR_DIR%"
    echo Only running from Monster folder.
    >> "%LOG_FILE%" echo WARNING: BWAPI data folder not found. Only running from Monster folder.
    if not exist "%STAR_DIR%" >> "%LOG_FILE%" echo WARNING: StarCraft folder not found: "%STAR_DIR%"
)

echo Starting Monster.exe...
echo Logs will be appended to:
echo %LOG_FILE%
echo.
echo After this, run Chaoslauncher as administrator with BWAPI 4.2.0 Client Connection enabled.
echo.

:monster_loop
echo Starting Monster.exe...
>> "%LOG_FILE%" echo Monster loop start: %date% %time%

Monster.exe >> "%LOG_FILE%" 2>&1
set "EXITCODE=%ERRORLEVEL%"

>> "%LOG_FILE%" echo ===============================
>> "%LOG_FILE%" echo End: %date% %time%
>> "%LOG_FILE%" echo ExitCode: %EXITCODE%
>> "%LOG_FILE%" echo ===============================
>> "%LOG_FILE%" echo.

echo.
echo Monster.exe ended or disconnected.
echo ExitCode: %EXITCODE%
echo Log saved to: %LOG_FILE%
echo.

if /I "%MONSTER_RUN_ONCE%"=="--once" goto monster_done

echo Monster will restart in %MONSTER_RESTART_DELAY_SEC% seconds and wait for the next BWAPI game.
echo Press Ctrl+C, then Y, to stop this Monster launcher.
>> "%LOG_FILE%" echo Restarting Monster in %MONSTER_RESTART_DELAY_SEC% seconds.
timeout /t %MONSTER_RESTART_DELAY_SEC% /nobreak >nul
goto monster_loop

:monster_done
popd
pause
exit /b %EXITCODE%
