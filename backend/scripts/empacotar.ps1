# Compila e empacota uma distribuição Windows com executável e Java incluído.
param([switch]$Installer)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
. (Join-Path $PSScriptRoot 'runtime.ps1')
$java = Get-InboxJava
$jdk = Split-Path -Parent (Split-Path -Parent $java)
foreach ($tool in @('javac.exe','jar.exe','jpackage.exe')) {
    if (!(Test-Path -LiteralPath (Join-Path $jdk "bin/$tool"))) { throw "JDK completo necessário: $tool ausente." }
}
$release = [IO.File]::ReadAllText((Join-Path $jdk 'release'))
if ($release -notmatch 'JAVA_VERSION="(\d+)' -or [int]$Matches[1] -lt 21) { throw 'Use JDK 21 ou superior para gerar a distribuição desktop.' }
# Java 21+ permite encerrar o cliente HTTP usado na verificação de disponibilidade.
Invoke-InboxMaven -Arguments @('-f', (Join-Path $root 'backend/pom.xml'), 'package')
$build = Join-Path $root ('backend/target/desktop-' + [guid]::NewGuid().ToString('N'))
$inputDir = Join-Path $build 'input'
$classes = Join-Path $build 'classes'
[IO.Directory]::CreateDirectory($inputDir) | Out-Null
[IO.Directory]::CreateDirectory($classes) | Out-Null
& (Join-Path $jdk 'bin/javac.exe') --release 21 -encoding UTF-8 -d $classes (Join-Path $root 'backend/desktop/DesktopLauncher.java')
if ($LASTEXITCODE) { throw 'Falha ao compilar o inicializador desktop. Use JDK 21 ou superior.' }
& (Join-Path $jdk 'bin/jar.exe') --create --file (Join-Path $inputDir 'launcher.jar') --main-class DesktopLauncher -C $classes .
if ($LASTEXITCODE) { throw 'Falha ao criar launcher.jar.' }
Copy-Item -LiteralPath (Join-Path $root 'backend/target/inbox-0.1.0-exec.jar') -Destination (Join-Path $inputDir 'inbox.jar')
$dest = Join-Path $build 'distribution'
& (Join-Path $jdk 'bin/jpackage.exe') --type app-image --name inBox --app-version 0.1.0 --vendor inBox --input $inputDir --main-jar launcher.jar --main-class DesktopLauncher --dest $dest --add-modules java.se,jdk.unsupported,jdk.crypto.ec --jlink-options "--strip-debug --no-header-files --no-man-pages"
if ($LASTEXITCODE) { throw 'Falha ao empacotar o aplicativo.' }
$zip = Join-Path $root 'backend/target/inBox-Windows.zip'
Add-Type -AssemblyName System.IO.Compression.FileSystem
$tempZip = Join-Path $build 'inBox-Windows.zip'
[IO.Compression.ZipFile]::CreateFromDirectory($dest, $tempZip)
Copy-Item -LiteralPath $tempZip -Destination $zip -Force
[IO.File]::WriteAllText((Join-Path $root 'backend/target/desktop-path.txt'), (Join-Path $dest 'inBox/inBox.exe'))
Write-Host "Aplicativo pronto: $dest\inBox\inBox.exe"
Write-Host "Distribuição: $zip"
if ($Installer) {
    & (Join-Path $jdk 'bin/jpackage.exe') --type exe --name inBox --app-version 0.1.0 --app-image (Join-Path $dest 'inBox') --dest $dest --win-per-user-install --win-menu --win-shortcut --win-dir-chooser
    if ($LASTEXITCODE) { throw 'Falha ao gerar instalador. Verifique se o WiX compatível com seu JDK está instalado.' }
}
