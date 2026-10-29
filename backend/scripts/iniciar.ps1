param([int]$Port = 8080, [switch]$SemAbrirNavegador)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$backendDir = Join-Path $projectRoot 'backend'
$jarPath = Join-Path $backendDir 'target\inbox-0.1.0-exec.jar'
$serverUrl = "http://127.0.0.1:$Port"
try {
    $health = Invoke-RestMethod "$serverUrl/api/health" -TimeoutSec 2
    if ($health.app -eq 'inbox') {
        if (-not $SemAbrirNavegador) { Start-Process $serverUrl }
        exit 0
    }
} catch { }
if (-not (Test-Path -LiteralPath $jarPath)) { throw 'O pacote do inBox nao foi preparado.' }
. (Join-Path $PSScriptRoot 'runtime.ps1')
$javaExe = Get-InboxJava
# O diretorio de trabalho e fixo para sempre abrir o mesmo banco local.
$serverProcess = Start-Process -FilePath $javaExe -ArgumentList @('-Dlogging.file.name=server.log', '-jar', ('"' + $jarPath + '"'), "--server.port=$Port") -WorkingDirectory ([System.Management.Automation.WildcardPattern]::Escape($backendDir)) -WindowStyle Hidden -PassThru
$ready = $false
for ($attempt=0; $attempt -lt 40; $attempt++) {
    $serverProcess.Refresh()
    if ($serverProcess.HasExited) { break }
    try { $health = Invoke-RestMethod "$serverUrl/api/health" -TimeoutSec 1; if ($health.app -eq 'inbox') { $ready=$true; break } } catch { }
    Start-Sleep -Milliseconds 500
}
if (-not $ready) { throw 'Nao foi possivel iniciar. Consulte backend/server.log.' }
$serverProcess.Id | Set-Content -LiteralPath (Join-Path $backendDir 'server.pid')
if (-not $SemAbrirNavegador) { Start-Process $serverUrl }
