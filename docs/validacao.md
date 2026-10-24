# Validação do protótipo

## Testes automáticos

26 testes de regras e integração Java e 6 testes da lógica da interface passaram (32 no total). Cobrem:

- Limite de ocupação de 85%, aceitando o valor exato e rejeitando excesso.
- Geometria, rotações, ausência de sobreposição, contenção e empilhamento.
- Capacidade de carga, tara e escolha da menor caixa em que a heurística encontra encaixe.
- Monitor incluído versus avulso, móveis, pacotes múltiplos e agrupamento interno.
- Ausência de caixa da placa-mãe de destino e medidas pendentes.
- Obrigatoriedade de origem da medição e IDs independentes dos PCs.
- Concorrência de gravação, SKU duplicado e exclusão de produto referenciado.
- Leitura de código, rejeição de divergência e limite da quantidade conferida.
- Bloqueio de dados fictícios e de conclusão antes da conferência completa.
- Reinício das conferências após alteração de cadastro.
- Conclusão com dados de teste validados e preservação do plano histórico.
- Restauração de backup, rejeição de arquivo incompleto e preservação da base em caso de erro.
- Abertura de pedido, cópias independentes de PCs, campos vazios e roteamento da conferência para linhas repetidas do mesmo SKU.

Os dados validados nos testes são construídos exclusivamente dentro do banco em memória dos testes. Não são medições reais e não são inseridos como validados na base de demonstração.

Execução Java: `mvn -f backend/pom.xml test`. Interface: `node --test tests/frontend.test.cjs`. Node é necessário somente para esses testes, não para usar a aplicação.

## Verificação no navegador

Conferir demonstração, separação de grupos, visualização da disposição, leitura de código digitado, abertura/edição de formulários, registro de ocorrência e persistência após reiniciar. Testes não substituem validação física com produtos reais nem teste de um leitor de código de barras.

## Limitações de implantação

Serviço local sem autenticação; banco H2 por documento. Integração empresarial, persistência relacional PostgreSQL e múltiplos postos são próximos passos, não funcionalidades liberadas para produção.
