package it.voyage.ms.dto.response;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per la risposta paginata del feed
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedPageDTO {
    
    /**
     * Lista dei viaggi nella pagina corrente
     */
    private List<TravelDTO> travels;
    
    /**
     * Cursor per la prossima pagina (timestamp + travelId)
     * Formato: "timestamp_travelId" es: "2024-04-20T10:30:00_123"
     */
    private String nextCursor;
    
    /**
     * Indica se ci sono più risultati disponibili
     */
    private boolean hasMore;
    
    /**
     * Numero totale di viaggi (opzionale, può essere costoso da calcolare)
     */
    private Integer totalCount;
    
    /**
     * Numero di elementi in questa pagina
     */
    private int pageSize;
}