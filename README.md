<div align="center">

# 📦 inBox

**Encontre a caixa certa para os seus produtos.**

Planejamento de embalagem · Organização de volumes · Visualização 3D

[Como utilizar](#como-utilizar) · [Backups](#backups-e-dados) · [Documentação técnica](#documentação-técnica)

</div>

---

## Testar no Windows sem instalar ferramentas

**[Baixar inBox pronto para Windows x64](https://github.com/AdneyCorrea/inBox/releases/download/windows-preview/inBox-Windows.zip)**

Extraia o ZIP inteiro e dê dois cliques em **`inBox/inBox.exe`**. O programa abre o
navegador automaticamente e já inclui Java. Não precisa instalar Java, Maven ou Git.
Para encerrar, use **Encerrar inBox** no ícone ao lado do relógio do Windows.
Os cadastros ficam em `%LOCALAPPDATA%\inBox`, separados do programa.

A versão de teste é gerada automaticamente após os testes de cada atualização da
`main`. O download fica disponível quando a execução **Windows em um clique**
terminar com sucesso em [Actions](https://github.com/AdneyCorrea/inBox/actions/workflows/windows.yml).
O pacote é para Windows x64. Extraia o conteúdo antes de executar; mantenha as
pastas `app` e `runtime` ao lado do executável.

**Prefere clonar ou usar Code → Download ZIP?** Depois de baixar e extrair o
repositório, dê dois cliques em **`inBox.cmd`**, na raiz. Ele prepara automaticamente
as ferramentas ausentes, compila a cópia que você baixou e abre o navegador.
A primeira execução precisa de internet e pode levar alguns minutos; nas próximas,
reutiliza o que já foi preparado. Após um `git pull`, abra o mesmo arquivo para
aplicar as alterações. Esse modo usa os dados locais em `backend/data/`.

## O que é o inBox?

O **inBox** é uma aplicação web que ajuda na escolha de caixas para embalar produtos e configurações de PC. A partir dos itens, das quantidades e das medidas cadastradas, o programa verifica o encaixe e sugere caixas para os volumes da embalagem.

O objetivo é facilitar a preparação do envio: entender o que precisa ser embalado, separar os itens conforme suas regras e visualizar como eles podem ficar dentro da caixa.

| Recurso | O que você pode fazer |
| --- | --- |
| **Catálogo de produtos** | Cadastrar produtos com SKU, categoria e medidas da embalagem. |
| **Cadastro de caixas** | Informar as medidas internas das caixas disponíveis. |
| **Sugestão de embalagem** | Verificar o encaixe geométrico, com pelo menos 15% de espaço livre para proteção. |
| **Separação de volumes** | Tratar itens da configuração, compras avulsas e produtos que seguem na embalagem do fabricante. |
| **Visualização 3D** | Consultar a disposição sugerida dos itens em cada volume. |
| **Importação e backup** | Carregar listas de peças em JSON e exportar ou restaurar os dados do sistema. |

> A sugestão depende das medidas cadastradas. Confira fisicamente o encaixe, a proteção e o acondicionamento antes de enviar. Os exemplos do programa usam medidas fictícias.

## Como utilizar

### 1. Abra o programa

Para usar o inicializador local, você precisa de **Windows de 64 bits** e um navegador atualizado. O inicializador detecta Java JDK 17 ou superior e Maven 3 compatível já instalados; se faltarem, baixa e prepara cópias locais automaticamente, sem precisar de administrador.

1. Na raiz do projeto, clique duas vezes em **`inBox.cmd`**.
2. Aguarde a preparação do programa. Quando houver alterações nos arquivos locais, o inicializador compila e aplica a nova versão.
3. O navegador será aberto automaticamente em **[http://127.0.0.1:8080](http://127.0.0.1:8080)**.

Na primeira execução, mantenha acesso à internet para baixar as ferramentas ausentes e as dependências da compilação. As cópias locais ficam em `backend/scripts/tools/`, e os downloads são conferidos por checksum. Nas próximas execuções, as ferramentas são reutilizadas. Para conferir somente Java e Maven, execute `inBox.cmd --verificar`.

### 2. Cadastre os produtos

Na aba **Produtos**, escolha o departamento e a categoria e use a opção de cadastro. Informe o SKU, os dados do produto e o comprimento, a largura e a altura em **centímetros**.

Use as **medidas externas da embalagem do produto**. Para um PC preparado, considere o gabinete montado com sua proteção. A opção **OEM** identifica peças cujas medidas são desconsideradas no PC montado e consideradas na compra avulsa.

### 3. Cadastre as caixas disponíveis

Na aba **Caixas**, cadastre as caixas que você utiliza na operação. Informe as **medidas internas**, também em centímetros: esse é o espaço disponível para acomodar os produtos.

### 4. Monte a lista e veja a recomendação

1. Acesse **Escolher caixa**.
2. Adicione os produtos e informe as quantidades, indicando se pertencem à configuração ou se foram comprados avulsos.
3. Clique em **Ver caixa recomendada**.
4. Consulte a caixa sugerida, a ocupação e o espaço livre para proteção. Se houver vários volumes, selecione cada um para conferir o resultado.
5. Use **Ver embalagem em 3D** para visualizar a disposição proposta.

Se faltarem medidas ou não houver uma caixa compatível, revise as pendências indicadas. Ao alterar a lista, clique novamente no botão de recomendação para atualizar o cálculo.

> **Quer conhecer o fluxo primeiro?** Abra **Experimentar com um exemplo** na tela de escolha de caixa. Substitua as medidas fictícias por valores reais antes de usar o resultado na operação.

### Como os itens são separados?

O programa considera a categoria e a origem de cada item. Peças já instaladas no PC não devem ser contadas novamente como embalagens separadas. Memórias e SSDs da configuração podem ser acomodados na caixa da placa-mãe, que precisa constar na lista.

Monitores, suportes e nobreaks comprados avulsos seguem na embalagem do fabricante, assim como cadeiras, mesas e pés de mesa. Outros itens avulsos são agrupados separadamente da configuração.

### Importar uma lista de peças

Na tela **Escolher caixa**, abra **Importar lista de peças** e selecione um arquivo JSON com a lista final. Os SKUs precisam existir no catálogo.

Essa opção simula uma futura integração com ERP. O formato do arquivo e as regras de importação estão em [Lista de peças e integração futura](backend/docs/lista-erp.md).

## Backups e dados

- **Exportar backup:** baixa o arquivo `inbox-backup.json` com os dados do sistema.
- **Restaurar backup:** importa um backup JSON de até **5 MB** e substitui os cadastros atuais. Exporte uma cópia antes de restaurar.
- **Banco local:** os dados ficam em `backend/data/`. A atualização pelo inicializador preserva essa pasta.
- **Pasta `backups/`:** pode ser usada para guardar suas cópias de segurança; mantenha também uma cópia fora da pasta do projeto.

## Se o programa não abrir

| Situação | O que verificar |
| --- | --- |
| Java não encontrado | Abra `inBox.cmd` com acesso à internet para baixar o JDK automaticamente. |
| Maven local não encontrado | Abra `inBox.cmd` com acesso à internet para preparar o Maven automaticamente. |
| Falha ao preparar ou iniciar | Consulte `backend/launcher.log` e `backend/server.log`. |
| O navegador não abriu | Após iniciar o programa, acesse `http://127.0.0.1:8080` manualmente. |

Para encerrar o servidor local, execute na raiz do projeto:

```powershell
.\backend\scripts\parar.ps1
```

## Estrutura e tecnologias

O backend utiliza **Java 17 e Spring Boot**. A interface utiliza **Vue e JavaScript**, com os arquivos servidos pela própria aplicação. Os dados locais são persistidos em **H2**; a configuração de nuvem utiliza **PostgreSQL**.

```text
inBox/
├── inBox.cmd               # Inicializador para Windows
├── backend/
│   ├── src/main/java/      # APIs, regras de embalagem e persistência
│   ├── src/main/resources/ # Interface web e configurações
│   ├── src/test/           # Testes de regras e interface
│   ├── scripts/            # Inicialização, compilação e validação
│   ├── docs/               # Documentação técnica
│   ├── deploy/             # Arquivos de apoio à hospedagem
│   └── data/               # Banco local, criado durante o uso
├── backups/                # Cópias de segurança locais
├── Dockerfile              # Empacotamento em contêiner
└── render.yaml             # Configuração de hospedagem no Render
```

As pastas de dados, backups e ferramentas locais não são versionadas no Git.

## Documentação técnica

| Documento | Conteúdo |
| --- | --- |
| [Arquitetura](backend/docs/arquitetura.md) | Organização interna, inicialização e atualização local. |
| [Mapa do código](backend/docs/mapa-do-codigo.md) | Localização das regras, métodos e cenários de validação. |
| [Lista de peças e ERP](backend/docs/lista-erp.md) | Contrato JSON e regras de composição dos volumes. |
| [Publicação](backend/docs/publicacao.md) | Orientações de hospedagem. |
| [Dependências](backend/docs/dependencias.md) | Informações sobre as dependências utilizadas. |

Para validar alterações, execute na raiz do projeto, com Java, Maven local e Node.js disponíveis:

```powershell
.\backend\scripts\testar.ps1
```

O script executa os testes Java e JavaScript em uma área separada, com banco em memória, sem alterar os cadastros locais.

## Versão pronta para Windows

A distribuição desktop inclui Java e o programa compilado. O usuário não precisa
instalar Java ou Maven, baixar dependências ou compilar o código.

1. Extraia `inBox-Windows.zip` para uma pasta local.
2. Abra `inBox/inBox.exe`. O navegador abre quando o servidor estiver pronto.
3. Para encerrar, use **Encerrar inBox** no ícone da bandeja do Windows (ao lado do relógio).
   Fechar a aba do navegador mantém o programa disponível na bandeja.

Mantenha toda a pasta extraída: o executável depende das subpastas `app` e `runtime`.
Abrir novamente o executável reutiliza o servidor desktop já iniciado.
O endereço local é `http://127.0.0.1:18080`.

Os dados e logs ficam em `%LOCALAPPDATA%\inBox`, separados dos arquivos do programa.
Para levar seus cadastros da versão de desenvolvimento para a desktop, exporte o
backup na versão anterior e restaure-o na nova. A distribuição não inclui dados,
senhas ou configurações privadas da máquina que gerou o pacote.

### Gerar a distribuição (desenvolvedor)

Com JDK 21 ou superior e Maven disponíveis, execute na raiz:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\backend\scripts\empacotar.ps1
```

O script executa os testes Java, compila o programa e usa `jpackage` para gerar
`backend/target/inBox-Windows.zip`. A pasta do executável também é registrada em
`backend/target/desktop-path.txt`. Gere o pacote no Windows e na arquitetura dos
computadores que receberão a distribuição.

Para gerar também um instalador `.exe` com atalhos, adicione `-Installer` ao comando.
Essa opção exige WiX compatível com a versão do JDK instalada. A distribuição ZIP
funciona sem WiX e sem instalação administrativa.
