@echo off
title inBox
echo Iniciando o inBox...
powershell.exe -NoLogo -NoProfile -NonInteractive -ExecutionPolicy Bypass -File "%~dp0backend\scripts\inbox.ps1" > "%~dp0backend\launcher.log" 2>&1
if errorlevel 1 (
    echo Nao foi possivel abrir o inBox:
    type "%~dp0backend\launcher.log"
    pause
    exit /b 1
)
