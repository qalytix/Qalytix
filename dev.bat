@echo off
setlocal enabledelayedexpansion

rem dev.bat — start / stop Qalytix in development mode (Windows)
rem
rem Usage:
rem   dev.bat start    — start postgres, backend, and frontend
rem   dev.bat stop     — stop all three
rem   dev.bat restart  — stop then start
rem   dev.bat status   — show what is running
rem   dev.bat logs     — tail backend + frontend logs
rem
rem Requirements: Docker Desktop, Java 21, Node.js, PowerShell 5+

set "SCRIPT_DIR=%~dp0"
if "%SCRIPT_DIR:~-1%"=="\" set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"

set "BACKEND_DIR=%SCRIPT_DIR%\qalytix-backend"
set "UI_DIR=%SCRIPT_DIR%\qalytix-ui"
set "LOGS_DIR=%SCRIPT_DIR%\.dev-logs"

set "BACKEND_PID_FILE=%LOGS_DIR%\backend.pid"
set "UI_PID_FILE=%LOGS_DIR%\ui.pid"
set "BACKEND_LOG=%LOGS_DIR%\backend.log"
set "UI_LOG=%LOGS_DIR%\ui.log"

if not exist "%LOGS_DIR%" mkdir "%LOGS_DIR%"

if /i "%~1"=="start"   goto :cmd_start
if /i "%~1"=="stop"    goto :cmd_stop
if /i "%~1"=="restart" goto :cmd_restart
if /i "%~1"=="status"  goto :cmd_status
if /i "%~1"=="logs"    goto :cmd_logs

echo Usage: %~nx0 {start^|stop^|restart^|status^|logs}
exit /b 1


rem ============================================================
rem  HELPERS
rem ============================================================

:is_running
rem Returns errorlevel 0 if pid file exists and process is alive, else 1
if not exist "%~1" exit /b 1
set /p "_IR_PID="<"%~1"
if "!_IR_PID!"=="" exit /b 1
tasklist /FI "PID eq !_IR_PID!" 2>nul | findstr /C:"!_IR_PID!" >nul 2>&1
exit /b !errorlevel!


:wait_for_port
rem :wait_for_port <port> <name>
set "_WP_PORT=%~1"
set "_WP_NAME=%~2"
set /a "_WP_I=0"
:_wfp_loop
powershell -NoProfile -Command ^
  "try{$t=New-Object Net.Sockets.TcpClient;$t.Connect('localhost',%_WP_PORT%);$t.Close();exit 0}catch{exit 1}" ^
  >nul 2>&1
if not errorlevel 1 exit /b 0
if !_WP_I! geq 60 (
  echo [qalytix] ERROR: %_WP_NAME% did not become ready on port %_WP_PORT% after 60 s.
  exit /b 1
)
timeout /t 1 /nobreak >nul
set /a "_WP_I+=1"
goto :_wfp_loop


rem ============================================================
rem  POSTGRES
rem ============================================================

:start_postgres
docker ps --format "{{.Names}}" 2>nul | findstr /C:"qalytix-postgres" >nul 2>&1
if not errorlevel 1 (
  echo [qalytix] Postgres already running.
  exit /b 0
)
echo [qalytix] Starting Postgres container...
set "JWT_SECRET=dev-placeholder"
docker-compose -f "%SCRIPT_DIR%\docker-compose.yml" up -d postgres
if errorlevel 1 ( echo [qalytix] ERROR: docker-compose failed. & exit /b 1 )

echo [qalytix] Waiting for Postgres to be healthy...
set /a "_PG_I=0"
:_pg_health_loop
for /f %%H in ('docker inspect --format "{{.State.Health.Status}}" qalytix-postgres 2^>nul') do set "_PG_H=%%H"
if /i "!_PG_H!"=="healthy" ( echo [qalytix] Postgres ready. & exit /b 0 )
if !_PG_I! geq 60 ( echo [qalytix] ERROR: Postgres did not become healthy. & exit /b 1 )
timeout /t 1 /nobreak >nul
set /a "_PG_I+=1"
goto :_pg_health_loop


:stop_postgres
docker ps --format "{{.Names}}" 2>nul | findstr /C:"qalytix-postgres" >nul 2>&1
if errorlevel 1 ( echo [qalytix] Postgres not running. & exit /b 0 )
echo [qalytix] Stopping Postgres container...
set "JWT_SECRET=dev-placeholder"
docker-compose -f "%SCRIPT_DIR%\docker-compose.yml" stop postgres
echo [qalytix] Postgres stopped.
exit /b 0


rem ============================================================
rem  BACKEND
rem ============================================================

:start_backend
call :is_running "%BACKEND_PID_FILE%"
if not errorlevel 1 (
  set /p "_PID="<"%BACKEND_PID_FILE%"
  echo [qalytix] Backend already running (pid !_PID!).
  exit /b 0
)
echo [qalytix] Starting backend (Spring Boot) on :8081...

rem Write a temp PS1 so paths with spaces are handled safely
set "_PS=%TEMP%\qalytix_backend_start.ps1"
set "QALYTIX_DIR=%BACKEND_DIR%"
set "QALYTIX_LOG=%BACKEND_LOG%"
(
  echo $dir = $env:QALYTIX_DIR
  echo $log = $env:QALYTIX_LOG
  echo $q   = [char]34
  echo $cmd = "cd /d " + $q + $dir + $q + " ^&^& mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=dev -q ^>^> " + $q + $log + $q + " 2^>^&1"
  echo ^(Start-Process cmd -ArgumentList '/c',$cmd -PassThru -WindowStyle Hidden^).Id
) > "!_PS!"
for /f %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -File "!_PS!"') do (
  echo %%P> "%BACKEND_PID_FILE%"
)
del "!_PS!" 2>nul

echo [qalytix] Waiting for backend to be ready...
call :wait_for_port 8081 Backend
if errorlevel 1 (
  echo [qalytix] ERROR: Backend failed to start. Check: %BACKEND_LOG%
  del "%BACKEND_PID_FILE%" 2>nul
  exit /b 1
)
echo [qalytix] Backend ready  --  http://localhost:8081
exit /b 0


:stop_backend
call :is_running "%BACKEND_PID_FILE%"
if errorlevel 1 ( echo [qalytix] Backend not running. & del "%BACKEND_PID_FILE%" 2>nul & exit /b 0 )
set /p "_PID="<"%BACKEND_PID_FILE%"
echo [qalytix] Stopping backend (pid !_PID!)...
taskkill /PID !_PID! /T /F >nul 2>&1
del "%BACKEND_PID_FILE%" 2>nul
echo [qalytix] Backend stopped.
exit /b 0


rem ============================================================
rem  FRONTEND
rem ============================================================

:start_ui
call :is_running "%UI_PID_FILE%"
if not errorlevel 1 (
  set /p "_PID="<"%UI_PID_FILE%"
  echo [qalytix] Frontend already running (pid !_PID!).
  exit /b 0
)
echo [qalytix] Starting frontend (Vite) on :3000...

set "_PS=%TEMP%\qalytix_ui_start.ps1"
set "QALYTIX_DIR=%UI_DIR%"
set "QALYTIX_LOG=%UI_LOG%"
(
  echo $dir = $env:QALYTIX_DIR
  echo $log = $env:QALYTIX_LOG
  echo $q   = [char]34
  echo $cmd = "cd /d " + $q + $dir + $q + " ^&^& npm run dev ^>^> " + $q + $log + $q + " 2^>^&1"
  echo ^(Start-Process cmd -ArgumentList '/c',$cmd -PassThru -WindowStyle Hidden^).Id
) > "!_PS!"
for /f %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -File "!_PS!"') do (
  echo %%P> "%UI_PID_FILE%"
)
del "!_PS!" 2>nul

echo [qalytix] Waiting for frontend to be ready...
call :wait_for_port 3000 Frontend
if errorlevel 1 (
  echo [qalytix] ERROR: Frontend failed to start. Check: %UI_LOG%
  del "%UI_PID_FILE%" 2>nul
  exit /b 1
)
echo [qalytix] Frontend ready  --  http://localhost:3000
exit /b 0


:stop_ui
call :is_running "%UI_PID_FILE%"
if errorlevel 1 ( echo [qalytix] Frontend not running. & del "%UI_PID_FILE%" 2>nul & exit /b 0 )
set /p "_PID="<"%UI_PID_FILE%"
echo [qalytix] Stopping frontend (pid !_PID!)...
taskkill /PID !_PID! /T /F >nul 2>&1
del "%UI_PID_FILE%" 2>nul
echo [qalytix] Frontend stopped.
exit /b 0


rem ============================================================
rem  COMMANDS
rem ============================================================

:cmd_start
call :start_postgres
if errorlevel 1 exit /b 1
call :start_backend
if errorlevel 1 exit /b 1
call :start_ui
if errorlevel 1 exit /b 1
echo.
echo [qalytix] All services running.
echo   Frontend  -^>  http://localhost:3000
echo   Backend   -^>  http://localhost:8081
echo   Swagger   -^>  http://localhost:8081/swagger-ui/index.html
echo   Logs dir  -^>  %LOGS_DIR%
exit /b 0


:cmd_stop
call :stop_ui
call :stop_backend
call :stop_postgres
echo [qalytix] All services stopped.
exit /b 0


:cmd_restart
call :cmd_stop
echo.
call :cmd_start
exit /b 0


:cmd_status
echo.
docker ps --format "{{.Names}}" 2>nul | findstr /C:"qalytix-postgres" >nul 2>&1
if not errorlevel 1 (
  echo   Postgres  [RUNNING]  (docker: qalytix-postgres)
) else (
  echo   Postgres  [STOPPED]
)

call :is_running "%BACKEND_PID_FILE%"
if not errorlevel 1 (
  set /p "_PID="<"%BACKEND_PID_FILE%"
  echo   Backend   [RUNNING]  (pid !_PID!)  :8081
) else (
  echo   Backend   [STOPPED]
)

call :is_running "%UI_PID_FILE%"
if not errorlevel 1 (
  set /p "_PID="<"%UI_PID_FILE%"
  echo   Frontend  [RUNNING]  (pid !_PID!)  :3000
) else (
  echo   Frontend  [STOPPED]
)
echo.
exit /b 0


:cmd_logs
if not exist "%BACKEND_LOG%" if not exist "%UI_LOG%" (
  echo [qalytix] No log files found -- run 'dev.bat start' first.
  exit /b 1
)
echo [qalytix] Tailing logs (Ctrl-C to exit)...
powershell -NoProfile -Command ^
  "& { $f=@(); if(Test-Path '%BACKEND_LOG%'){$f+='%BACKEND_LOG%'}; if(Test-Path '%UI_LOG%'){$f+='%UI_LOG%'}; Get-Content $f -Wait -Tail 40 }"
exit /b 0
