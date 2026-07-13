@echo off
title Credit Platform API Launcher
color 0B

echo.
echo  ==========================================
echo       CREDIT PLATFORM API LAUNCHER
echo  ==========================================
echo.

where java >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Java not found in PATH. Please install Java 21 or newer.
    pause
    exit /b 1
)

where mvn >nul 2>&1
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Maven not found in PATH. Please install Maven.
    pause
    exit /b 1
)

echo Starting Spring Boot backend on port 8081...
start "Credit Platform - BACKEND" cmd /k "cd /d %~dp0 && set DB_USERNAME=postgres&& set DB_PASSWORD=53649713&& mvn spring-boot:run"

echo.
echo Backend URL: http://localhost:8081
echo Frontend login: http://localhost:4200/login.html
echo.
echo Run the Angular frontend from projet_stage_front with: npm.cmd start
echo.
pause
