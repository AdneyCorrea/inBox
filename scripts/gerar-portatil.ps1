param(
    [string]$Jdk = $env:JAVA_HOME,
    [string]$Maven = 'mvn'
)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $raiz
if (-not $Jdk -or -not (Test-Path -LiteralPath "$Jdk\bin\jlink.exe")) {
    throw 'Informe -Jdk com o caminho de um JDK 17 ou superior que contenha jlink.'
}
$env:JAVA_HOME = $Jdk
$env:PATH = "$Jdk\bin;$env:PATH"
& $Maven package
if ($LASTEXITCODE -ne 0) { throw 'Falha no build ou nos testes Maven.' }
# Pasta nova para nao sobrescrever distribuicoes anteriores e seus dados.
$saida = Join-Path $raiz ('target\portatil-' + [guid]::NewGuid().ToString('N'))
$pacote = Join-Path $saida 'inBox'
[IO.Directory]::CreateDirectory("$pacote\app") | Out-Null
$jar = @(Get-ChildItem -LiteralPath "$raiz\target" -Filter '*.jar')
if ($jar.Count -ne 1) { throw 'Esperado exatamente um JAR executavel em target.' }
Copy-Item -LiteralPath $jar[0].FullName -Destination "$pacote\app\inbox.jar"
& "$Jdk\bin\jlink.exe" --module-path "$Jdk\jmods" --add-modules java.se,jdk.unsupported,jdk.crypto.ec --strip-debug --no-header-files --no-man-pages --output "$pacote\runtime"
if ($LASTEXITCODE -ne 0) { throw 'Falha ao gerar o Java embarcado.' }
Copy-Item -LiteralPath "$raiz\distribuicao\Iniciar inBox.cmd" -Destination $pacote
Copy-Item -LiteralPath "$raiz\distribuicao\LEIA-ME.txt" -Destination $pacote
$zip = Join-Path $saida 'inBox-Windows.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
[IO.Compression.ZipFile]::CreateFromDirectory($pacote, $zip, [IO.Compression.CompressionLevel]::Optimal, $true)
Write-Host "Pacote pronto: $zip"
