# inBox

O inBox ajuda a escolher uma caixa para embalar um pedido. Você cadastra as dimensões dos produtos e das caixas disponíveis, seleciona os itens e suas quantidades e solicita o cálculo. O sistema procura uma disposição tridimensional que caiba em uma única caixa e deixe pelo menos 15% do volume livre para proteção.

Este documento descreve a implementação atual em `src/`.

## Funcionalidades

- Cadastro, edição e exclusão de produtos e caixas.
- Busca de produtos por nome ou SKU, ignorando maiúsculas, minúsculas e acentos.
- Organização e filtragem dos produtos por categoria.
- Seleção de vários produtos e quantidades para o mesmo pedido.
- Recomendação de caixa considerando dimensões, rotações e volume livre.
- Visualização da disposição em 3D, com giro, zoom e exibição gradual das unidades.
- Prévia das caixas e comparação de duas caixas na mesma escala.
- Persistência dos cadastros em arquivos JSON locais.

## Tecnologias e organização

| Parte | Implementação |
| --- | --- |
| Servidor e API | Java 17, Spring Boot 4.1.2 e Spring MVC |
| Interface | HTML, CSS e JavaScript com Vue, servido pelo próprio Spring Boot |
| Visualização | Canvas para a disposição dos produtos e SVG para as prévias das caixas |
| Dados | Arquivos JSON e listas em memória, sem banco SQL |
| Conversão JSON | Jackson |
| Modelos | Lombok para gerar getters e setters |
| Build e testes | Maven e dependências de teste do Spring Boot |

O Vue está incluído em `src/main/resources/static/vendor/`. A interface não exige Node.js, instalação de pacotes npm ou build separado. Após obter as dependências Java, a execução usa os recursos locais do projeto.

## Como executar

### Requisitos

- JDK 17 ou compatível com o projeto.
- Maven instalado e disponível no terminal, ou uma IDE com suporte a Maven.
- Navegador moderno com JavaScript habilitado.

Confira o ambiente:

```powershell
java -version
mvn -version
```

Execute os comandos a partir da raiz do projeto, onde está o `pom.xml`. Isso também define onde o diretório de dados relativo será localizado.

### Pela IDE

1. Abra a raiz do projeto e importe as dependências do Maven.
2. Abra `src/main/java/br/com/pichau/inbox/InboxApplication.java`.
3. Execute o método `main` pela opção Run da IDE.
4. Acesse <http://localhost:8080>.

### Pelo terminal

```powershell
mvn spring-boot:run
```

Para gerar e executar o JAR:

```powershell
mvn clean package
java -jar target/inbox-0.0.1-SNAPSHOT.jar
```

O projeto contém propriedades do Maven Wrapper em `.mvn/`, mas atualmente não contém os scripts `mvnw` e `mvnw.cmd`. Os exemplos usam o comando `mvn` instalado no ambiente.

Para encerrar uma execução iniciada no terminal, use `Ctrl+C`. Pela IDE, use Stop. Depois de alterar código Java, reinicie a aplicação para carregar a nova versão.

## Como usar

### 1. Conferir os cadastros

Abra a aba **Cadastros** e selecione produtos ou caixas. É possível criar um registro, editar um existente ou removê-lo.

Para produtos, informe nome, SKU, categoria e comprimento, largura e altura. Para caixas, informe nome e as três dimensões. O SKU pode ficar em branco.

As medidas dos produtos representam as dimensões externas da embalagem de cada item. As medidas das caixas representam o espaço interno disponível. Essa diferença é essencial para que a recomendação corresponda ao encaixe real.

As alterações feitas pela interface são enviadas à API. Os novos registros recebem um identificador UUID gerado pelo servidor. A edição mantém o identificador existente.

### 2. Montar o pedido

1. Abra **Embalar pedido**.
2. Localize os produtos pela busca ou pelo filtro de categoria.
3. Marque os produtos que farão parte da embalagem.
4. Ajuste a quantidade de cada produto.
5. Solicite o cálculo da caixa.

Cada produto selecionado começa com uma unidade. O cálculo aceita no máximo **200 unidades no total**, somando todos os produtos. Embora os campos de quantidade da interface aceitem valores até 999, seleções acima de 200 unidades não podem ser calculadas.

A seleção e o resultado ficam no estado da página; não há histórico persistente de pedidos ou de cálculos. Ao alterar a seleção, o resultado anterior é descartado e deve ser calculado novamente.

### 3. Interpretar o resultado

Quando encontra uma disposição, o sistema retorna a caixa, o percentual de volume livre e a posição e orientação de cada unidade. A recomendação é a menor caixa, por volume, na qual o algoritmo conseguiu acomodar os itens.

Na visualização 3D:

- Arraste para girar ou use as setas com o canvas em foco.
- Use a roda do mouse ou as teclas `+` e `-` para ajustar o zoom.
- Use **TopView** para visualizar de cima.
- Use **Restaurar** para recuperar a câmera inicial.
- Ajuste **Unidades visíveis** para revelar os itens progressivamente.

A tabela abaixo da visualização mostra cada unidade e suas dimensões na orientação escolhida. As cores identificam os produtos. A câmera altera apenas a visualização, sem recalcular o encaixe.

No cadastro de caixas, a janela de inspeção permite comparar duas caixas com câmera e escala compartilhadas. As miniaturas individuais são ajustadas ao espaço disponível e não devem ser usadas sozinhas para comparar tamanhos absolutos.

## Unidades e validações

A interface usa **centímetros**. A API, os arquivos de dados e o algoritmo usam **milímetros inteiros**.

Ao salvar pela interface, cada medida é multiplicada por 10 e arredondada para o milímetro mais próximo. Ao exibir, o valor é dividido por 10.

Exemplo da fonte Pichau Cluster 850W:

| Medida | Interface | API e JSON |
| --- | --- | --- |
| Comprimento | 33,5 cm | 335 mm |
| Largura | 20 cm | 200 mm |
| Altura | 9,5 cm | 95 mm |

As regras de cadastro no servidor são:

- Nome obrigatório, não composto apenas por espaços, com até 120 caracteres; os espaços nas extremidades são removidos.
- Cada dimensão entre 1 e 100.000 mm, equivalente a 0,1 e 10.000 cm.
- SKU de produto com até 80 caracteres.
- Categoria do produto pertencente à lista aceita pela aplicação.

### Categorias de produtos

| Código | Nome exibido |
| --- | --- |
| `CABLE` | Cabos |
| `CASE` | Gabinetes |
| `PREPARED_PC` | Computadores montados |
| `CPU` | Processadores |
| `MOTHERBOARD` | Placas-mãe |
| `MEMORY` | Memórias |
| `GPU` | Placas de vídeo |
| `SSD` | Armazenamento / SSD |
| `PSU` | Fontes |
| `COOLER` | Coolers |
| `FAN` | Ventoinhas |
| `UNCATEGORIZED` | Sem categoria |

O formulário oferece as categorias definidas na interface. Já os filtros mostram somente categorias presentes nos produtos carregados. Por isso, se o último produto de uma categoria for removido, ela desaparece do filtro, mas continua disponível para novos cadastros.

As categorias são definidas em `static/app.js` e validadas em `ProdutoService.java`. Para adicionar uma nova, atualize ambos. A categoria organiza o catálogo; não altera as regras geométricas do cálculo.

## Como o cálculo funciona

A lógica está em `EmbalagemService.java`.

1. **Valida o pedido:** exige itens válidos, produtos existentes e entre 1 e 200 unidades no total.
2. **Expande as quantidades:** três unidades de um produto passam a ser três volumes independentes.
3. **Soma os volumes:** usa comprimento × largura × altura de cada unidade.
4. **Ordena as caixas por volume:** testa primeiro as menores, desconsiderando caixas com dimensões não positivas.
5. **Verifica a reserva:** descarta caixas nas quais o volume dos produtos ultrapassa 85% do volume interno.
6. **Tenta acomodar os itens:** para cada caixa, combina três ordens de produtos com duas formas de dividir o espaço livre, totalizando até seis tentativas.
7. **Testa rotações:** cada unidade pode assumir as seis permutações de comprimento, largura e altura.
8. **Retorna a primeira solução:** se todas as unidades couberem, encerra a busca e retorna a caixa e as posições.

As três ordens priorizam, em ordem decrescente, volume, maior dimensão ou menor dimensão. Durante a acomodação, o algoritmo escolhe o menor espaço livre em que alguma orientação caiba; em empate de volume, prefere uma orientação com menor altura. Depois de colocar um item, divide o espaço restante em regiões sem sobreposição. O espaço superior criado tem a base do produto colocado abaixo.

O percentual informado é:

```text
percentualLivre = (1 - volumeTotalDosProdutos / volumeInternoDaCaixa) × 100
```

A reserva de 15% é uma regra de volume global. Ela não adiciona uma camada de plástico-bolha ao redor de cada produto nem garante uma distância mínima entre os itens.

### Limitações do cálculo

O algoritmo é heurístico: testa algumas estratégias, sem esgotar todas as disposições possíveis. Se não encontrar uma caixa, isso não prova que o pedido seja fisicamente impossível de embalar.

Os itens são tratados como blocos retangulares. O modelo atual não considera peso, resistência ao empilhamento, fragilidade, orientação obrigatória, formatos irregulares, custo de frete ou estoque de caixas. Também não divide um pedido entre várias caixas e não encaixa componentes dentro de outros produtos.

A indicação deve ser conferida conforme as condições reais de embalagem e proteção dos produtos.

## Onde os dados ficam

Há duas fontes de catálogo:

| Local | Função |
| --- | --- |
| `src/main/resources/catalogo-inicial/produtos.json` | Produtos iniciais incluídos na aplicação |
| `src/main/resources/catalogo-inicial/caixas.json` | Caixas iniciais incluídas na aplicação |
| `catalogo-local/produtos.json` | Produtos salvos durante o uso |
| `catalogo-local/caixas.json` | Caixas salvas durante o uso |

Na inicialização, cada repository verifica seu arquivo local. Se existir, ele prevalece sobre o catálogo inicial. Se não existir, o repository carrega o recurso inicial. Os arquivos locais são gravados quando ocorre uma alteração no respectivo catálogo.

Não há mesclagem automática: adicionar um produto ao catálogo inicial não o acrescenta a um arquivo local já existente. O arquivo `inbox-backup.json` também não é importado automaticamente.

Os repositories carregam os dados em memória ao iniciar. Cada alteração cria uma nova lista, grava o JSON em um arquivo temporário e substitui o arquivo definitivo. A substituição tenta usar movimentação atômica e recorre à substituição comum quando ela não é suportada. A lista em memória é atualizada após a gravação.

As operações dos repositories são sincronizadas dentro de uma execução da aplicação. Não existe coordenação entre dois processos usando o mesmo diretório. Use uma única instância por diretório de dados.

O diretório `catalogo-local/` está no `.gitignore`: os cadastros de uso não entram automaticamente no controle de versão.

### Alterar o diretório de dados

A propriedade `inbox.dados-diretorio` define a pasta. O padrão é `catalogo-local`, relativo ao diretório de trabalho do processo.

Exemplo com um caminho explícito:

```powershell
java -jar target/inbox-0.0.1-SNAPSHOT.jar --inbox.dados-diretorio=C:/inbox-dados
```

Também é possível configurá-la em `src/main/resources/application.yaml`:

```yaml
spring:
  application:
    name: inbox

inbox:
  dados-diretorio: C:/inbox-dados
```

### Backup e restauração

Para preservar os cadastros atuais:

1. Pare a aplicação.
2. Copie os arquivos `produtos.json` e `caixas.json` do diretório de dados para outro local.
3. Reinicie a aplicação.

Para restaurar, pare a aplicação, substitua os arquivos pelos da cópia e inicie novamente. Preserve uma cópia dos dados anteriores antes da substituição.

Ao editar JSON manualmente, mantenha arrays válidos, identificadores e medidas em milímetros. Reinicie a aplicação após a edição: ela não monitora alterações externas e uma gravação feita com dados antigos em memória pode sobrescrever a edição manual.

O formato normal dos produtos é:

```json
[
  {
    "id": "pichau-hf-psu",
    "nome": "Pichau Cluster 850W",
    "sku": "48234",
    "categoria": "PSU",
    "comprimento": 335,
    "largura": 200,
    "altura": 95
  }
]
```

As caixas usam `id`, `nome`, `comprimento`, `largura` e `altura`. Os modelos também aceitam os aliases antigos `name`, `category` (produtos), `l`, `w` e `h`, ignorando campos desconhecidos. Isso permite ler registros antigos, mas não converte um backup completo com outra estrutura em catálogo: é necessário extrair os arrays correspondentes. Os campos antigos como `pcMode` e `topic` não participam da lógica atual.

### Cadastros antigos no navegador

A interface tenta recuperar registros legados do `localStorage`, na chave `inbox.catalog.v1`. Eles aparecem como registros locais do navegador, separados dos dados do servidor.

Produtos com essa origem não podem participar do cálculo enquanto não forem salvos no servidor. Ao editar e salvar um desses registros, a interface cria o cadastro pela API e remove sua cópia legada. Os novos cadastros do fluxo atual são enviados ao servidor.

## API HTTP

A interface e a API são servidas pela mesma aplicação. Os exemplos abaixo usam a porta padrão 8080.

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
| `POST` | `/api/embalagens/calcular` | Calcular uma embalagem |

### Cadastrar um produto

```http
POST /api/produtos
Content-Type: application/json

{
  "nome": "Pichau Cluster 850W",
  "sku": "48234",
  "categoria": "PSU",
  "comprimento": 335,
  "largura": 200,
  "altura": 95
}
```

O servidor gera o `id`. O corpo de atualização usa os mesmos campos; o identificador vem da URL. Para cadastrar uma caixa, envie nome e dimensões a `/api/caixas`.

### Solicitar um cálculo

Substitua o identificador pelo valor retornado na listagem de produtos:

```http
POST /api/embalagens/calcular
Content-Type: application/json

{
  "itens": [
    { "produtoId": "pichau-hf-psu", "quantidade": 1 }
  ]
}
```

A resposta contém:

- `caixa`: cadastro da caixa recomendada, ou `null` quando nenhuma disposição é encontrada.
- `posicoes`: uma entrada por unidade, com `produtoId`, `nome`, coordenadas `x`, `y`, `z` e dimensões orientadas `comprimento`, `largura`, `altura`, em milímetros.
- `percentualLivre`: percentual de volume não ocupado pelos produtos.
- `mensagem`: explicação do resultado.

As coordenadas usam a origem da caixa em `(0, 0, 0)`, com `z` representando a altura. Quando nenhuma caixa é encontrada, a lista de posições vem vazia e o percentual é zero.

### Códigos de resposta

- `200`: consulta, atualização ou cálculo concluído. Um cálculo sem caixa compatível também retorna `200`, com `caixa: null`.
- `201`: cadastro criado.
- `204`: exclusão concluída, sem corpo de resposta.
- `400`: validação rejeitada, como dimensões ou quantidades inválidas.
- `404`: identificador não encontrado.

Nos erros de validação e de registro inexistente dos cadastros, o corpo contém `mensagem`. O cálculo usa o tratamento de erro HTTP do Spring por meio de `ResponseStatusException`.

## Estrutura do código

```text
src/
├── main/
│   ├── java/br/com/pichau/inbox/
│   │   ├── InboxApplication.java
│   │   ├── controller/          # Endpoints HTTP
│   │   ├── service/             # Validações e algoritmo de embalagem
│   │   ├── database/
│   │   │   ├── model/           # Produto e Caixa
│   │   │   └── repository/      # Leitura, memória e gravação dos JSONs
│   │   └── handler/             # Respostas de erro dos cadastros
│   └── resources/
│       ├── application.yaml
│       ├── catalogo-inicial/
│       └── static/
│           ├── index.html       # Estrutura e componentes da tela
│           ├── styles.css      # Aparência e layout
│           ├── app.js          # Estado Vue, filtros e chamadas à API
│           ├── packing-view.js # Desenho da disposição em 3D
│           ├── box-preview.js  # Prévias e comparação de caixas
│           └── vendor/         # Vue distribuído com o projeto
└── test/java/br/com/pichau/inbox/
```

O fluxo de cadastro é **interface → controller → service → repository → JSON**. O service valida os dados e o repository cuida da persistência. No cálculo, o controller entrega os itens ao `EmbalagemService`, que consulta os dois repositories e retorna a disposição para ser desenhada na interface.

## Testes

Execute:

```powershell
mvn test
```

Para executar somente os testes do algoritmo:

```powershell
mvn "-Dtest=EmbalagemServiceTests" test
```

A suíte inclui inicialização do contexto Spring, leitura dos catálogos, consultas HTTP, cadastro/edição/exclusão com persistência, validações e cálculo. Os testes do algoritmo verificam rotações, reserva de volume, encaixe físico, sobreposição, limites geométricos, combinações de produtos e limites de quantidade.

## Problemas comuns

| Sintoma | O que verificar |
| --- | --- |
| Uma categoria desapareceu | Os filtros só mostram categorias com produtos. Confira se ainda existe um cadastro naquela categoria. |
| Alterei o catálogo inicial e nada mudou | O arquivo local existente tem prioridade. Atualize os dados pelo cadastro ou pelo procedimento de restauração. |
| Editei o JSON e a tela continua igual | Pare e reinicie a aplicação; o catálogo fica em memória. |
| Os cadastros mudam conforme a execução | Confira o diretório de trabalho da IDE/terminal e o valor de `inbox.dados-diretorio`. |
| A interface diz que a aplicação está desatualizada | Pare a execução anterior, inicie `InboxApplication.java` novamente e recarregue a página. |
| A rota de edição não está disponível | Reinicie o servidor com a versão atual do código. |
| Não foi possível conectar à aplicação | Confirme que o Spring Boot está rodando e abra a interface pelo endereço HTTP do servidor. |
| Nenhuma caixa foi encontrada | Confira medidas, unidades, quantidades, reserva de 15% e opções cadastradas. A busca não testa todas as disposições possíveis. |
| A aplicação falha ao carregar o catálogo | Confira a validade dos JSONs e as permissões de leitura da pasta indicada no erro. |
| Não é possível salvar alterações | Confira permissão de escrita e espaço disponível no diretório de dados. |
| A porta 8080 está ocupada | Encerre a execução anterior ou use outra porta, por exemplo `java -jar target/inbox-0.0.1-SNAPSHOT.jar --server.port=8081`. |

## Escopo atual

O projeto implementa um catálogo local e um simulador de embalagem. Não há autenticação, permissões por usuário, integração com ERP, gestão de estoque, histórico de pedidos ou importador de backup na interface. A API atual permite alterar os cadastros sem login; considere isso ao decidir onde disponibilizar o servidor.
