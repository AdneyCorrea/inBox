# Importação e integração futura

## Importar um pedido

Na fila, baixe **Modelo de importação**. Preencha JSON com SKUs já cadastrados. O arquivo abre para revisão e só entra no banco após **Salvar**. Limite: 500 KB.

```json
{
  "reference": "PEDIDO-123",
  "operator": "Operador",
  "note": "Conferir composição final",
  "configurations": [
    {
      "sku": "SKU-COMERCIAL-PC",
      "name": "PC personalizado",
      "preparedSku": "SKU-PC-PREPARADO",
      "lines": [
        {"sku": "SKU-PLACA-MAE", "quantity": 1, "origin": "BASE", "role": "ACCOMPANYING"},
        {"sku": "SKU-COOLER-ESCOLHIDO", "quantity": 1, "origin": "CUSTOMIZATION", "role": "ACCOMPANYING"}
      ]
    }
  ],
  "standalone": [{"sku": "SKU-FONE", "quantity": 1}]
}
```

Cada configuração representa uma unidade de PC. As linhas contêm a composição final, sem alternativas descartadas. `preparedSku` pode ficar vazio enquanto o perfil está pendente. Origens no PC: `BASE` e `CUSTOMIZATION`. Avulsos recebem `STANDALONE` e `PACKAGED` automaticamente.

Tratamentos: `PACKAGED` (produto embalado), `ACCOMPANYING` (caixa acompanhante), `NESTED` (dentro da caixa da placa-mãe), `INSTALLED` (instalado sem caixa separada). Avulsos não herdam tratamento de peça instalada.

## API local

Base `/api`. Não expor em rede sem autenticação, autorização e revisão da persistência.

| Método e caminho | Uso |
| --- | --- |
| GET `/health` | Identificação e versão |
| GET `/state` | Estado e geração de concorrência |
| PUT `/products/{id}` | Salvar produto e perfis |
| PUT `/boxes/{id}` | Salvar caixa |
| PUT `/templates/{id}` | Salvar modelo de PC |
| DELETE `/{type}/{id}` | Excluir produto não referenciado, caixa ou modelo |
| PUT `/orders/{id}` | Salvar composição e reiniciar conferência |
| GET `/orders/{id}/plan` | Calcular plano ou consultar retrato concluído |
| GET `/orders/{id}/checklist` | Unidades esperadas por linha |
| POST `/orders/{id}/check` | Conferir unidade |
| POST `/orders/{id}/complete` | Validar condições e concluir |
| POST `/orders/{id}/notes` | Registrar ocorrência |
| GET `/backup` | Exportar estado |
| POST `/restore` | Restaurar backup validado, até 5 MB |
| POST `/demo` | Exemplos somente em base vazia |

Mutações exigem `X-State-Revision` com `generation` da última leitura. Edição concorrente retorna HTTP 409. Respostas de mutação incluem o estado atualizado.

O formato de cadastro da API corresponde às classes em `Model.java`. O JSON amigável por SKU é convertido pela interface; não equivale ao objeto do PUT de pedido.

## Integração com ERP/WMS

Obter exemplo anonimizado com identificadores de linhas, vínculo das personalizações com cada PC, composição final, quantidades e identificação dos avulsos. Preservar esses vínculos; não reconstruir a compra pelo título ou URL do anúncio.

O protótipo ainda não consulta catálogo/pedidos da Pichau nem acessa sistemas internos.
