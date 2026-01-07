package it.voyage.ms.repository.impl;

import it.voyage.ms.repository.entity.BookmarkEty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository per la gestione dei segnalibri (bookmarks)
 */
@Repository
public interface BookmarkRepository extends MongoRepository<BookmarkEty, String> {
    
    /**
     * Trova tutti i segnalibri di un utente
     * @param userId ID dell'utente
     * @return Lista di segnalibri
     */
    List<BookmarkEty> findByUserId(String userId);
    
    /**
     * Trova un segnalibro specifico per utente e viaggio
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     * @return Optional con il bookmark se esiste
     */
    Optional<BookmarkEty> findByUserIdAndTravelId(String userId, String travelId);
    
    /**
     * Verifica se esiste un bookmark per un utente e un viaggio
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     * @return true se esiste, false altrimenti
     */
    boolean existsByUserIdAndTravelId(String userId, String travelId);
    
    /**
     * Elimina un bookmark specifico
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     */
    void deleteByUserIdAndTravelId(String userId, String travelId);
    
    /**
     * Elimina tutti i bookmark di un viaggio (quando il viaggio viene eliminato)
     * @param travelId ID del viaggio
     */
    void deleteByTravelId(String travelId);
    
    /**
     * Conta quanti bookmark ha ricevuto un viaggio
     * @param travelId ID del viaggio
     * @return Numero di bookmark
     */
    long countByTravelId(String travelId);
}