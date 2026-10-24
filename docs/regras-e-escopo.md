# Regras e escopo inicial

## Objetivo

Gerar um plano de embalagem por pedido: conteúdo de cada grupo, caixas sugeridas, volumes de fabricante e pendências. O primeiro escopo cobre PCs de configurações prontas do site, suas personalizações e itens avulsos que acompanham o pedido no setor da montagem.

## Fonte da composição

Usar a composição final comprada, incluindo quantidades, substituições e acréscimos. A peça substituída não participa do cálculo. O SKU comercial do computador e o título do anúncio não bastam para reconstruir a compra.

Cada configuração comprada recebe um identificador próprio dentro do pedido. Dois PCs do mesmo SKU podem ter composições diferentes e precisam de grupos independentes.

Cada linha deve preservar SKU, quantidade, origem e vínculo com a configuração. O mesmo SKU comprado dentro de um PC e avulso exige linhas distintas.

## Separação de grupos

| Situação | Destino |
| --- | --- |
| PC e embalagens/acessórios acompanhantes | Grupo principal daquele PC |
| Monitor e periféricos incluídos na configuração | Grupo principal daquele PC |
| Componentes trocados ou acrescentados no configurador | Integram a composição final daquele PC |
| Monitor, suporte de monitor e nobreak avulsos | Embalagem própria do fabricante |
| Cadeira, mesa e pé de mesa, incluídos ou avulsos | Embalagem própria do fabricante |
| Demais itens avulsos do pedido | Grupo compartilhado de avulsos, separado dos PCs |

Um grupo de embalagem não equivale automaticamente a um volume físico. O grupo principal e o grupo de avulsos têm como objetivo uma caixa cada. Se não houver caixa compatível disponível, registrar exceção para revisão; não dividir automaticamente sem regra operacional aprovada.

Produtos enviados na embalagem própria podem ter mais de um volume de fábrica. Cadastrar cada pacote e sua quantidade; não assumir que uma unidade de produto sempre equivale a um volume.

## Peças instaladas e embalagens acompanhantes

A peça instalada não entra novamente como objeto solto. A caixa que acompanha essa peça pode ocupar espaço no grupo principal. Seu peso deve refletir o conteúdo efetivamente enviado, sem contar o peso da peça instalada duas vezes.

OEM sem caixa, caixas de memória e caixas de SSD/M.2 não geram objetos externos separados nos casos em que são acomodados na caixa da placa-mãe, conforme o procedimento informado. Manter esses itens na composição e registrar o recipiente de destino. Se ele não estiver disponível ou o agrupamento não for aplicável, sinalizar revisão.

O PC preparado para transporte é um objeto próprio, medido no estado em que entra na caixa principal. Não deduzir suas medidas a partir das medidas dos componentes.

## Reserva de plástico-bolha

Para caixas externas selecionadas pelo sistema:

- Reserva mínima: 15% do volume interno.
- Ocupação máxima dos objetos: 85% do volume interno.
- Não exigir folga mínima nas laterais nem camada uniforme de bolha.
- A reserva pode estar distribuída entre os objetos.
- Não converter essa reserva em metros ou peso de plástico-bolha.
- Não aplicar a reserva aos volumes expedidos diretamente na embalagem do fabricante.

Fórmulas, com todas as medidas na mesma unidade:

    volume_objetos = soma(comprimento × largura × altura × quantidade)
    ocupacao = volume_objetos / volume_interno_caixa
    volume_interno_minimo = volume_objetos / 0,85

Para objetos acomodados dentro de outro recipiente, contar apenas o envelope externo do recipiente no cálculo de ocupação. Não somar novamente seus conteúdos.

A ocupação de até 85% é necessária, mas não prova encaixe. Também validar posições sem sobreposição, contenção nas medidas internas, orientações permitidas e capacidade de peso.

Usar medidas internas para selecionar a caixa e externas para identificar o volume expedido. Somar o peso da própria caixa ao peso expedido.

## Dados pendentes

Medidas e pesos desconhecidos ficam nulos, nunca zero ou estimativas silenciosas. Perfis de demonstração e pendentes não podem produzir plano aprovado para uso operacional.

É permitido listar grupos e pacotes próprios cadastrados antes de medir. Não apresentar a quantidade total de volumes como definitiva quando houver pendências que possam alterá-la.

## Reaproveitamento de planos

Reutilizar um plano somente para a mesma composição final, quantidades, regras e revisões das embalagens. Alterações nesses dados invalidam o reaproveitamento automático.

## Fluxo de telas proposto

1. Cadastros: produtos, perfis de embalagem, caixas e pendências de medição.
2. Pedido: configurações compradas, composição final e itens avulsos.
3. Preparação: agrupamento automático e conferência das embalagens físicas.
4. Simulação: caixa candidata, ocupação, disposição e motivos de bloqueio.
5. Operação: confirmação do resultado e registro de troca de caixa ou falha de encaixe.

## Decisões ainda necessárias

- Fonte interna da composição final e formato do vínculo entre linhas e PCs.
- Procedimento quando o grupo principal ou os avulsos não couberem em uma caixa disponível.
- Rotações e empilhamento permitidos para cada embalagem.
- Forma de preparar e medir o PC montado para transporte.
- Registro de estoque de caixas e política de escolha entre alternativas compatíveis.
