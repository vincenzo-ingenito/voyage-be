-- Creazione tabella per gestire i lock sui viaggi di gruppo
-- Previene modifiche concorrenti quando più editor lavorano sullo stesso viaggio

CREATE TABLE IF NOT EXISTS travel_locks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    travel_id BIGINT NOT NULL,
    locked_by_user_id VARCHAR(255) NOT NULL,
    locked_by_user_name VARCHAR(255),
    locked_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    last_heartbeat_at TIMESTAMP NOT NULL,
    
    -- Indici per performance
    INDEX idx_travel_lock_travel_id (travel_id),
    INDEX idx_travel_lock_expires_at (expires_at),
    
    -- Un viaggio può avere un solo lock attivo alla volta
    UNIQUE KEY uk_travel_lock (travel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Commenti sulla tabella
ALTER TABLE travel_locks COMMENT = 'Gestisce i lock per prevenire modifiche concorrenti sui viaggi di gruppo';