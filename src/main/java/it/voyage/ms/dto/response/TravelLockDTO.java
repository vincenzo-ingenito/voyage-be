package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO per rappresentare lo stato del lock su un viaggio
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelLockDTO {
    
    private Long travelId;
    private boolean isLocked;
    private String lockedByUserId;
    private String lockedByUserName;
    private LocalDateTime lockedAt;
    private LocalDateTime expiresAt;
    private boolean isOwnedByCurrentUser;
    
    /**
     * Costruttore per quando non c'è lock
     */
    public static TravelLockDTO unlocked(Long travelId) {
        TravelLockDTO dto = new TravelLockDTO();
        dto.setTravelId(travelId);
        dto.setLocked(false);
        dto.setOwnedByCurrentUser(false);
        return dto;
    }
}