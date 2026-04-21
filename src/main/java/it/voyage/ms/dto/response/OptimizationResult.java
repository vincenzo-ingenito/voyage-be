package it.voyage.ms.dto.response;

import it.voyage.ms.enums.OptimizationScope;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Risultato dell'ottimizzazione dell'itinerario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationResult {
    
    /**
     * Viaggio con punti ottimizzati
     */
    private TravelDTO travel;
    
    /**
     * Scope utilizzato per l'ottimizzazione
     */
    private OptimizationScope scope;
    
    /**
     * Numero del giorno ottimizzato (valorizzato solo se scope=SINGLE_DAY)
     */
    private Integer dayOptimized;
    
    /**
     * Distanza totale prima dell'ottimizzazione (in km)
     */
    private Double totalDistanceBefore;
    
    /**
     * Distanza totale dopo l'ottimizzazione (in km)
     */
    private Double totalDistanceAfter;
    
    /**
     * Indica se le modifiche sono state applicate al DB
     */
    private Boolean applied;
}