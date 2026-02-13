package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO per la risposta dell'autocompletamento dei luoghi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlaceAutocompleteDTO {
    
    /**
     * Lista di suggerimenti di luoghi
     */
    private List<PlaceSuggestion> predictions;
    
    /**
     * Status della risposta da Google Places API
     */
    private String status;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlaceSuggestion {
        /**
         * Testo principale del suggerimento
         */
        private String mainText;
        
        /**
         * Descrizione completa del luogo
         */
        private String description;
        
        /**
         * ID univoco del luogo
         */
        private String placeId;
    }
}