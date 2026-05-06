package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per i suggerimenti di amici nel feed.
 * Contiene informazioni arricchite per aiutare l'utente a decidere se aggiungere l'amico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSuggestionDTO {
    
    /**
     * ID univoco dell'utente suggerito
     */
    private String id;
    
    /**
     * Nome completo dell'utente
     */
    private String name;
    
    /**
     * URL dell'avatar/foto profilo
     */
    private String avatar;
    
    /**
     * Biografia o descrizione dell'utente (opzionale)
     */
    private String bio;
    
    /**
     * Numero totale di viaggi creati dall'utente
     */
    private Integer travelsCount;
    
    /**
     * Numero di amici in comune con l'utente corrente
     */
    private Integer mutualFriendsCount;
    
    /**
     * Lista dei primi 3 nomi di amici in comune (per mostrare "Amico di Mario, Luca...")
     */
    private String mutualFriendsPreview;
    
    /**
     * Motivo del suggerimento:
     * - "mutual_friends" = Amici in comune
     * - "similar_travels" = Ha visitato destinazioni simili
     * - "active_traveler" = Viaggiatore attivo
     * - "new_user" = Nuovo su Voyage
     * - "ai_explorer" = AI Travel Explorer (utente demo)
     */
    private String reason;
    
    /**
     * Indica se l'utente è un AI User generato per demo
     */
    private Boolean isAiUser;
}
