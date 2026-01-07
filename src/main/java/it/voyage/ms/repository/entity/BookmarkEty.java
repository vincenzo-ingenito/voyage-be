package it.voyage.ms.repository.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * Entità che rappresenta un segnalibro (bookmark) di un viaggio
 * Permette agli utenti di salvare i viaggi di altri utenti
 */
@Document(collection = "bookmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndex(name = "user_travel_idx", def = "{'userId': 1, 'travelId': 1}", unique = true)
public class BookmarkEty {
    
    @Id
    private String id;
    
    /**
     * ID dell'utente che ha creato il segnalibro
     */
    private String userId;
    
    /**
     * ID del viaggio che è stato salvato
     */
    private String travelId;
    
    /**
     * ID del proprietario del viaggio
     */
    private String travelOwnerId;
    
    /**
     * Data di creazione del segnalibro
     */
    private Date createdAt;
    
    /**
     * Costruttore per creare un nuovo bookmark
     */
    public BookmarkEty(String userId, String travelId, String travelOwnerId) {
        this.userId = userId;
        this.travelId = travelId;
        this.travelOwnerId = travelOwnerId;
        this.createdAt = new Date();
    }
}
