package it.voyage.ms.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per rappresentare lo stato di un bookmark
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkCheckDTO {
    
    /**
     * Indica se il viaggio è salvato nei segnalibri
     */
    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
}
