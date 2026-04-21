package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response per le raccomandazioni di luoghi vicini
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NearbyRecommendationResponse {
    
    /**
     * Lista di luoghi vicini (ordinata per distanza crescente)
     */
    private List<NearbyPlaceDTO> places;
    
    /**
     * Nome del punto di riferimento usato come centro
     */
    private String referencePointName;
    
    /**
     * Latitudine del punto di riferimento
     */
    private Double referenceLatitude;
    
    /**
     * Longitudine del punto di riferimento
     */
    private Double referenceLongitude;
    
    /**
     * Raggio effettivo utilizzato nella ricerca (in metri)
     */
    private Integer radiusUsed;
    
    /**
     * Numero totale di risultati trovati dopo il filtraggio
     */
    private Integer totalFound;
}