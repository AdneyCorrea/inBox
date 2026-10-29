# Localiza ou prepara Java e Maven e fornece funções de apoio à compilação e inicialização.
# Localiza o executavel real do Java, evitando inicializadores intermediarios do PATH.
function Get-InboxMavenHome {
    $candidates = @($env:INBOX_MAVEN_HOME, $env:MAVEN_HOME, $env:M2_HOME,
        (Join-Path $PSScriptRoot 'tools\apache-maven-3.9.11'))
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($command) { $candidates += Split-Path -Parent (Split-Path -Parent $command.Source) }
    foreach ($candidate in $candidates) {
        if (-not $candidate) { continue }
        $boot = Join-Path $candidate 'boot'
        $lib = Join-Path $candidate 'lib'
        if (-not (Test-Path -LiteralPath $boot) -or -not (Test-Path -LiteralPath $lib)) { continue }
        $core = @(Get-ChildItem -LiteralPath $lib -Filter 'maven-core-*.jar' -File)
        $classworlds = @(Get-ChildItem -LiteralPath $boot -Filter 'plexus-classworlds-*.jar' -File)
        if ($core.Count -ne 1 -or $classworlds.Count -ne 1) { continue }
        if ($core[0].Name -notmatch '^maven-core-(3\.\d+\.\d+)\.jar$') { continue }
        if ([version]$Matches[1] -lt [version]'3.6.3') { continue }
        if (Test-Path -LiteralPath (Join-Path $candidate 'bin\m2.conf')) { return $candidate }
    }
    throw 'Maven 3.6.3 ou superior nao encontrado. Abra inBox.cmd para preparar automaticamente.'
}

# Evita a busca do mvn.cmd por pastas .mvn em diretorios ancestrais protegidos.
function Invoke-InboxMaven {
    param([string[]]$Arguments, [string]$MavenRoot = (Get-InboxMavenHome))
    $javaExe = Get-InboxJava
    $launcher = Get-ChildItem -LiteralPath (Join-Path $MavenRoot 'boot') -Filter 'plexus-classworlds-*.jar' -File | Select-Object -First 1
    $backend = Split-Path -Parent $PSScriptRoot
    $javaArguments = @(
        "-Dmaven.multiModuleProjectDirectory=$backend",
        "-Dmaven.home=$MavenRoot",
        '-classpath', $launcher.FullName,
        "-Dclassworlds.conf=$MavenRoot\bin\m2.conf",
        'org.codehaus.plexus.classworlds.launcher.Launcher'
    ) + $Arguments
    & $javaExe @javaArguments
    if ($LASTEXITCODE -ne 0) { throw "Maven falhou (codigo $LASTEXITCODE)." }
}

function Get-InboxJava {
    $javaCandidates = @()
    if ($env:JAVA_HOME) { $javaCandidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    foreach ($vendorRoot in @((Join-Path $env:ProgramFiles 'Java'), (Join-Path $env:ProgramFiles 'Eclipse Adoptium'))) {
        if (Test-Path -LiteralPath $vendorRoot) {
            $javaCandidates += Get-ChildItem -LiteralPath $vendorRoot -Directory | Sort-Object LastWriteTime -Descending | ForEach-Object { Join-Path $_.FullName 'bin\java.exe' }
        }
    }
    $javaCandidates += (Get-Command java -ErrorAction SilentlyContinue).Source
    $javaExe = $javaCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
    if (-not $javaExe) { throw 'Java nao encontrado. Instale Java 17 ou superior.' }
    
    return $javaExe
}

# Hash dos fontes: ignora banco, logs, backups e arquivos gerados.
function Get-InboxSourceFingerprint([string]$ProjectRoot) {
    $files = @(Get-ChildItem -LiteralPath (Join-Path $ProjectRoot 'backend\src\main') -Recurse -File)
    $files += Get-Item -LiteralPath (Join-Path $ProjectRoot 'backend\pom.xml')
    $manifest = New-Object Text.StringBuilder
    foreach ($file in ($files | Sort-Object FullName)) {
        $relative = $file.FullName.Substring($ProjectRoot.Length).Replace('\','/')
        [void]$manifest.AppendLine($relative + ':' + (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash)
    }
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($manifest.ToString())))).Replace('-','') }
    finally { $sha.Dispose() }
}
