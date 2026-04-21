package it.voyage.ms.enums;

/**
 * Scope per l'ottimizzazione dell'itinerario
 */
public enum OptimizationScope {
    /**
     * Ottimizza solo un singolo giorno
     */
    SINGLE_DAY,
    
    /**
     * Ottimizza l'intero viaggio (tutti i giorni indipendentemente)
     */
    FULL_TRAVEL
}