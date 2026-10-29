
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
. (Join-Path $PSScriptRoot 'runtime.ps1')
$javaExe = Get-InboxJava
$sourceFingerprint = Get-InboxSourceFingerprint $projectRoot
$mavenRoot = Join-Path $projectRoot 'backend\scripts\tools\apache-maven-3.9.11'
$launcher = Join-Path $mavenRoot 'boot\plexus-classworlds-2.9.0.jar'
if (-not (Test-Path -LiteralPath $launcher)) { throw 'Maven local nao encontrado em backend/scripts/tools/apache-maven-3.9.11.' }

# Compila separado do JAR aberto pelo servidor. A troca ocorre apenas no atualizador.
$staging = Join-Path $projectRoot 'backend\target\staging'
# Remove classes antigas apenas na area de compilacao; nunca no banco ou JAR em uso.
$targetRoot = [IO.Path]::GetFullPath((Join-Path $projectRoot 'backend\target'))
$resolvedStaging = [IO.Path]::GetFullPath($staging)
if (-not $resolvedStaging.StartsWith($targetRoot + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) {
    throw 'Diretorio de compilacao fora de backend/target.'
}
if (Test-Path -LiteralPath $staging) {
    if ((Get-Item -LiteralPath $staging).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Staging nao pode ser um link.' }
    foreach ($name in @('classes', 'test-classes')) {
        $generated = Join-Path $staging $name
        if (Test-Path -LiteralPath $generated) { Remove-Item -LiteralPath $generated -Recurse -Force }
    }
}
$arguments = @(
    "-Dmaven.multiModuleProjectDirectory=$projectRoot\backend",
    "-Dmaven.home=$mavenRoot",
    '-classpath', $launcher,
    "-Dclassworlds.conf=$mavenRoot\bin\m2.conf",
    'org.codehaus.plexus.classworlds.launcher.Launcher',
    '-f', (Join-Path $projectRoot 'backend\pom.xml'),
    "-Dinbox.build.directory=$staging", 'package', '-Dmaven.test.skip=true'
)


& $javaExe @arguments
if ($LASTEXITCODE -ne 0) { throw 'A compilacao falhou. A versao em execucao foi preservada.' }
if ((Get-InboxSourceFingerprint $projectRoot) -ne $sourceFingerprint) { throw 'O codigo mudou durante a compilacao. Abra o inBox novamente.' }
[IO.File]::WriteAllText((Join-Path $staging 'source.sha256'), $sourceFingerprint, (New-Object Text.UTF8Encoding($false)))

# Guarda somente o executavel pronto e seu hash; os intermediarios sao recriados.
foreach ($item in (Get-ChildItem -LiteralPath $staging -Force)) {
    if ($item.Name -in @('inbox-0.1.0-exec.jar','source.sha256')) { continue }
    $generatedPath = [IO.Path]::GetFullPath($item.FullName)
    if (-not $generatedPath.StartsWith($resolvedStaging + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase)) { throw 'Arquivo fora de staging.' }
    if ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'Intermediario nao pode ser um link.' }
    if ($item.PSIsContainer) { Remove-Item -LiteralPath $generatedPath -Recurse -Force }
    else { Remove-Item -LiteralPath $generatedPath -Force }
}
