package it.voyage.ms.service;

import it.voyage.ms.dto.response.NearbyRecommendationResponse;

/**
 * Service per le raccomandazioni di luoghi vicini
 */
public interface IRecommendationService {
    
    /**
     * Ottiene raccomandazioni di luoghi vicini a un punto dell'itinerario
     * 
     * @param travelId ID del viaggio
     * @param dayNumber numero del giorno
     * @param pointId ID del punto di riferimento
     * @param radius raggio di ricerca in metri (default 1500, max 5000)
     * @param type tipo di luogo Google Places (default tourist_attraction)
     * @param userId ID dell'utente che fa la richiesta
     * @return risposta con luoghi vicini ordinati per distanza
     */
    NearbyRecommendationResponse getNearbyRecommendations(
        Long travelId, 
        Integer dayNumber, 
        Long pointId, 
        Integer radius, 
        String type, 
        String userId
    );
}