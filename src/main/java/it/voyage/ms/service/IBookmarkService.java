package it.voyage.ms.service;

import it.voyage.ms.dto.response.BookmarkDTO;

import java.util.List;

/**
 * Interfaccia del servizio per la gestione dei segnalibri
 */
public interface IBookmarkService {
    
    /**
     * Aggiunge un segnalibro per un viaggio
     * @param userId ID dell'utente che crea il segnalibro
     * @param travelId ID del viaggio da salvare
     * @return Il bookmark creato
     */
    BookmarkDTO addBookmark(String userId, Long travelId);
    
    /**
     * Rimuove un segnalibro
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     */
    void removeBookmark(String userId, Long travelId);
    
    /**
     * Verifica se un viaggio è salvato nei segnalibri dell'utente
     * @param userId ID dell'utente
     * @param travelId ID del viaggio
     * @return true se è salvato, false altrimenti
     */
    boolean isBookmarked(String userId, Long travelId);
    
    /**
     * Ottiene tutti i segnalibri di un utente con i dettagli dei viaggi
     * @param userId ID dell'utente
     * @return Lista di bookmark con dettagli
     */
    List<BookmarkDTO> getUserBookmarks(String userId);
}