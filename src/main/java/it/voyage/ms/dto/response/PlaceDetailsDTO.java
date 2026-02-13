package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO per i dettagli di un luogo specifico
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceDetailsDTO {
    
    /**
     * Coordinate geografiche del luogo
     */
    private Coordinates coord;
    
    /**
     * Tipo di luogo mappato (es. 'Attrazione', 'Hotel')
     */
    private String type;
    
    /**
     * Paese
     */
    private String country;
    
    /**
     * Regione/Provincia
     */
    private String region;
    
    /**
     * Città
     */
    private String city;
    
    /**
     * Tipi originali da Google Places
     */
    private List<String> googleTypes;
    
    /**
     * Status della risposta
     */
    private String status;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Coordinates {
        private double lat;
        private double lng;
    }
}