# Lista de peças e integração futura

Na tela Escolher caixa, use Informar peças e adicione a lista final de produtos. Informe se cada item pertence à configuração ou foi comprado avulso. Os cadastros definem a categoria e as medidas externas da embalagem. O tratamento depende da origem do item na lista.

O PC preparado representa o gabinete montado com sua proteção. Não some peças instaladas novamente: use OEM para peças sem embalagem acompanhante. Memória e SSD dentro da caixa da placa-mãe usam NESTED; a caixa da placa-mãe precisa constar na lista.

Monitores, suportes e nobreaks avulsos seguem na caixa do fabricante. Cadeiras, mesas e pés de mesa também. Outros avulsos são agrupados separadamente. Caixas externas precisam comportar os itens geometricamente com no máximo 85% do volume ocupado. A disposição é uma sugestão calculada e deve ser conferida na operação.

## Contrato para o futuro ERP

POST /api/simple/packing/preview recebe a lista final completa, sem consultar o site, sem exigir SKU base e sem gravar pedidos. Exemplo:

```json
{"items":[{"sku":"SKU-CADASTRADO","quantity":1,"origin":"CONFIG","mode":"MAIN"}]}
```

origin: CONFIG ou EXTRA. mode opcional: MAIN, OEM, NESTED ou FACTORY; se omitido, aplica as regras da categoria e da origem: memória/SSD da configuração usam NESTED; avulsos usam suas próprias embalagens; móveis e monitores/suportes/nobreaks avulsos usam FACTORY. Compras somente de avulsos não exigem gabinete. A resposta contém composition e plan, com grupos, caixas, posicionamentos 3D e pendências. SKUs desconhecidos são rejeitados. Medidas ausentes impedem a indicação do grupo afetado. O arquivo JSON pode ser importado pela tela para simular o futuro ERP. A conexão, autenticação e mapeamento real do ERP ainda dependem de sua documentação.
