# inBox

Aplicação local para cadastrar produtos e caixas e calcular a menor caixa compatível com um pedido, com pelo menos 15% de volume livre para proteção. A interface responsiva usa Vue.js incluído no projeto e é servida pelo próprio Spring Boot.

## Executar localmente

Requisitos: JDK 17 ou superior e uma IDE com suporte a Java e Maven.

1. Abra a raiz do projeto na IDE e importe como projeto Maven.
2. Aguarde a resolução das dependências do `pom.xml`.
3. Abra `src/main/java/br/com/pichau/inbox/InboxApplication.java` e execute o método `main` com **Run Java**.
4. Acesse <http://localhost:8080>. No VS Code, a configuração **inBox - Abrir na web** também abre o navegador.
5. Encerre com **Stop** na IDE.

Com Maven instalado, também é possível iniciar pela raiz com `mvn spring-boot:run`.

A IDE configura as dependências Spring Boot no classpath e compila automaticamente o Java. Essa compilação pode recriar `target/`, que contém arquivos temporários e está ignorada no Git. Não é necessário gerar JAR ou ZIP nem instalar Node.js para executar a interface.

## Organização e fluxo

```text
Interface Vue.js → Controller → Service → Repository → JSON
Interface Vue.js ← Controller ← Service ← Repository
```

- `controller/`: recebe requisições HTTP e devolve as respostas à interface.
- `service/`: valida os cadastros e realiza os cálculos de embalagem.
- `database/repository/`: consulta e salva produtos e caixas.
- `database/model/`: representa produtos e caixas.
- `handler/`: transforma erros de cadastro em respostas HTTP para a interface.
- `src/main/resources/static/`: interface Vue, estilos e visualizações.
- `src/main/resources/catalogo-inicial/`: catálogos iniciais de produtos e caixas.
- `src/main/resources/application.yaml`: configuração da aplicação.

Os pacotes Java acima ficam em `src/main/java/br/com/pichau/inbox/`. As APIs são `/api/produtos`, `/api/caixas` e `/api/embalagens/calcular`. A interface envia os dados ao controller; o service faz o cálculo consultando os repositories.

## Dados locais

Os cadastros ficam em `catalogo-local/produtos.json` e `catalogo-local/caixas.json`, relativos ao diretório de execução, normalmente a raiz do projeto. Na ausência de um arquivo local, o repository usa o catálogo inicial correspondente. Para fazer backup, encerre a aplicação e copie `catalogo-local/`.

## Uso

Cadastre produtos e caixas, selecione os itens e quantidades do pedido e solicite o cálculo. A interface exibe centímetros; a API e os JSON usam milímetros inteiros.

O cálculo aceita até 200 unidades, considera rotação e empilhamento e procura uma única caixa com pelo menos 15% de volume livre. A visualização mostra a disposição sugerida. O algoritmo é heurístico e não considera peso, fragilidade ou resistência das caixas.
