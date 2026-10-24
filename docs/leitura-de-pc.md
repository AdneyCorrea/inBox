# Leitura de SKU e composição final

Na tela **Escolher caixa**, leia ou digite o código e pressione Enter. O aplicativo carrega as peças cadastradas, separa as embalagens e calcula a sugestão com reserva de 15%, incluindo a cena 3D.

## Exemplos disponíveis

| Código | Conteúdo |
| --- | --- |
| `66705` ou `PCM-Pichau-Highflyer-66705` | Base Highflyer |
| `68475` ou `PCM-Pichau-Gamer-68475` | Base Gamer |
| `DEMO-PEDIDO-66705` | Highflyer com GPU alternativa fictícia, três ventoinhas e monitor incluído |
| `DEMO-PEDIDO-68475` | Gamer com memória alternativa fictícia, monitor avulso e periféricos avulsos |

Referências públicas consultadas: [Highflyer](https://www.pichau.com.br/pc-gamer-pichau-highflyer-amd-ryzen-7-9850x3d-geforce-rtx-5070-ti-16gb-32gb-ddr5-ssd-m-2-2tb) e [Gamer](https://www.pichau.com.br/pc-gamer-pichau-amd-ryzen-5-5500-radeon-rx-9050-8gb-16gb-ddr4-ssd-480gb-68475).

As descrições públicas não fornecem uma lista contratual dos modelos individuais de cada compra; os anúncios informam que marcas/modelos podem variar. A composição detalhada cadastrada é uma demonstração baseada nas famílias descritas. TODOS os SKUs de componentes prefixados com `DEMO-`, medidas, alternativas de personalização e pedidos de exemplo são fictícios. Eles não representam compras reais nem garantem opções atualmente oferecidas pelo configurador.

## Alterar e salvar

Abra **Ver peças / alterar configuração**. Substitua na própria linha ou adicione outro produto. A sugestão é atualizada automaticamente. O seletor de troca mostra produtos da mesma categoria; cadastre alternativas em **Produtos**, usando a seção opcional de categoria e embalagem no PC.

Escolha **Personalização do PC** para um adicional dentro da configuração ou **Compra avulsa** para uma linha externa do pedido. Monitores avulsos, suportes e nobreaks seguem em embalagem própria; demais avulsos compartilham outro grupo. Cadeiras, mesas e pés de mesa usam embalagem própria. Nesta interface simples, cada unidade cadastrada representa um pacote físico de fabricante; para um móvel com pacotes diferentes, cadastre cada pacote em uma linha distinta.

Memórias e SSDs podem usar **Dentro da caixa da placa-mãe**. OEM/peças instaladas sem caixa separada usam o tratamento correspondente e não adicionam objetos ao cálculo externo. O PC preparado representa o gabinete com os componentes instalados e a preparação real de transporte. Caixas acompanhantes são objetos separados; não confundir a peça instalada com a sua embalagem vazia.

Em **Salvar esta composição**, informe um código único do pedido e salve. Essa composição permanece independente da base. Leia o código salvo para recuperá-la no futuro. Duas compras do mesmo SKU comercial podem ter conteúdos e caixas diferentes.

## Importar a composição efetivamente comprada

Use **Importar pedido (.json)**. Há um arquivo completo em `examples/pedido-highflyer-personalizado.json`. O campo `items` contém a lista FINAL de peças: o aplicativo não adiciona automaticamente componentes da base que estejam ausentes no arquivo. Portanto, uma substituição não mantém a peça original por engano.

Os SKUs de componentes devem existir nos cadastros e ser únicos. O arquivo exige `code`, `configurationSku` e `items`. Cada linha inclui `sku`, `quantity`, `origin` (`CONFIG` ou `EXTRA`) e opcionalmente `mode` (`MAIN`, `NESTED`, `OEM`, `FACTORY`). Sem `mode`, é usado o padrão cadastrado no produto. Códigos já existentes são rejeitados na importação para evitar sobrescrita silenciosa.

## Limite da automação atual

A leitura consulta os cadastros locais e os pedidos importados/salvos. O aplicativo não está conectado ao carrinho, checkout ou ERP da Pichau e não descobre sozinho as alterações feitas por um cliente no site. Para automatizar compras reais, o sistema interno precisa entregar a composição final associada a um identificador de pedido. O SKU do anúncio identifica a base, sem identificar quais personalizações uma compra recebeu.

## API adicionada

- GET `/api/simple/pc/lookup?code=...`: buscar configuração/pedido por código ou alias explícito.
- POST `/api/simple/pc/calculate`: agrupar e calcular a composição final recebida.
- PUT `/api/simple/pc/{id}`: salvar composição, com `X-State-Revision`.
- POST `/api/simple/pc/import`: importar lista final por SKUs, com `X-State-Revision`.
- POST `/api/simple/pc/examples`: adicionar exemplos sem sobrescrever medidas ou registros já existentes, com `X-State-Revision`.

Os pedidos atuais suportam um PC por composição. Para dois PCs na mesma compra, use composições/códigos distintos neste piloto. Não há fila, operador ou conferência obrigatória na interface.
