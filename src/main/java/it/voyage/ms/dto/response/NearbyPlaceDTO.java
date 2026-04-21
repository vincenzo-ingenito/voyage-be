package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per un luogo vicino raccomandato dall'API Google Places Nearby
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyPlaceDTO {
    
    /**
     * Google Place ID univoco
     */
    private String placeId;
    
    /**
     * Nome del luogo
     */
    private String name;
    
    /**
     * Indirizzo formattato
     */
    private String address;
    
    /**
     * Latitudine
     */
    private Double latitude;
    
    /**
     * Longitudine
     */
    private Double longitude;
    
    /**
     * Tipo di luogo (mappato in italiano)
     */
    private String type;
    
    /**
     * Reference per recuperare foto (nullable)
     */
    private String photoReference;
    
    /**
     * Distanza in metri dal punto di riferimento (calcolata con Haversine)
     */
    private Integer distanceMeters;
    
    /**
     * Indica se già presente nel viaggio (sempre false nei risultati filtrati)
     */
    private Boolean alreadyInTravel;
}