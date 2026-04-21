package it.voyage.ms.dto.request;

import it.voyage.ms.enums.OptimizationScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request per l'ottimizzazione dell'itinerario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRequest {
    
    /**
     * Scope dell'ottimizzazione (SINGLE_DAY o FULL_TRAVEL)
     */
    private OptimizationScope scope;
    
    /**
     * Numero del giorno da ottimizzare (obbligatorio solo se scope=SINGLE_DAY)
     * Campo 'day' di DailyItineraryEty
     */
    private Integer dayNumber;
    
    /**
     * Se true, persiste i nuovi orderIndex sul DB
     * Se false, restituisce solo l'anteprima senza salvare
     */
    private Boolean applyChanges = false;
}