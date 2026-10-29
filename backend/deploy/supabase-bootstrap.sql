-- Cria o schema e a tabela privados do inBox no Supabase e restringe o acesso público.
-- Execute somente no projeto exclusivo do inBox, pelo SQL Editor do Supabase.
-- Sem tabelas expostas à API pública do Supabase.
BEGIN;
CREATE SCHEMA IF NOT EXISTS inbox_private;
REVOKE ALL ON SCHEMA inbox_private FROM PUBLIC, anon, authenticated;
CREATE TABLE IF NOT EXISTS inbox_private.app_state (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    payload TEXT NOT NULL
);
REVOKE ALL ON TABLE inbox_private.app_state FROM PUBLIC, anon, authenticated;
ALTER TABLE inbox_private.app_state ENABLE ROW LEVEL SECURITY;
COMMIT;
