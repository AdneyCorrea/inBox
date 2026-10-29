# Encerra somente os processos do servidor Java associados a este projeto.
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $projectRoot 'backend\server.pid'
$expectedJars = @(
    (Join-Path $projectRoot 'backend\target\inbox-0.1.0.jar'),
    (Join-Path $projectRoot 'backend\target\inbox-0.1.0-exec.jar')
)
# Encerra somente processos cujo comando aponta para um JAR deste projeto.
$serverProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object {
    $commandLine = $_.CommandLine
    $commandLine -and @($expectedJars | Where-Object {
        $commandLine.IndexOf(('"' + $_ + '"'), [StringComparison]::OrdinalIgnoreCase) -ge 0
    }).Count -gt 0
}
foreach ($serverInfo in $serverProcesses) {
    Stop-Process -Id $serverInfo.ProcessId
    Wait-Process -Id $serverInfo.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
}
if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile }
Write-Host 'inBox encerrado. Os dados continuam salvos.'
