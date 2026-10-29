# Executa os testes Java e JavaScript em área separada, preservando os dados locais.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$maven = Join-Path $PSScriptRoot 'tools/apache-maven-3.9.11/bin/mvn.cmd'

# Os testes usam outro diretório e um banco em memória. Não precisamos parar
# o aplicativo, substituir seu executável ou abrir os cadastros de verdade.
& $maven -f (Join-Path $projectRoot 'backend/pom.xml') '-Dinbox.build.directory=target/validation' test
if ($LASTEXITCODE -ne 0) { throw 'Os testes Java falharam. Consulte o relatório acima.' }

Push-Location -LiteralPath $projectRoot
try {
    # O Node interpreta colchetes no caminho absoluto como padrão de busca.
    # O caminho relativo também funciona quando a pasta se chama "[2] Projetos".
    & node --test backend/src/test/js/workflow.test.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Os testes da interface falharam. Consulte o relatório acima.' }
} finally { Pop-Location }
Write-Output 'Validação concluída: regras de embalagem, acesso, gravação e interface.'
