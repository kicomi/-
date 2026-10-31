@echo off
chcp 65001 >nul
title 校园二手书 - 停止服务
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\stop-all.ps1"
pause
