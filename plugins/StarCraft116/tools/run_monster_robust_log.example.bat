@echo off
setlocal EnableExtensions

REM 20260705_kpopmodder: Example Monster launcher template for standalone BWAPI client reuse.
REM Copy this file beside Monster.exe and rename it to run_monster_robust_log.bat.
REM It keeps Monster.exe alive across games by restarting after disconnects.

set "MONSTER_DIR=%~dp0"
if "%MONSTER_DIR:~-1%"=="\" set "MONSTER_DIR=%MONSTER_DIR:~0,-1%"

REM Default StarCraft folder: ..\StarCraft relative to the Monster folder.
set "STAR_DIR=%MONSTER_DIR%\..\StarCraft"
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
echo LogFile:    %LOG_FILE%
echo ===============================================
echo.

>> "%LOG_FILE%" echo ===============================
>> "%LOG_FILE%" echo Start: %date% %time%
>> "%LOG_FILE%" echo MonsterDir: %MONSTER_DIR%
>> "%LOG_FILE%" echo WorkingDir: %CD%
>> "%LOG_FILE%" echo StarDir: %STAR_DIR%
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
if exist "%STAR_DIR%" (
    if not exist "%STAR_DIR%\bwapi-data" mkdir "%STAR_DIR%\bwapi-data" >nul 2>nul
    if not exist "%STAR_DIR%\bwapi-data\AI" mkdir "%STAR_DIR%\bwapi-data\AI" >nul 2>nul
    if not exist "%STAR_DIR%\bwapi-data\read" mkdir "%STAR_DIR%\bwapi-data\read" >nul 2>nul

    echo Copying Monster data files to StarCraft fallback folders...
    >> "%LOG_FILE%" echo Copying Monster data files to fallback folders...

    for %%F in (sc.dat fp.dat wt_*.dat) do (
        if exist "%MONSTER_DIR%\%%F" (
            copy /Y "%MONSTER_DIR%\%%F" "%STAR_DIR%\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%STAR_DIR%\bwapi-data\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%STAR_DIR%\bwapi-data\AI\" >nul 2>nul
            copy /Y "%MONSTER_DIR%\%%F" "%STAR_DIR%\bwapi-data\read\" >nul 2>nul
        )
    )

    >> "%LOG_FILE%" echo --- sc.dat locations after copy ---
    if exist "%MONSTER_DIR%\sc.dat" >> "%LOG_FILE%" echo OK MonsterDir sc.dat
    if exist "%STAR_DIR%\sc.dat" >> "%LOG_FILE%" echo OK StarDir sc.dat
    if exist "%STAR_DIR%\bwapi-data\sc.dat" >> "%LOG_FILE%" echo OK bwapi-data sc.dat
    if exist "%STAR_DIR%\bwapi-data\AI\sc.dat" >> "%LOG_FILE%" echo OK bwapi-data\AI sc.dat
    if exist "%STAR_DIR%\bwapi-data\read\sc.dat" >> "%LOG_FILE%" echo OK bwapi-data\read sc.dat
) else (
    echo WARNING: StarCraft folder was not found: "%STAR_DIR%"
    echo Only running from Monster folder.
    >> "%LOG_FILE%" echo WARNING: StarCraft folder not found. Only running from Monster folder.
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
