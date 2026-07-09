@echo off
:: ============================================================
::   Credit Platform - Full Stack Startup Script
::   Starts: Spring Boot Backend + JavaFX Desktop Frontend
:: ============================================================

title Credit Platform Launcher
color 0B

echo.
echo  ==========================================
echo       CREDIT PLATFORM  --  Launcher
echo  ==========================================
echo.

:: ── Pre-flight: Java ──────────────────────────────────────
where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found in PATH. Please install Java 21+.
    pause
    exit /b 1
)
echo [OK] Java found.

:: ── Pre-flight: Maven ─────────────────────────────────────
where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven (mvn) not found in PATH. Please install Maven.
    pause
    exit /b 1
)
echo [OK] Maven found.

echo.

:: ── Start Spring Boot Backend ─────────────────────────────
echo [1/2] Starting Spring Boot backend on port 8081...
start "Credit Platform - BACKEND" cmd /k "cd /d %~dp0 && mvn spring-boot:run"

:: ── Wait for backend to be ready (poll port 8081) ─────────
echo Waiting for backend to start (up to 90 seconds)...
set /a attempts=0

:WAIT_LOOP
set /a attempts+=1
if %attempts% gtr 30 (
    echo [ERROR] Backend did not start in time. Check the backend window.
    pause
    exit /b 1
)
timeout /t 3 /nobreak >nul
powershell -Command "if((Test-NetConnection localhost -Port 8081 -WarningAction SilentlyContinue).TcpTestSucceeded){exit 0}else{exit 1}" >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo    [%attempts%/30] Still waiting...
    goto WAIT_LOOP
)

echo [OK] Backend is UP!
echo.

:: ── Launch JavaFX Desktop App ─────────────────────────────
echo [2/2] Launching JavaFX desktop application...
start "Credit Platform - DESKTOP" cmd /k "cd /d %~dp0 && mvn exec:java -Dexec.mainClass=com.talan.creditplatform.view.DesktopLauncher"

echo.
echo  ==========================================
echo    All systems GO! Enjoy Credit Platform!
echo  ==========================================
echo.
echo   Backend  ->  http://localhost:8081
echo   Desktop  ->  JavaFX window opened
echo.
pause
