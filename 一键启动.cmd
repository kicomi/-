@echo off
chcp 65001 >nul
title 校园二手书 - 一键启动
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1"
