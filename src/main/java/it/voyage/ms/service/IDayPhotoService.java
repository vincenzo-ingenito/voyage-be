package it.voyage.ms.service;

import org.springframework.web.multipart.MultipartFile;

public interface IDayPhotoService {
    
    /**
     * Aggiunge o sostituisce la foto ricordo per un giorno specifico
     * @param travelId ID del viaggio
     * @param dayNumber Numero del giorno (1, 2, 3...)
     * @param file File immagine da caricare
     * @param userId ID dell'utente proprietario del viaggio
     * @return URL della foto caricata
     */
    String addOrReplacePhotoToDay(Long travelId, Integer dayNumber, MultipartFile file, String userId) throws Exception;
    
    /**
     * Rimuove la foto ricordo da un giorno specifico
     * @param travelId ID del viaggio
     * @param dayNumber Numero del giorno
     * @param userId ID dell'utente proprietario del viaggio
     */
    void removePhotoFromDay(Long travelId, Integer dayNumber, String userId) throws Exception;
}