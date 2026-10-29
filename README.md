# inBox

**Clique duas vezes em inBox.cmd.**

Ele verifica as alteracoes locais, atualiza o programa quando necessario e abre o site automaticamente. Nao ha menu ou comandos manuais.

- backend/: arquivos internos necessarios para funcionar e atualizar.
- backups/: copias de seguranca dos cadastros.
- Dockerfile e render.yaml: configuracao da hospedagem existente.

O banco fica em backend/data/. A atualizacao do programa nao substitui os cadastros. Detalhes tecnicos ficam em backend/docs/arquitetura.md.

Para se localizar nas regras e nos arquivos, consulte [o mapa do código](backend/docs/mapa-do-codigo.md).
Para validar as alterações sem tocar nos cadastros, execute `./backend/scripts/testar.ps1` na raiz do projeto.
