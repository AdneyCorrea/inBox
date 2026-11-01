# inBox

Aplicação local para escolher uma caixa para embalar um pedido. Cadastre produtos e caixas, selecione os itens e suas quantidades e visualize a disposição sugerida em 3D.

O cálculo procura a menor caixa, por volume, em que consegue acomodar todos os itens, reservando pelo menos **15% de volume livre** para proteção.

## Download para Windows

**[Baixar inBox para Windows — ZIP portátil](https://github.com/AdneyCorrea/inBox/releases/latest/download/inBox-Windows.zip)**

Extraia o ZIP, abra a pasta `inBox` e execute **Iniciar inBox.cmd**. O site abre no navegador, sem instalar Java, Maven ou IDE e sem precisar de internet. Mantenha a janela do inBox aberta durante o uso.

Veja também [todas as versões disponíveis](https://github.com/AdneyCorrea/inBox/releases).

## Sumário

- [Funcionalidades](#funcionalidades)
- [Executar sem instalar dependências](#executar-sem-instalar-dependências)
- [Gerar o pacote portátil](#gerar-o-pacote-portátil)
- [Executar em desenvolvimento](#executar-em-desenvolvimento)
- [Como usar](#como-usar)
- [Dados e backup](#dados-e-backup)
- [Regras e limitações](#regras-e-limitações)
- [Tecnologias e estrutura](#tecnologias-e-estrutura)
- [API HTTP](#api-http)
- [Testes](#testes)
- [Solução de problemas](#solução-de-problemas)

## Funcionalidades

- Cadastro, edição e exclusão de produtos e caixas.
- Busca de produtos por nome ou SKU, ignorando acentos e diferenças entre maiúsculas e minúsculas.
- Organização e filtro dos produtos por categoria.
- Seleção de vários produtos e quantidades por pedido.
- Recomendação de caixa considerando dimensões, rotações e volume livre.
- Visualização 3D da disposição, com giro, zoom e exibição gradual das unidades.
- Prévia das caixas e comparação de duas caixas na mesma escala.
- Persistência dos cadastros em arquivos JSON locais, sem banco de dados externo.
- Execução offline: a interface e o Vue estão incluídos no projeto.

## Executar sem instalar dependências

**Para o usuário final no Windows, utilize o pacote `inBox-Windows.zip`.** Ele inclui o servidor e o runtime Java; não exige instalar Java, Maven, Node.js ou uma IDE. É necessário apenas um navegador moderno.

1. Extraia **todo o ZIP** para uma pasta com permissão de escrita.
2. Entre na pasta `inBox` extraída.
3. Dê dois cliques em **Iniciar inBox.cmd**.
4. Aguarde o navegador abrir automaticamente.
5. Mantenha a janela do inBox aberta enquanto usa o site.

Para encerrar, feche a janela do inBox ou pressione `Ctrl+C` nela. Fechar apenas a aba do navegador mantém o servidor em execução.

O pacote escolhe uma porta livre e aceita conexões apenas do próprio computador. Se o navegador não abrir, copie o endereço `http://127.0.0.1:...` exibido na janela. A porta pode mudar a cada execução.

Use uma única instância por pasta. O pacote deve ter a mesma arquitetura do Windows de destino, por exemplo, x64.

> Abrir `index.html` diretamente não executa o sistema completo: os cadastros e o cálculo dependem da API Java incluída no pacote.

## Gerar o pacote portátil

Esta etapa é feita pelo desenvolvedor antes de distribuir a aplicação.

**Requisitos:** Windows, Maven e JDK 17 ou superior com `jlink`. A primeira compilação precisa de internet para obter as dependências Maven; o pacote pronto funciona offline.

Na raiz do projeto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\gerar-portatil.ps1 -Jdk "C:\caminho\do\jdk"
```

Se o Maven não estiver no `PATH`, informe também seu executável:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\gerar-portatil.ps1 -Jdk "C:\caminho\do\jdk" -Maven "C:\caminho\do\mvn.cmd"
```

O script executa os testes, gera o JAR e inclui um runtime Java no pacote. Ao terminar, informa o caminho de `target/portatil-<identificador>/inBox-Windows.zip`.

Distribua **o ZIP completo**. Ele usa o catálogo inicial do projeto e não copia os cadastros particulares de `catalogo-local/` da máquina de desenvolvimento.

## Executar em desenvolvimento

Requisitos: JDK 17 ou superior, Maven e navegador moderno.

### Pela IDE

1. Abra a raiz do projeto e importe o projeto Maven.
2. Aguarde a resolução das dependências.
3. Abra `src/main/java/br/com/pichau/inbox/InboxApplication.java`.
4. Execute o método `main` pela opção **Run Java**.
5. Acesse <http://localhost:8080>.

### Pelo terminal

Execute a partir da raiz, onde está o `pom.xml`:

```powershell
mvn spring-boot:run
```

Ou gere e execute o JAR:

```powershell
mvn package
java -jar target/inbox-0.0.1-SNAPSHOT.jar
```

Acesse <http://localhost:8080>. Encerre com `Ctrl+C` no terminal ou **Stop** na IDE. Reinicie após alterar o código Java.

O JAR isolado exige Java instalado. Para usuários sem Java, distribua o pacote portátil. Os comandos de desenvolvimento usam Maven instalado na máquina.

## Como usar

1. Em **Cadastros**, confira ou cadastre os produtos e as caixas disponíveis.
2. Informe as dimensões externas da embalagem de cada produto e as dimensões internas das caixas, em **centímetros**.
3. Em **Embalar pedido**, busque ou filtre os produtos e selecione os itens.
4. Ajuste as quantidades, respeitando o limite de **200 unidades no total**.
5. Solicite o cálculo e confira a caixa recomendada, o percentual livre e a disposição em 3D.

O SKU é opcional. A categoria organiza o catálogo e não altera o cálculo geométrico. As categorias incluem cabos, gabinetes, computadores montados, processadores, placas-mãe, memórias, placas de vídeo, armazenamento, fontes, coolers, ventoinhas e sem categoria.

A seleção e o resultado ficam apenas na página: não há histórico persistente de pedidos ou cálculos. Alterar a seleção descarta o resultado anterior.

## Dados e backup

| Local | Conteúdo |
| --- | --- |
| `src/main/resources/catalogo-inicial/` | Catálogos iniciais incluídos no aplicativo |
| `catalogo-local/produtos.json` | Produtos salvos durante o uso |
| `catalogo-local/caixas.json` | Caixas salvas durante o uso |

No pacote portátil, `catalogo-local/` fica ao lado de **Iniciar inBox.cmd**. Em desenvolvimento, fica no diretório de trabalho do processo, normalmente a raiz do projeto.

Cada catálogo usa seu arquivo local quando ele existe; caso contrário, carrega o catálogo inicial. O arquivo local é criado ao salvar uma alteração. Não há mesclagem automática com futuras alterações do catálogo inicial, nem importação automática de `inbox-backup.json`.

**Backup:** encerre a aplicação e copie a pasta `catalogo-local` para outro local. Para restaurar, encerre o aplicativo, preserve uma cópia dos dados atuais e substitua os arquivos pelos do backup. Ao atualizar o pacote, leve essa pasta para a nova instalação antes de iniciar.

Os dados são carregados em memória na inicialização. Edições manuais nos JSON exigem reiniciar a aplicação. Use uma única instância por diretório de dados, pois não há coordenação entre processos.

Para escolher outra pasta ao executar o JAR:

```powershell
java -jar target/inbox-0.0.1-SNAPSHOT.jar --inbox.dados-diretorio=C:/inbox-dados
```

A interface também tenta recuperar cadastros antigos do `localStorage` na chave `inbox.catalog.v1`. Produtos recuperados precisam ser editados e salvos no servidor antes de participar do cálculo.

## Regras e limitações

- A interface exibe centímetros; a API e os JSON usam milímetros inteiros. Ao salvar, as medidas são convertidas e arredondadas para o milímetro mais próximo.
- Os nomes são obrigatórios e aceitam até 120 caracteres. O SKU aceita até 80 caracteres.
- Cada dimensão deve estar entre 1 e 100.000 mm, equivalentes a 0,1 e 10.000 cm.
- Cada cálculo aceita entre 1 e 200 unidades, acomodadas em uma única caixa.
- O algoritmo ordena as caixas por volume e tenta diferentes ordens de itens, divisões do espaço livre e rotações.
- A reserva de 15% considera o volume total; não representa uma camada de proteção com espessura definida ao redor de cada item.

O algoritmo é heurístico: encontrar uma solução não significa esgotar todas as disposições possíveis. Se nenhuma caixa for encontrada, outras disposições físicas ainda podem existir.

Os produtos são tratados como blocos retangulares. O cálculo não considera peso, fragilidade, resistência ao empilhamento, orientação obrigatória, formatos irregulares, frete ou estoque. Também não divide o pedido em várias caixas nem encaixa componentes dentro de outros produtos.

## Tecnologias e estrutura

| Parte | Tecnologia |
| --- | --- |
| Servidor e API | Java 17, Spring Boot 4.1.2 e Spring MVC |
| Interface | HTML, CSS, JavaScript e Vue incluído localmente |
| Visualização | Canvas 3D e prévias em SVG |
| Persistência | Arquivos JSON locais com Jackson |
| Modelos | Lombok |
| Build e testes | Maven e ferramentas de teste do Spring Boot |
| Distribuição Windows | JAR executável, runtime gerado com `jlink` e iniciador CMD |

```text
src/main/java/br/com/pichau/inbox/
  InboxApplication.java      Inicialização
  NavegadorLocal.java         Abertura do navegador no pacote portátil
  controller/                Endpoints HTTP
  service/                   Cadastros e cálculo de embalagem
  database/model/            Modelos de produtos e caixas
  database/repository/       Leitura e gravação dos catálogos
  handler/                   Tratamento de erros de cadastro
src/main/resources/
  static/                    Interface e Vue local
  catalogo-inicial/           Dados iniciais
  application.yaml           Configuração do Spring Boot
src/test/                    Testes automatizados
scripts/gerar-portatil.ps1    Geração do pacote Windows
distribuicao/               Iniciador e instruções do pacote
```

## API HTTP

A interface e a API são servidas pela mesma aplicação.

| Método | Rota | Operação |
| --- | --- | --- |
| `GET` | `/api/produtos` | Listar produtos |
| `POST` | `/api/produtos` | Cadastrar produto |
| `PUT` | `/api/produtos/{id}` | Atualizar produto |
| `DELETE` | `/api/produtos/{id}` | Remover produto |
| `GET` | `/api/caixas` | Listar caixas |
| `POST` | `/api/caixas` | Cadastrar caixa |
| `PUT` | `/api/caixas/{id}` | Atualizar caixa |
| `DELETE` | `/api/caixas/{id}` | Remover caixa |
| `POST` | `/api/embalagens/calcular` | Calcular embalagem |

Exemplo de corpo para cadastrar um produto (dimensões em milímetros):

```json
{
  "nome": "Produto de exemplo",
  "sku": "SKU-001",
  "categoria": "PSU",
  "comprimento": 335,
  "largura": 200,
  "altura": 95
}
```

O servidor gera o identificador. Para caixas, envie apenas nome e dimensões. O cálculo recebe identificadores retornados pela API:

```json
{
  "itens": [
    { "produtoId": "identificador-do-produto", "quantidade": 1 }
  ]
}
```

A resposta contém `caixa`, `posicoes`, `percentualLivre` e `mensagem`. As posições trazem coordenadas e dimensões orientadas em milímetros. Quando nenhuma disposição é encontrada, `caixa` é `null` e a lista de posições fica vazia.

## Testes

Na raiz do projeto:

```powershell
mvn test
```

Os testes cobrem a inicialização, as APIs, a persistência dos catálogos e o cálculo de embalagem. A geração do pacote portátil também executa os testes antes de empacotar.

## Solução de problemas

| Situação | Como resolver |
| --- | --- |
| O iniciador não encontra o Java incluído | Extraia todo o ZIP e mantenha suas pastas juntas. |
| O navegador não abriu | Use o endereço exibido na janela do inBox. |
| A porta 8080 está ocupada em desenvolvimento | Encerre o processo que usa a porta ou execute o JAR com `--server.port=8081`. O pacote portátil escolhe uma porta livre. |
| Não foi possível salvar um cadastro | Confira a permissão de escrita da pasta de dados. |
| A aplicação não inicia após editar JSON | Verifique se os arquivos são válidos ou restaure um backup com a aplicação encerrada. |
| Não foi encontrada uma caixa | Confira dimensões, quantidades, caixas cadastradas e a reserva mínima de volume. |
| O comando `mvn` não existe | Instale/configure Maven na máquina de desenvolvimento; o usuário do pacote portátil não precisa dele. |
