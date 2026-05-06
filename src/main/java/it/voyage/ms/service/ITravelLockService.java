package it.voyage.ms.service;

import it.voyage.ms.dto.response.TravelLockDTO;

/**
 * Servizio per gestire i lock sui viaggi di gruppo
 */
public interface ITravelLockService {

    /**
     * Tenta di acquisire un lock su un viaggio
     * @param travelId ID del viaggio
     * @param userId ID dell'utente che richiede il lock
     * @param userName Nome dell'utente (per visualizzazione)
     * @return DTO con lo stato del lock
     * @throws RuntimeException se il lock non può essere acquisito
     */
    TravelLockDTO acquireLock(Long travelId, String userId, String userName);

    /**
     * Rilascia il lock su un viaggio
     * @param travelId ID del viaggio
     * @param userId ID dell'utente che rilascia il lock
     */
    void releaseLock(Long travelId, String userId);

    /**
     * Aggiorna il heartbeat per mantenere il lock attivo
     * @param travelId ID del viaggio
     * @param userId ID dell'utente
     * @return DTO con lo stato aggiornato del lock
     */
    TravelLockDTO heartbeat(Long travelId, String userId);

    /**
     * Ottiene lo stato del lock per un viaggio
     * @param travelId ID del viaggio
     * @param currentUserId ID dell'utente corrente (per verificare ownership)
     * @return DTO con lo stato del lock
     */
    TravelLockDTO getLockStatus(Long travelId, String currentUserId);

    /**
     * Verifica se un utente può modificare un viaggio (ha il lock o non c'è lock)
     * @param travelId ID del viaggio
     * @param userId ID dell'utente
     * @return true se può modificare
     */
    boolean canEdit(Long travelId, String userId);

    /**
     * Pulizia periodica dei lock scaduti
     */
    void cleanupExpiredLocks();
}