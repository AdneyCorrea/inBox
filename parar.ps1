$ErrorActionPreference = 'Stop'
$pidFile = Join-Path $PSScriptRoot 'backend\server.pid'
$expectedJar = Join-Path $PSScriptRoot 'backend\target\inbox-0.1.0.jar'
$serverProcesses = Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object {
    $_.CommandLine -and $_.CommandLine.IndexOf(('"' + $expectedJar + '"'), [StringComparison]::OrdinalIgnoreCase) -ge 0
}
foreach ($serverInfo in $serverProcesses) {
    Stop-Process -Id $serverInfo.ProcessId
    Wait-Process -Id $serverInfo.ProcessId -Timeout 10 -ErrorAction SilentlyContinue
}
if (Test-Path -LiteralPath $pidFile) { Remove-Item -LiteralPath $pidFile }
Write-Host 'inBox encerrado. Os dados continuam salvos.'
