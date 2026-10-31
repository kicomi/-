# ============================================================
#  校园二手书 —— 一键启动
#  依次拉起 MySQL / Redis / 后端 / 算法 / 管理端 / 学生端
#  重复执行是安全的：已在运行的组件会被跳过
# ============================================================
$ErrorActionPreference = 'Continue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# ---------- 路径配置（换机器只改这一段） ----------
$Root      = Split-Path -Parent $PSScriptRoot
$JavaExe   = 'E:\dev\jdk-17\bin\java.exe'
$MysqlExe  = 'E:\dev\mysql\bin\mysqld.exe'
$MysqlIni  = 'E:\dev\mysql\my.ini'
$RedisExe  = 'E:\dev\redis\redis-server.exe'
$RedisConf = 'E:\dev\redis\redis.dev.conf'
$PythonExe = 'E:\dev\pyenv\Scripts\python.exe'
$LogDir    = Join-Path $Root 'logs'

New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Test-Port($port) {
    return [bool](Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue)
}

function Wait-Port($port, $seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        if (Test-Port $port) { return $true }
        Start-Sleep -Seconds 1
    }
    return $false
}

Write-Host ''
Write-Host '  校园二手书 · 一键启动' -ForegroundColor Green
Write-Host '  --------------------------------------------------' -ForegroundColor DarkGray

# ---------- 1. MySQL ----------
if (Test-Port 3306) {
    Write-Host '  [跳过] MySQL 已在运行' -ForegroundColor DarkGray
} else {
    Write-Host '  [启动] MySQL ...' -NoNewline
    Start-Process -FilePath $MysqlExe -ArgumentList ("--defaults-file=" + $MysqlIni) -WindowStyle Hidden
    if (Wait-Port 3306 40) { Write-Host ' OK' -ForegroundColor Green } else { Write-Host ' 失败' -ForegroundColor Red }
}

# ---------- 2. Redis ----------
if (Test-Port 6379) {
    Write-Host '  [跳过] Redis 已在运行' -ForegroundColor DarkGray
} else {
    Write-Host '  [启动] Redis ...' -NoNewline
    Start-Process -FilePath $RedisExe -ArgumentList $RedisConf -WindowStyle Hidden
    if (Wait-Port 6379 15) { Write-Host ' OK' -ForegroundColor Green } else { Write-Host ' 失败' -ForegroundColor Red }
}

# ---------- 3. 后端 ----------
if (Test-Port 8080) {
    Write-Host '  [跳过] 后端 已在运行' -ForegroundColor DarkGray
} else {
    $jarDir = Join-Path $Root 'backend\target'
    $jar = Get-ChildItem $jarDir -Filter '*.jar' -ErrorAction SilentlyContinue |
           Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
    if (-not $jar) {
        Write-Host '  [错误] 找不到后端 jar，请先在 backend 目录执行 mvn package' -ForegroundColor Red
    } else {
        Write-Host '  [启动] 后端 Spring Boot ...' -NoNewline
        $argLine = '-Dfile.encoding=UTF-8 -jar "' + $jar.FullName + '"'
        $p = @{
            FilePath               = $JavaExe
            ArgumentList           = $argLine
            WorkingDirectory       = (Join-Path $Root 'backend')
            RedirectStandardOutput = (Join-Path $LogDir 'backend.out.log')
            RedirectStandardError  = (Join-Path $LogDir 'backend.err.log')
            WindowStyle            = 'Hidden'
        }
        Start-Process @p
        if (Wait-Port 8080 60) { Write-Host ' OK' -ForegroundColor Green } else { Write-Host ' 失败（看 logs\backend.err.log）' -ForegroundColor Red }
    }
}

# ---------- 4. 算法服务 ----------
if (Test-Port 5001) {
    Write-Host '  [跳过] 算法服务 已在运行' -ForegroundColor DarkGray
} else {
    Write-Host '  [启动] 算法服务 Flask ...' -NoNewline
    $env:PYTHONIOENCODING = 'utf-8'
    $p = @{
        FilePath               = $PythonExe
        ArgumentList           = 'app.py'
        WorkingDirectory       = (Join-Path $Root 'algorithm')
        RedirectStandardOutput = (Join-Path $LogDir 'algo.out.log')
        RedirectStandardError  = (Join-Path $LogDir 'algo.err.log')
        WindowStyle            = 'Hidden'
    }
    Start-Process @p
    if (Wait-Port 5001 40) { Write-Host ' OK' -ForegroundColor Green } else { Write-Host ' 失败（看 logs\algo.err.log）' -ForegroundColor Red }
}

# ---------- 5. 前端 ----------
function Start-Frontend($name, $dir, $port) {
    if (Test-Port $port) {
        Write-Host ('  [跳过] ' + $name + ' 已在运行') -ForegroundColor DarkGray
        return
    }
    Write-Host ('  [启动] ' + $name + ' ...') -NoNewline
    $p = @{
        FilePath               = 'C:\Windows\System32\cmd.exe'
        ArgumentList           = '/c npm run dev'
        WorkingDirectory       = (Join-Path $Root $dir)
        RedirectStandardOutput = (Join-Path $LogDir ($port.ToString() + '.out.log'))
        RedirectStandardError  = (Join-Path $LogDir ($port.ToString() + '.err.log'))
        WindowStyle            = 'Hidden'
    }
    Start-Process @p
    if (Wait-Port $port 60) { Write-Host ' OK' -ForegroundColor Green } else { Write-Host ' 失败（看 logs\' + $port + '.err.log）' -ForegroundColor Red }
}

Start-Frontend '管理端 (Vite)' 'admin-web' 5173
Start-Frontend '学生端 H5 (Vite)' 'student-h5' 5174

# ---------- 汇总 ----------
Write-Host ''
Write-Host '  --------------------------------------------------' -ForegroundColor DarkGray
$ports = @(3306, 6379, 8080, 5001, 5173, 5174)
$allOk = $true
foreach ($pt in $ports) {
    if (Test-Port $pt) { Write-Host ("  端口 {0,-6} 运行中" -f $pt) -ForegroundColor Green }
    else { Write-Host ("  端口 {0,-6} 未启动" -f $pt) -ForegroundColor Red; $allOk = $false }
}

Write-Host ''
Write-Host '  ================ 访问地址 ================' -ForegroundColor Cyan
Write-Host '   管理后台   http://127.0.0.1:5173' -ForegroundColor White
Write-Host '              账号 admin     密码 admin123' -ForegroundColor DarkGray
Write-Host '   学生端 H5  http://127.0.0.1:5174' -ForegroundColor White
Write-Host '   后端接口   http://127.0.0.1:8080/api' -ForegroundColor DarkGray
Write-Host '   接口文档   http://127.0.0.1:8080/swagger-ui/index.html' -ForegroundColor DarkGray
Write-Host '   算法服务   http://127.0.0.1:5001' -ForegroundColor DarkGray
Write-Host ''
Write-Host '   微信小程序：开发者工具打开 miniprogram 目录' -ForegroundColor White
Write-Host '              详情 - 本地设置 - 勾选「不校验合法域名」' -ForegroundColor DarkGray
Write-Host '  ==========================================' -ForegroundColor Cyan

if ($allOk) {
    Write-Host ''
    $ans = Read-Host '  全部就绪，要打开管理后台吗？(Y/n)'
    if ($ans -ne 'n') { Start-Process 'http://127.0.0.1:5173' }
}

Write-Host ''
Write-Host '  服务在后台运行，关掉本窗口不影响' -ForegroundColor DarkGray
Read-Host '  按回车关闭本窗口'
