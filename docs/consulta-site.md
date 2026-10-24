# Consultar o catálogo da Pichau

A interface principal consulta SKU ou link público da Pichau e cruza a identificação com as medidas locais das embalagens. A criação/importação de pedidos foi retirada dessa interface. Os dados anteriores foram preservados no banco e no backup.

O fluxo é: consultar o site, identificar o SKU e a descrição, localizar o vínculo local das embalagens, verificar medidas e sugerir a caixa com 15% de espaço livre e modelo 3D.

Os dois PCs de referência possuem vínculos de demonstração já cadastrados. Todas as medidas desses exemplos e os SKUs locais de componentes são fictícios. O texto público identifica famílias de componentes e pode não identificar os modelos individuais; esse vínculo genérico não é uma composição real validada para expedição.

Para outro produto, o link direto é a entrada mais confiável. A pesquisa por SKU depende dos links identificáveis na página pública de busca. Quando essa pesquisa não identifica um produto único, o aplicativo pede o link direto. Produtos individuais podem usar o SKU do cadastro de medidas; PCs precisam do vínculo de todas as embalagens acompanhantes.

Se o nome/SKU não estiver disponível, se a descrição de um PC conhecido mudar, ou se faltar vínculo/medida, o aplicativo informa a pendência. A identificação por família não garante o modelo exato das peças quando o próprio anúncio permite variação.

Cada consulta é nova, sem reaproveitar pedidos locais como se fossem uma consulta ao site. Erros de manutenção, bloqueio ou indisponibilidade são exibidos. Na verificação de 24/10/2026, uma requisição direta retornou manutenção HTTP 503; a requisição real feita pelo aplicativo Java foi bloqueada com HTTP 403. Portanto, a consulta ao vivo não está validada nem liberada como integração funcional. Para torná-la confiável, é necessário um endpoint/API/feed de catálogo autorizado pela empresa. Os testes automáticos usam páginas de teste e simulações HTTP; não confirmam disponibilidade contínua do site.

O site público informa a configuração anunciada. Trocas/adicionais de uma compra específica continuam dependendo da composição final do sistema interno da empresa.

API: POST `/api/simple/site/lookup`, corpo `{"query":"SKU ou URL"}`. O destino é restrito a HTTPS em `www.pichau.com.br`, inclusive redirecionamentos. O aplicativo não acessa contas, carrinho ou pedidos privados e não contorna verificações de acesso.
