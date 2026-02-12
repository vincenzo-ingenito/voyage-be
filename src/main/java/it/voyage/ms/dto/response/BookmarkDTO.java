package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO per rappresentare un segnalibro con i dettagli del viaggio associato
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkDTO {
    
    /**
     * ID del bookmark
     */
    private String bookmarkId;
    
    /**
     * ID del viaggio salvato
     */
    private String travelId;
    
    /**
     * Nome del viaggio
     */
    private String travelName;
    
    /**
     * Data inizio viaggio
     */
    private String dateFrom;
    
    /**
     * Data fine viaggio
     */
    private String dateTo;
    
    /**
     * ID del proprietario del viaggio
     */
    private String ownerId;
    
    /**
     * Nome del proprietario del viaggio
     */
    private String ownerName;
    
    /**
     * Data in cui è stato creato il bookmark
     */
    private Date bookmarkedAt;
     
}