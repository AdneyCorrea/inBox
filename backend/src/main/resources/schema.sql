-- Cria a tabela local que armazena o estado da aplicação em JSON.
CREATE TABLE IF NOT EXISTS app_state (id INTEGER PRIMARY KEY, payload CLOB NOT NULL);
