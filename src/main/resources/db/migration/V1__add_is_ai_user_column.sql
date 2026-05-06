-- Aggiunge colonna is_ai_user per identificare utenti AI mock
-- Utilizzati per sessioni demo e testing

ALTER TABLE users 
ADD COLUMN IF NOT EXISTS is_ai_user BOOLEAN DEFAULT false;

-- Crea indice per query ottimizzate sui suggerimenti
CREATE INDEX IF NOT EXISTS idx_users_ai_user 
ON users(is_ai_user) 
WHERE is_ai_user = true;

-- Commento sulla colonna
COMMENT ON COLUMN users.is_ai_user IS 'Identifica utenti AI generati per demo. Questi utenti hanno profili pubblici e itinerari pre-popolati.';