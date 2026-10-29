# Publicação gratuita: Render + Supabase

O perfil `cloud` executa o servidor com login obrigatório e usa PostgreSQL em schema privado. O perfil padrão continua disponível apenas para desenvolvimento local. Docker define `cloud` automaticamente. A hospedagem só está concluída depois que o serviço HTTPS responder e a importação dos cadastros for conferida.

## Banco exclusivo no Supabase

1. Crie `inBox` em uma organização Free. Não altere projetos de outros aplicativos.
2. Desative a Data API e a exposição automática de tabelas: este aplicativo acessa PostgreSQL pelo servidor Java.
3. No SQL Editor, execute `backend/deploy/supabase-bootstrap.sql`. Ele cria somente `inbox_private.app_state`, com RLS e sem permissões para os papéis públicos.
4. Em Connect, copie host e usuário de **Session pooler**, porta 5432. O pooler permite conexão IPv4. Não use a chave `anon` nem `service_role` como senha do PostgreSQL.
5. Baixe o certificado CA nas configurações de SSL do banco. No Render ele será um **Secret File** chamado `supabase-ca.crt`.

## Login do aplicativo

A primeira versão online usa um acesso de equipe, independente do login do painel Supabase. Não permite cadastro público nem recuperação de senha por e-mail. O administrador altera o hash de senha no Render. As sessões expiram após 30 minutos sem atividade e são encerradas em reinicializações do serviço. Contas individuais podem ser acrescentadas posteriormente.

A geracao de hash para o login hospedado e uma operacao de administracao, disponivel na classe PasswordHash do backend. Ela nao faz parte da abertura local automatica. Nunca coloque a senha ou o hash no repositorio. APP_LOGIN_USERNAME define o usuario.

## Código e serviço

1. Use um repositório **privado** no GitHub para o código.
2. Conecte o Render somente a esse repositório. Caso a tela de autorização peça acesso adicional a outros repositórios, restrinja a seleção.
3. Crie um Web Service com Docker e selecione **Free**. O arquivo `render.yaml` também descreve o serviço.
4. Configure as variáveis listadas em `backend/deploy/.env.example`, sem armazenar os valores reais no código.
5. Adicione o Secret File do certificado antes do primeiro deploy.
6. Use `/api/health/ready` como health check. Essa rota verifica a tabela do banco; responde sem revelar dados ou credenciais.

Exemplo de URL JDBC (sem senha na URL):

```text
jdbc:postgresql://SEU-HOST.pooler.supabase.com:5432/postgres?sslmode=verify-full&sslrootcert=/etc/secrets/supabase-ca.crt
```

O container executa com usuário sem privilégios e memória ajustada para o plano gratuito. Não grava cadastros no disco temporário do Render. A conexão usa até três conexões com o banco. Não habilite modo de desenvolvimento na hospedagem.

## Migração e conferência

Uma cópia dos 10 produtos e 10 caixas atuais foi exportada para `backend/deploy/private/dados-para-migracao.json`, que é ignorado pelo Git e pelo Docker. Depois de entrar no site publicado, use **Restaurar backup** para importar esse arquivo. A primeira importação deve ser feita com o banco online vazio. Não restaure novamente se já houver alterações de outros usuários sem antes exportar os dados online.

Confira no endereço HTTPS, em celular e computador: login, departamentos, busca, cadastro, cálculo, troca de volumes, 3D, exportação e saída. A tela deve mostrar dados compartilhados. Alterações concorrentes são recusadas com 409 para evitar sobrescrever a edição de outro acesso.

## Limites e estado de validação

Os planos gratuitos podem suspender o serviço por inatividade e têm franquias de uso. O Render gratuito pode levar cerca de um minuto para reabrir após suspensão; o Supabase Free pode pausar por inatividade. Use para testes e validação inicial. A disponibilidade e as quotas devem ser conferidas nos painéis.

Os testes locais validam login, CSRF, restrição de acesso, conflitos e regras existentes. Eles usam H2. A conexão PostgreSQL real, a imagem Docker e o endereço público exigem validação no ambiente hospedado; não são confirmados apenas pelos testes locais.

Documentação oficial consultada:
- https://render.com/docs/docker
- https://render.com/docs/free
- https://render.com/docs/blueprint-spec
- https://supabase.com/docs/guides/database/connecting-to-postgres
- https://supabase.com/docs/guides/platform/ssl-enforcement
- https://supabase.com/pricing
