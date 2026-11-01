@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title FTP Server Dashboard (RFC 959 + MySQL) - Lab 2

echo ========================================================
echo   FTP Server Dashboard (RFC 959 + MySQL) - Lab 2
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pserver

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong FTP Server. Vui long kiem tra JDK 21 va MySQL.
    pause
)
