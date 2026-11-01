@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title FTP Client (FileZilla Style) - Lab 2

echo ========================================================
echo   FTP Client (FileZilla Style) - Lab 2
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pclient

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong FTP Client. Vui long kiem tra JDK 21.
    pause
)
