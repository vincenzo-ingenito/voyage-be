package it.voyage.ms.controller;

import it.voyage.ms.dto.response.NearbyRecommendationResponse;
import it.voyage.ms.service.IRecommendationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per le raccomandazioni di luoghi vicini
 */
@RestController
@RequestMapping("/api/recommendations")
@AllArgsConstructor
@Slf4j
public class RecommendationController {

    private final IRecommendationService recommendationService;

    /**
     * Endpoint per ottenere raccomandazioni di luoghi vicini a un punto dell'itinerario
     * 
     * @param travelId ID del viaggio
     * @param dayNumber numero del giorno (campo 'day' di DailyItineraryEty)
     * @param pointId ID del punto di riferimento
     * @param radius raggio di ricerca in metri (opzionale, default 1500, max 5000)
     * @param type tipo di luogo Google Places (opzionale, default tourist_attraction)
     * @param authentication autenticazione Spring Security
     * @return 200 con lista di luoghi raccomandati ordinati per distanza
     *         400 se il punto non ha coordinate
     *         403 se l'utente non può visualizzare il viaggio
     *         404 se viaggio, giorno o punto non trovati
     */
    @GetMapping("/nearby")
    public ResponseEntity<NearbyRecommendationResponse> getNearbyRecommendations(
            @RequestParam Long travelId,
            @RequestParam Integer dayNumber,
            @RequestParam Long pointId,
            @RequestParam(required = false) Integer radius,
            @RequestParam(required = false, defaultValue = "tourist_attraction") String type,
            Authentication authentication) {

        log.info("GET /api/recommendations/nearby - travelId={}, dayNumber={}, pointId={}, radius={}, type={}", 
            travelId, dayNumber, pointId, radius, type);

        String userId = authentication.getName();
        
        NearbyRecommendationResponse response = recommendationService.getNearbyRecommendations(
            travelId, dayNumber, pointId, radius, type, userId
        );

        return ResponseEntity.ok(response);
    }
}
