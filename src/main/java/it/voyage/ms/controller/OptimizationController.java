package it.voyage.ms.controller;

import it.voyage.ms.dto.request.OptimizationRequest;
import it.voyage.ms.dto.response.OptimizationResult;
import it.voyage.ms.service.IItineraryOptimizerService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per l'ottimizzazione dell'itinerario
 */
@RestController
@RequestMapping("/api/optimize")
@AllArgsConstructor
@Slf4j
public class OptimizationController {

    private final IItineraryOptimizerService optimizerService;

    /**
     * Endpoint per ottimizzare l'itinerario di un viaggio
     * 
     * @param travelId ID del viaggio
     * @param request configurazione dell'ottimizzazione (scope, dayNumber, applyChanges)
     * @param authentication autenticazione Spring Security
     * @return 200 con risultato ottimizzazione (distanze before/after, viaggio)
     *         400 se scope=SINGLE_DAY senza dayNumber
     *         403 se l'utente non può modificare il viaggio
     *         404 se viaggio o giorno non trovati
     */
    @PostMapping("/{travelId}")
    public ResponseEntity<OptimizationResult> optimizeItinerary(
            @PathVariable Long travelId,
            @RequestBody OptimizationRequest request,
            Authentication authentication) {

        log.info("POST /api/optimize/{} - scope={}, dayNumber={}, applyChanges={}", 
            travelId, request.getScope(), request.getDayNumber(), request.getApplyChanges());

        String userId = authentication.getName();
        
        OptimizationResult result = optimizerService.optimize(travelId, request, userId);

        return ResponseEntity.ok(result);
    }
}