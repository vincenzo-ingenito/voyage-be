package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.repository.entity.BookmarkEty;

/**
 * Repository per la gestione dei segnalibri (bookmarks)
 */
@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkEty, Long> {

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
    Optional<BookmarkEty> findByUserIdAndTravelId(String userId, Long travelId);

    /**
     * Verifica se esiste un bookmark per un utente e un viaggio
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     * @return true se esiste, false altrimenti
     */
    boolean existsByUserIdAndTravelId(String userId, Long travelId);

    /**
     * Elimina un bookmark specifico
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     */
    @Transactional
    @Modifying
    void deleteByUserIdAndTravelId(String userId, Long travelId);

    /**
     * Elimina tutti i bookmark di un viaggio (quando il viaggio viene eliminato)
     * @param travelId ID del viaggio
     */
    @Transactional
    @Modifying
    void deleteByTravelId(Long travelId);

    /**
     * Conta quanti bookmark ha ricevuto un viaggio
     * @param travelId ID del viaggio
     * @return Numero di bookmark
     */
    long countByTravelId(Long travelId);
}