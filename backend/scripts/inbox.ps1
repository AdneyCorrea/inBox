$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
. (Join-Path $PSScriptRoot 'runtime.ps1')

# Evita duas atualizacoes simultaneas em caso de cliques repetidos.
$sha = [Security.Cryptography.SHA256]::Create()
try { $identity = ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($projectRoot.ToLowerInvariant())))).Replace('-','') }
finally { $sha.Dispose() }
$mutex = New-Object Threading.Mutex($false, ('Local\inBox-' + $identity))
$locked = $false
try {
    try { $locked = $mutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $locked = $true }
    if (-not $locked) { exit 0 }
    $fingerprint = Get-InboxSourceFingerprint $projectRoot
    $staging = Join-Path $projectRoot 'backend\target\staging'
    $stagedJar = Join-Path $staging 'inbox-0.1.0-exec.jar'
    $stamp = Join-Path $staging 'source.sha256'
    $needsBuild = -not (Test-Path -LiteralPath $stagedJar) -or -not (Test-Path -LiteralPath $stamp)
    if (-not $needsBuild) { $needsBuild = [IO.File]::ReadAllText($stamp).Trim() -ne $fingerprint }
    if ($needsBuild) {
        Write-Output 'Atualizando o programa...'
        & (Join-Path $PSScriptRoot 'compilar.ps1')
        if (-not $?) { throw 'Nao foi possivel compilar a atualizacao.' }
    }
    $activeJar = Join-Path $projectRoot 'backend\target\inbox-0.1.0-exec.jar'
    $needsInstall = -not (Test-Path -LiteralPath $activeJar)
    if (-not $needsInstall) { $needsInstall = (Get-FileHash -LiteralPath $activeJar -Algorithm SHA256).Hash -ne (Get-FileHash -LiteralPath $stagedJar -Algorithm SHA256).Hash }
    if ($needsInstall) {
        # Atualiza somente o programa, sem importar ou substituir o banco.
        & (Join-Path $PSScriptRoot 'parar.ps1')
        if (-not $?) { throw 'Nao foi possivel encerrar a versao anterior.' }
        Copy-Item -LiteralPath $stagedJar -Destination $activeJar -Force
        # Pacotes antigos sao descartados somente depois de encerrar a versao anterior.
        foreach ($name in @('inbox-0.1.0.jar','inbox-0.1.0-ui.jar','inbox-refresh.jar','inbox-refresh-ui.jar')) {
            $obsolete = Join-Path $projectRoot ('backend\target\' + $name)
            if (Test-Path -LiteralPath $obsolete) { Remove-Item -LiteralPath $obsolete -Force }
        }
    }
    & (Join-Path $PSScriptRoot 'iniciar.ps1') -SemAbrirNavegador
    if (-not $?) { throw 'Nao foi possivel iniciar o servidor local.' }
    Start-Process ('http://127.0.0.1:8080/?v=' + $fingerprint.Substring(0,12))
} catch {
    Write-Error $_.Exception.Message -ErrorAction Continue
    exit 1
} finally {
    if ($locked) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
