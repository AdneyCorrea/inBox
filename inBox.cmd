@echo off
rem Prepara as ferramentas locais e aciona o inicializador do inBox no Windows.
setlocal
title inBox
set "INBOX_CMD=%~f0"
set "INBOX_ROOT=%~dp0"
set "INBOX_MODE=%~1"
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$t=[IO.File]::ReadAllText($env:INBOX_CMD); & ([ScriptBlock]::Create(($t -split '(?m)^# POWERSHELL\r?$',2)[1]))"
if errorlevel 1 (
 echo Falha ao iniciar. Consulte backend\launcher.log.
 if /I not "%INBOX_MODE%"=="--verificar" pause
 exit /b 1
)
exit /b 0
# POWERSHELL
$ErrorActionPreference = 'Stop'
# Carrega tambem as funcoes do modulo antes das chamadas em escopos internos.
Import-Module Microsoft.PowerShell.Utility -ErrorAction Stop
$root = [IO.Path]::GetFullPath($env:INBOX_ROOT).TrimEnd('\')
$tools = Join-Path $root 'backend\scripts\tools'
function Test-Jdk([string]$homePath) {
    if (!$homePath) { return $false }
    $java = Join-Path $homePath 'bin\java.exe'
    $javac = Join-Path $homePath 'bin\javac.exe'
    if (!(Test-Path -LiteralPath $java) -or !(Test-Path -LiteralPath $javac)) { return $false }
    try {
        # Java pode escrever a versao em stderr; isso nao representa uma falha.
        foreach ($exe in @($java, $javac)) {
            $info = New-Object Diagnostics.ProcessStartInfo
            $info.FileName = $exe
            $info.Arguments = '-version'
            $info.UseShellExecute = $false
            $info.CreateNoWindow = $true
            $info.RedirectStandardOutput = $true
            $info.RedirectStandardError = $true
            $process = [Diagnostics.Process]::Start($info)
            try {
                $output = $process.StandardOutput.ReadToEndAsync()
                $errors = $process.StandardError.ReadToEndAsync()
                if (!$process.WaitForExit(15000)) { $process.Kill(); return $false }
                $v = $output.Result + $errors.Result
                if ($process.ExitCode -ne 0 -or $v -notmatch '(?:javac\s+|version\s+")(\d+)' -or [int]$Matches[1] -lt 17) { return $false }
            } finally { $process.Dispose() }
        }
        return $true
    } catch { return $false }
}
function Install-Package($url, $hash, $algorithm, $destination) {
    # Prepara no mesmo volume do destino para concluir com uma renomeacao atomica.
    $destination = [IO.Path]::GetFullPath($destination)
    $tempRoot = [IO.Path]::GetFullPath((Split-Path -Parent $destination)).TrimEnd('\')
    $tempDir = [IO.Path]::GetFullPath((Join-Path $tempRoot ('inbox-' + [guid]::NewGuid().ToString('N'))))
    if (!$tempDir.StartsWith($tempRoot + '\', [StringComparison]::OrdinalIgnoreCase)) { throw 'Pasta temporaria invalida.' }
    [IO.Directory]::CreateDirectory($tempDir) | Out-Null
    try {
        $zip = Join-Path $tempDir 'package.zip'
        Invoke-WebRequest $url -OutFile $zip -UseBasicParsing -TimeoutSec 600
        if ((Get-FileHash -LiteralPath $zip -Algorithm $algorithm).Hash -ne $hash) { throw 'Download com integridade invalida.' }
        $expanded = Join-Path $tempDir 'expanded'
        Add-Type -AssemblyName System.IO.Compression.FileSystem
        [IO.Compression.ZipFile]::ExtractToDirectory($zip, $expanded)
        $dirs = @(Get-ChildItem -LiteralPath $expanded -Directory)
        if ($dirs.Count -ne 1) { throw 'Estrutura do pacote inesperada.' }
        if (Test-Path -LiteralPath $destination) { [IO.Directory]::Move($destination, ($destination + '-backup-' + [guid]::NewGuid().ToString('N'))) }
        [IO.Directory]::Move($dirs[0].FullName, $destination)
    } finally { Remove-Item -LiteralPath $tempDir -Recurse -Force -ErrorAction SilentlyContinue }
}
try {
    if (!(Test-Path -LiteralPath (Join-Path $root 'backend\scripts\inbox.ps1'))) { throw 'Coloque inBox.cmd ao lado da pasta backend.' }
    Start-Transcript -LiteralPath (Join-Path $root 'backend\launcher.log') -Force | Out-Null
    New-Item -ItemType Directory $tools -Force | Out-Null
    [Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
    Write-Host 'Verificando Java JDK 17 ou superior...'
    $candidates = @((Join-Path $tools 'java'), $env:JAVA_HOME)
    $command = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($command) { $candidates += Split-Path -Parent (Split-Path -Parent $command.Source) }
    foreach ($vendor in @('Java','Eclipse Adoptium','Microsoft','Amazon Corretto')) {
        $base = Join-Path $env:ProgramFiles $vendor
        if (Test-Path -LiteralPath $base) { $candidates += Get-ChildItem -LiteralPath $base -Directory | ForEach-Object { $_.FullName } }
    }
    $jdk = $null
    foreach ($candidate in $candidates) { if (Test-Jdk $candidate) { $jdk = $candidate; break } }
    if (!$jdk) {
        Write-Host 'Baixando e preparando Java 17. Aguarde...'
        if (![Environment]::Is64BitOperatingSystem) { throw 'Requer Windows de 64 bits.' }
        $arch = if ($env:PROCESSOR_ARCHITECTURE -eq 'ARM64' -or $env:PROCESSOR_ARCHITEW6432 -eq 'ARM64') { 'aarch64' } else { 'x64' }
        $assets = @(Invoke-RestMethod "https://api.adoptium.net/v3/assets/latest/17/hotspot?architecture=$arch&image_type=jdk&os=windows&vendor=eclipse" -TimeoutSec 60)
        $package = $assets[0].binary.package
        if (!$package.link -or $package.checksum -notmatch '^[a-fA-F0-9]{64}$') { throw 'Pacote oficial Java indisponivel.' }
        $jdk = Join-Path $tools 'java'
        Install-Package $package.link $package.checksum 'SHA256' $jdk
        if (!(Test-Jdk $jdk)) { throw 'Falha ao validar Java.' }
    }
    $env:JAVA_HOME = $jdk
    $env:PATH = (Join-Path $jdk 'bin') + ';' + $env:PATH
    Write-Host "Java pronto: $jdk"
    Write-Host 'Verificando Maven...'
    . (Join-Path $root 'backend\scripts\runtime.ps1')
    $maven = $null
    try {
        $maven = Get-InboxMavenHome
        Invoke-InboxMaven -MavenRoot $maven -Arguments @('-version')
    } catch {
        Write-Host ('Maven ausente ou invalido: ' + $_.Exception.Message)
        Write-Host 'Preparando uma copia local...'
        $maven = $null
    }
    if (!$maven) {
        Write-Host 'Baixando e preparando Maven 3.9.11. Aguarde...'
        $url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'
        $checksumFile = Join-Path ([IO.Path]::GetTempPath()) ('inbox-' + [guid]::NewGuid().ToString('N') + '.sha512')
        try {
            Invoke-WebRequest "$url.sha512" -OutFile $checksumFile -UseBasicParsing -TimeoutSec 60
            $hash = [IO.File]::ReadAllText($checksumFile).Trim()
        } finally { Remove-Item -LiteralPath $checksumFile -Force -ErrorAction SilentlyContinue }
        if ($hash -notmatch '^[a-fA-F0-9]{128}$') { throw 'Checksum oficial Maven invalido.' }
        $maven = Join-Path $tools 'apache-maven-3.9.11'
        Install-Package $url $hash 'SHA512' $maven
        Invoke-InboxMaven -MavenRoot $maven -Arguments @('-version')
    }
    $env:INBOX_MAVEN_HOME = $maven
    Write-Host "Maven pronto: $maven"
    if ($env:INBOX_MODE -eq '--verificar') {
        Write-Host 'Java e Maven prontos para uso.'
        exit 0
    }
    Write-Host 'Iniciando o inBox...'
    & (Join-Path $root 'backend\scripts\inbox.ps1')
    if (!$?) { throw 'Falha ao iniciar o inBox.' }
} catch {
    Write-Host ('Erro: ' + $_.Exception.Message) -ForegroundColor Red
    exit 1
} finally {
    try { Stop-Transcript | Out-Null } catch {}
}
