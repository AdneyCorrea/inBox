# Guia de uso do protótipo

## Experimentar sem medidas reais

Em uma base vazia, use **Explorar demonstração**. O pedido DEMO-1001 contém PC, monitor incluído, periféricos, cadeira, ventoinhas adicionais, periféricos avulsos, monitor avulso e mesa com dois pacotes. O DEMO-1002 demonstra a falta de medidas.

O primeiro exemplo sugere duas caixas externas e lista quatro pacotes de fabricante, totalizando seis volumes apenas na simulação. A conclusão fica bloqueada porque os dados são fictícios.

## Cadastrar antes da medição

Em **Produtos**, registre SKU, nome, categoria e tratamento. Deixe dimensões e pesos vazios e mantenha a situação **Pendente**. Não preencha zeros para indicar ausência de informação.

O perfil de **PC preparado para transporte** representa o conjunto físico que entra na caixa principal, incluindo o peso das peças instaladas. Confirme sua correspondência com a composição final. Para alterações relevantes, crie outro perfil ou revise a medição. Não há reaproveitamento automático de planos entre composições.

Caixas acompanhantes usam suas medidas externas e o peso do conteúdo realmente enviado. Se contêm memória/SSD, inclua esse conteúdo no peso medido. Não conte novamente a peça instalada, já incluída no peso do PC preparado.

Um produto com dois pacotes diferentes precisa de dois perfis. O campo de quantidade por perfil serve para pacotes idênticos. Produtos OEM instalados sem caixa podem ficar sem perfis.

Em **Caixas**, registre códigos e disponibilidade; depois complete medidas internas/externas, tara, capacidade, responsável e origem da medição.

## Configurações e personalização

Crie modelos em **Configurações**. Selecione o perfil do PC preparado e adicione as linhas, conferindo origem base/personalização e tratamento físico.

Ao criar um pedido, use o modelo para cada unidade de PC. As cópias são independentes. Para substituir uma peça, troque o produto na linha existente, removendo a alternativa original. Para acréscimos, adicione uma linha de personalização. Avulsos ficam fora das configurações.

Categorias de embalagem própria têm prioridade sobre o tratamento da linha. **Dentro da caixa da placa-mãe** exige uma linha de placa-mãe com tratamento **Caixa acompanhante** no mesmo PC. O sistema verifica a existência do recipiente; a capacidade interna exige conferência física.

## Conferência e conclusão

Informe o operador ao salvar o pedido. No campo de leitura, use SKU ou código de barras cadastrado e pressione Enter. Leitores que funcionam como teclado podem utilizar esse campo; foram testados códigos digitados, sem hardware de leitura.

Cada leitura confirma uma unidade; há confirmação manual por linha. Quando o mesmo código existe em várias linhas, a leitura preenche a primeira ainda incompleta. Para escolher uma linha específica, use sua ação manual.

O checklist inclui PC preparado e linhas da composição, inclusive peças instaladas e itens internos, para conferência visual da composição final.

Concluir exige medidas reais validadas, disposição encontrada, conferência completa, operador e confirmação da validação física. O plano concluído preserva os dados daquele momento. Alterações nos cadastros reiniciam as conferências dos pedidos abertos.

Registre troca de caixa ou falha em **Ocorrências e ajustes**. Esse registro não ignora incompatibilidades. Corrija cadastro, preparação ou composição e simule novamente.

## Backup e base limpa

Exporte backups regularmente. Para começar sem exemplos, exporte a base atual e restaure `examples/base-vazia.json`. A interface solicita confirmação antes de substituir os dados. Não limpe dados reais sem uma cópia segura.
