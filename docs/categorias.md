# Departamentos e categorias

O catálogo inclui os departamentos e tópicos visíveis nas imagens fornecidas pelo usuário. "Ver todos" é uma ação de navegação, não uma categoria de produto. Submenus não abertos nas imagens não foram inventados. Nobreak e Pé de Mesa foram mantidos para as regras operacionais já definidas; Outros atende itens sem correspondência.

A visualização tem dois níveis recolhíveis: departamento e categoria. Categorias vazias aparecem e permitem cadastrar diretamente nelas. A busca considera nome, SKU, categoria e departamento.

No formulário, informe o nome, confira a sugestão por palavras-chave ou selecione departamento e categoria manualmente. A sugestão é local, baseada em regras, não consulta a internet. Nomes não reconhecidos exigem escolha manual. Uma escolha manual nunca é sobrescrita enquanto o formulário estiver aberto. Produtos existentes mantêm sua categoria ao editar o nome.

O campo topic guarda a categoria de catálogo. O backend valida esse identificador e associa as regras de embalagem já existentes. Regras de monitores, suportes, nobreaks, cadeiras, mesas, pés de mesa, memória e SSD são preservadas. Categorias novas sem regra especial usam embalagem normal. Não foram adicionados produtos nem medidas por causa da nova árvore.

Fontes da árvore: static/catalog-categories.json (servidor) e catalog-categories.js (navegador), verificadas por teste de igualdade. Regras de sugestão: category-suggestions.js.

## Ampliação das palavras-chave — 24/10/2026

Consulta pública por amostragem, não coleta integral do catálogo. Foram lidas páginas de Hardware, Periféricos, Kits, Redes, Monitores, Eletrônicos, Casa Inteligente, Casa e Lazer, Vestuário, Esporte e Pets. Trinta e sete títulos públicos foram registrados em tests/pichau-product-samples.json com sua fonte; as categorias esperadas são o enquadramento operacional do aplicativo. Certos produtos aparecem em mais de uma categoria no site, portanto não existe garantia de reprodução exata de todos os vínculos da loja.

Dicionário de 94 categorias específicas em catalog-keywords.json e seu equivalente carregado no navegador. Outros é uma categoria manual. O motor combina expressões de tipo de produto, sinônimos, famílias de modelos, termos de exclusão e posição no nome. Kits e acessórios recebem regras próprias. Empates próximos não recebem atribuição automática. Marcas isoladas não determinam uma categoria. Nenhum modelo de linguagem externo é chamado. A atualização desse dicionário é local, não uma sincronização contínua com o site.

Fontes de consulta:
- https://www.pichau.com.br/hardware
- https://www.pichau.com.br/perifericos
- https://www.pichau.com.br/perifericos/kit
- https://www.pichau.com.br/perifericos/kit-teclado-e-mouse
- https://www.pichau.com.br/redes-wireless
- https://www.pichau.com.br/monitores
- https://www.pichau.com.br/eletronicos
- https://www.pichau.com.br/casa-inteligente
- https://www.pichau.com.br/casa-e-lazer
- https://www.pichau.com.br/vestuario
- https://www.pichau.com.br/esporte
- https://www.pichau.com.br/pets

## Busca na embalagem

O seletor usa os mesmos departamentos e categorias do cadastro. A pesquisa ordena correspondências exatas de SKU primeiro, aceita códigos sem separadores e termos de categoria. Pequenos erros de uma letra são aceitos em palavras com pelo menos cinco letras; números e códigos não recebem correção aproximada. Resultados aproximados são identificados. Filtros opcionais limitam a busca por departamento e categoria. A escolha do produto é explícita por clique ou setas e Enter, seguida da quantidade e origem. Limpar a busca não remove os itens já adicionados.
