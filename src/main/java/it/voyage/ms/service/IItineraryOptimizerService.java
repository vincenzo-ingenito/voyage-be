package it.voyage.ms.service;

import it.voyage.ms.dto.request.OptimizationRequest;
import it.voyage.ms.dto.response.OptimizationResult;

/**
 * Service per l'ottimizzazione dell'itinerario
 */
public interface IItineraryOptimizerService {
    
    /**
     * Ottimizza i punti di un itinerario minimizzando la distanza percorsa
     * Usa l'algoritmo Nearest Neighbor (O(n²), ottimale per n ≤ 20)
     * 
     * @param travelId ID del viaggio
     * @param request configurazione dell'ottimizzazione
     * @param userId ID dell'utente che fa la richiesta
     * @return risultato con distanze prima/dopo e viaggio ottimizzato
     */
    OptimizationResult optimize(Long travelId, OptimizationRequest request, String userId);
}