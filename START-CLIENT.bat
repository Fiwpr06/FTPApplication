@echo off
setlocal
cd /d "%~dp0"
chcp 65001 >nul
title FTP Application Client (FileZilla Style)

echo ========================================================
echo   FTP Application Client (FileZilla Style)
echo ========================================================
echo.

call .\mvnw.cmd javafx:run -Pclient

if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong the khoi dong FTP Client. Vui long kiem tra JDK 21.
    pause
)
