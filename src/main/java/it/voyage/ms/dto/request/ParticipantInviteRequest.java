package it.voyage.ms.dto.request;

import it.voyage.ms.repository.entity.ParticipantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per richiedere l'invito di un partecipante a un viaggio di gruppo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantInviteRequest {
    
    /**
     * ID utente da invitare (Firebase UID)
     */
    @NotBlank(message = "User ID is required")
    private String userId;
    
    /**
     * Ruolo da assegnare (VIEWER o EDITOR)
     */
    @NotNull(message = "Role is required")
    private ParticipantRole role;
}