param([int]$Port = 8080, [switch]$SemAbrirNavegador)
$ErrorActionPreference = 'Stop'
$taskRoot = $PSScriptRoot
$backendDir = Join-Path $taskRoot 'backend'
$jarPath = Join-Path $backendDir 'target\inbox-0.1.0.jar'
$serverUrl = "http://127.0.0.1:$Port"
try {
    $health = Invoke-RestMethod "$serverUrl/api/health" -TimeoutSec 2
    if ($health.app -eq 'inbox') {
        Write-Host "inBox já está aberto em $serverUrl"
        if (-not $SemAbrirNavegador) { Start-Process $serverUrl }
        exit 0
    }
} catch { }
if (-not (Test-Path -LiteralPath $jarPath)) {
    $mavenCmd = Join-Path $taskRoot 'tools\apache-maven-3.9.11\bin\mvn.cmd'
    if (-not (Test-Path -LiteralPath $mavenCmd)) {
        Write-Host 'Compile o projeto com Maven: mvn -f backend/pom.xml clean package'
        exit 1
    }
    & $mavenCmd -f (Join-Path $backendDir 'pom.xml') package
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$javaExe = (Get-Command java -ErrorAction Stop).Source
$taskErrorPreference = $ErrorActionPreference
try { $ErrorActionPreference = 'Continue'; $javaProperties = & $javaExe -XshowSettings:properties -version 2>&1 | Out-String }
finally { $ErrorActionPreference = $taskErrorPreference }
$javaHomeMatch = [regex]::Match($javaProperties, '(?m)^\s*java\.home\s*=\s*(.+)$')
if ($javaHomeMatch.Success) {
    $directJava = Join-Path $javaHomeMatch.Groups[1].Value.Trim() 'bin\java.exe'
    if (Test-Path -LiteralPath $directJava) { $javaExe = $directJava }
}
Push-Location -LiteralPath $backendDir
try {
$serverProcess = Start-Process -FilePath $javaExe -ArgumentList @('-Dlogging.file.name=server.log', '-jar', ('"' + $jarPath + '"'), "--server.port=$Port") -WorkingDirectory ([System.Management.Automation.WildcardPattern]::Escape($backendDir)) -WindowStyle Hidden -PassThru
} finally { Pop-Location }
$ready = $false
for ($attempt=0; $attempt -lt 40; $attempt++) {
    if ($serverProcess.HasExited) { break }
    try { $health = Invoke-RestMethod "$serverUrl/api/health" -TimeoutSec 1; if ($health.app -eq 'inbox') { $ready=$true; break } } catch { }
    Start-Sleep -Milliseconds 500
}
if (-not $ready) { Write-Host 'Não foi possível iniciar. Consulte backend/server.log.'; exit 1 }
$serverProcess.Id | Set-Content -LiteralPath (Join-Path $backendDir 'server.pid')
Write-Host "inBox disponível em $serverUrl"
if (-not $SemAbrirNavegador) { Start-Process $serverUrl }



