package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entità che rappresenta un lock su un viaggio di gruppo.
 * Gestisce la concorrenza quando più editor cercano di modificare contemporaneamente.
 */
@Entity
@Table(name = "travel_locks", indexes = {
    @Index(name = "idx_travel_lock_travel_id", columnList = "travel_id"),
    @Index(name = "idx_travel_lock_expires_at", columnList = "expires_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelLockEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID del viaggio bloccato
     */
    @Column(name = "travel_id", nullable = false)
    private Long travelId;

    /**
     * ID dell'utente che ha acquisito il lock
     */
    @Column(name = "locked_by_user_id", nullable = false, length = 255)
    private String lockedByUserId;

    /**
     * Nome dell'utente che ha acquisito il lock (per visualizzazione)
     */
    @Column(name = "locked_by_user_name", length = 255)
    private String lockedByUserName;

    /**
     * Timestamp di acquisizione del lock
     */
    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    /**
     * Timestamp di scadenza del lock
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Ultimo heartbeat ricevuto (per mantenere il lock attivo)
     */
    @Column(name = "last_heartbeat_at", nullable = false)
    private LocalDateTime lastHeartbeatAt;

    /**
     * Verifica se il lock è ancora valido
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Verifica se il lock appartiene a un determinato utente
     */
    public boolean isOwnedBy(String userId) {
        return lockedByUserId.equals(userId);
    }
}