# 停止本项目相关进程（MySQL / Redis 默认保留）
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Write-Host ''
Write-Host '  正在停止校园二手书相关进程 ...' -ForegroundColor Yellow

Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object { $_.CommandLine -match 'campus-book' } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host '   已停止 后端'
}
Get-CimInstance Win32_Process -Filter "Name='python.exe'" | Where-Object { $_.CommandLine -match 'app\.py' } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host '   已停止 算法服务'
}
Get-CimInstance Win32_Process -Filter "Name='node.exe'" | Where-Object { $_.CommandLine -match 'vite' } | ForEach-Object {
    Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    Write-Host '   已停止 前端 Vite'
}

Write-Host ''
Write-Host '  MySQL 与 Redis 保留运行；如需一并停止，在任务管理器结束 mysqld / redis-server' -ForegroundColor DarkGray
Write-Host '  完成' -ForegroundColor Green
Start-Sleep -Seconds 2
