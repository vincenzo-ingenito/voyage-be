package it.voyage.ms.dto.response;

import java.time.LocalDateTime;

import it.voyage.ms.repository.entity.ParticipantRole;
import it.voyage.ms.repository.entity.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per rappresentare un partecipante a un viaggio di gruppo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDTO {
    
    private Long id;
    
    /**
     * ID del viaggio
     */
    private Long travelId;
    
    /**
     * ID utente partecipante (Firebase UID)
     */
    private String userId;
    
    /**
     * Nome dell'utente partecipante
     */
    private String userName;
    
    /**
     * Email dell'utente partecipante
     */
    private String userEmail;
    
    /**
     * Avatar dell'utente partecipante
     */
    private String userAvatar;
    
    /**
     * Ruolo del partecipante (VIEWER/EDITOR)
     */
    private ParticipantRole role;
    
    /**
     * Stato dell'invito (PENDING/ACCEPTED/DECLINED)
     */
    private ParticipantStatus status;
    
    /**
     * ID di chi ha inviato l'invito
     */
    private String invitedBy;
    
    /**
     * Data invito
     */
    private LocalDateTime invitedAt;
    
    /**
     * Data risposta all'invito
     */
    private LocalDateTime respondedAt;
}