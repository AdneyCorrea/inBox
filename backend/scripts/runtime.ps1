# Localiza o executavel real do Java, evitando inicializadores intermediarios do PATH.
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
