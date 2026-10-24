# inBox — escolher a caixa

Aplicativo de embalagem para celular e computador: cadastre as dimensões das embalagens dos produtos e das caixas, selecione o que vai junto e receba uma sugestão de caixa com pelo menos 15% do volume interno livre para bolha.

## Publicação na internet

Preparado para Render + Supabase, com login obrigatório no perfil cloud. Veja [o guia de publicação](docs/publicacao.md). A configuração não significa que o endereço online já foi publicado.

## Abrir localmente para desenvolvimento

Execute `iniciar.cmd` e acesse [http://127.0.0.1:8080](http://127.0.0.1:8080). É necessário Java 17 ou superior. Para encerrar o servidor, execute `parar.ps1` no PowerShell.

## Usar

1. **Produtos:** informe nome, código opcional e comprimento, largura e altura externos da embalagem, em centímetros. Medidas podem ficar pendentes.
2. **Caixas:** informe nome e as três medidas internas, em centímetros.
3. **Escolher caixa:** adicione os produtos e quantidades que vão juntos e clique em **Encontrar caixa**.

O resultado mostra a caixa sugerida, suas medidas, o percentual ocupado e o espaço livre. Usa encaixe em três dimensões; somar volumes não é suficiente. Produtos podem ser girados nas seis orientações ortogonais. O cálculo usa somente dimensões, sem peso ou capacidade de carga. Não garante a menor solução global; limite de 100 unidades por cálculo.

Em **Ver embalagem em 3D**, os blocos mostram as dimensões, posições e orientações do cálculo. Arraste para girar; use a roda do mouse, dois dedos ou os botões +/− para aproximar. Há vista superior, recentralização e legenda por embalagem. A visualização funciona localmente, sem dependências externas. O espaço livre representa a reserva para bolha; não é uma simulação do material plástico.

A lista final de peças é informada na tela ou importada por JSON. O seletor usa departamentos, categorias e palavras-chave. A integração automática com pedidos do ERP será adicionada depois.

Não há fila, operador ou conferência obrigatória na interface. A seleção manual continua disponível. O vínculo automático com compras reais depende da integração com a composição final fornecida pelo sistema da empresa; o SKU base sozinho não contém as personalizações do cliente.

## Sem medidas reais

É possível cadastrar nomes e completar medidas depois. Para experimentar, use **Testar com medidas de exemplo** em uma base simples vazia. Os exemplos são identificados como fictícios. Ao inserir medidas reais em um exemplo, desmarque **Estas medidas ainda são de exemplo**.

## Dados e backup

Os cadastros ficam no banco local `backend/data/inbox.mv.db`. Os botões de backup exportam e restauram os dados. Restaurar substitui a base atual e pede confirmação; exporte uma cópia antes.

Os dados do protótipo anterior foram preservados no banco/backup, mas não aparecem na nova interface. Os novos cadastros simplificados são independentes. Os documentos e APIs do fluxo anterior permanecem no projeto como referência histórica.

## Desenvolvimento

Backend Java/Spring Boot e interface Vue 3. O desenvolvimento local usa H2; o perfil cloud usa PostgreSQL do Supabase e exige login. A persistência é por documento JSON, com proteção contra alterações concorrentes. A integração com o ERP ainda não está implementada.

Pare o aplicativo antes de recompilar. Com Maven: `mvn -f backend/pom.xml package`. Nesta máquina também há Maven em `tools/apache-maven-3.9.11/bin/mvn.cmd`. Após compilar, use o iniciador novamente.

Testes da interface e geometria 3D: `node --test tests/simple.test.cjs tests/packing-3d.test.cjs`. Testes Java: `mvn -f backend/pom.xml test`. O uso normal não exige Node nem internet.
