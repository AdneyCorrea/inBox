# Arquivos internos

A entrada unica e inBox.cmd, na raiz. Os arquivos internos ficam agrupados em backend/.

| Local | Funcao |
| --- | --- |
| src/main/java/ | Regras, calculo de caixas e persistencia |
| src/main/resources/static/ | Interface, estilos e visualizacao 3D |
| data/ | Banco local com os cadastros; nao participa da compilacao |
| target/ | Pacote executado e ultima compilacao pronta para aplicar |
| scripts/ | Abertura, verificacao de atualizacoes e compilacao automatica |
| scripts/tools/ | Maven usado para compilar quando os fontes mudam |
| docs/ | Este mapa, licencas e informacoes de importacao/hospedagem |
| deploy/ | Configuracoes e dados privados de preparacao da hospedagem |

## Abertura

inBox.cmd executa scripts/inbox.ps1. Esse script compara o hash dos fontes com o pacote compilado. Se mudou, compila em uma area separada, encerra somente o servidor deste projeto e aplica o novo pacote. Entao inicia o servidor e abre o navegador. Sem alteracoes, reutiliza a versao pronta e o servidor existente.

scripts/runtime.ps1 localiza o Java e calcula o hash dos fontes. compilar.ps1 gera o pacote. iniciar.ps1 aguarda o servidor responder. parar.ps1 encerra somente os processos deste projeto. Falhas sao registradas em launcher.log e server.log.

A suíte antiga e os exemplos históricos foram removidos na reorganização. A suíte de regressão atual fica em src/test/ e pode ser executada com scripts/testar.ps1. Consulte docs/mapa-do-codigo.md para localizar regras, métodos e cenários de validação. Os campos antigos do modelo persistido continuam no codigo para manter compatibilidade com o banco e os backups.

## Interface

Em src/main/resources/static/, simple.html define a tela, simple.css a disposicao e theme.css a aparencia. simple.js conecta os modulos js/catalog.js (cadastros), js/packing.js (lista e volumes), js/http.js (comunicacao) e js/backup.js (exportacao/restauracao).

O backend usa PackingEngine para o encaixe geometrico, PcApi para regras de embalagem, DimensionsApi para cadastros e Store para gravacao com controle de revisao.
