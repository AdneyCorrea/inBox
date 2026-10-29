# Por onde começar no código

## O caminho de uma sugestão

1. `src/main/resources/static/simple.html` mostra os campos e resultados.
2. `static/pc-loader.js` monta as linhas: produto, quantidade e origem (PC ou avulso).
3. `static/js/packing.js` envia a lista e controla qual volume está sendo visto.
4. `PcApi.calculate` separa os volumes e decide quais embalagens ocupam espaço.
5. `DimensionsApi.calculate` confere medidas, reserva 15% para bolha e testa caixas da menor para a maior.
6. `PackingEngine.fit` tenta as posições e rotações. `static/packing-3d.js` desenha o resultado, sem recalcular o encaixe.

Os caminhos Java abaixo ficam em `src/main/java/br/com/inbox/`.

| Quero entender ou alterar… | Procure aqui |
| --- | --- |
| Campos de produtos, caixas e pedidos | `Model.java` |
| OEM e cabos dentro do PC versus avulsos | `PcApi.defaultMode` e `PcApi.calculate` |
| Memória e SSD dentro da caixa da placa-mãe | Trecho `NESTED` em `PcApi.calculate` |
| Monitor, suporte, nobreak e móveis na caixa do fabricante | Trecho `FACTORY` em `PcApi.calculate` |
| Medidas ausentes e escolha da menor caixa | `DimensionsApi.calculate` |
| Espaço reservado para bolha | `PackingEngine.MAX_OCCUPANCY` (0,85 = até 85% ocupado) |
| Colisões, apoio e giro das peças | `PackingEngine.fit`, `overlap` e `rotations` |
| Integração futura com a lista de SKUs do ERP | `PackingListApi.java` |
| Gravação e conflitos entre duas telas | `Store.read` e `Store.save` |
| Login e proteção das gravações | `SecurityConfiguration.java` e `SessionApi.java` |
| Cadastro e conversão entre cm e mm | `static/js/catalog.js` |
| Comunicação, CSRF e revisão do cadastro | `static/js/http.js` |
| Importação e descarte de respostas antigas | `static/js/packing.js` |
| Pesquisa por nome/SKU e tolerância a erros | `static/product-search.js` |
| Estilo e layout | `static/simple.css`, `theme.css` e `simple.html` |

## Cuidados que evitam erros difíceis de enxergar

- A tela trabalha em **centímetros**; a API e o banco guardam **milímetros**. Não converta duas vezes.
- Medida `null` significa que ninguém mediu ainda. Não use zero para esconder pendências.
- Um produto OEM avulso precisa das medidas normalmente. A dispensa depende de ele estar no PC.
- Os 15% são uma reserva de **volume**, não uma distância obrigatória até as paredes.
- `fit` usa uma heurística conservadora: um item elevado precisa caber inteiramente sobre uma base empilhável. A sugestão não promete o melhor arranjo possível.
- `generation` é a revisão conhecida pela tela. Nunca remova a comparação no `Store`: ela impede que uma pessoa apague a edição de outra.
- O motor antigo (`PackingEngine.plan`) continua por compatibilidade; a tela atual passa pelo `PcApi` e pelo `DimensionsApi`.

## Como validar sem tocar no banco real

Na raiz do projeto, execute:

```powershell
./backend/scripts/testar.ps1
```

É necessário Java, o Maven já incluído em `backend/scripts/tools` e Node.js. A validação usa `backend/target/validation` e H2 **em memória**, sem depender do Supabase.

- `src/test/java/br/com/inbox/PackagingRegressionTest.java`: geometria, 15%, OEM, cabos, avulsos, fabricante e 150 arranjos pseudoaleatórios reprodutíveis.
- `src/test/java/br/com/inbox/AccessAndStorageTest.java`: login, CSRF, acesso anônimo, persistência e conflito de revisões.
- `src/test/js/workflow.test.cjs`: importação assíncrona, respostas antigas, conversão de medidas, categoria manual e conflito na API.

## O que foi otimizado nesta revisão

- Rotações são calculadas uma vez por perfil em cada tentativa de caixa. Orientações repetidas de cubos e peças simétricas são descartadas.
- Pontos já ocupados são retirados da busca; faces livres continuam disponíveis para encaixar ou empilhar.
- A validação da composição usa um índice de IDs, evitando procurar cada peça no catálogo inteiro.
- A reserva de 15% usa uma única constante no motor e no cálculo dimensional.
- Uma importação antiga não reaparece depois de limpar a lista, nem substitui o estado de uma operação mais recente.

Esta revisão não altera dados reais nem publica automaticamente a reorganização local no Render.
