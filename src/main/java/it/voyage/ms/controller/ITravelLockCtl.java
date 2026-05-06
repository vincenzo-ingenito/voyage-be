package it.voyage.ms.controller;

import it.voyage.ms.dto.response.TravelLockDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per gestire i lock sui viaggi di gruppo
 */
@RequestMapping("/api/travel")
public interface ITravelLockCtl {

    /**
     * Acquisisce un lock su un viaggio
     * POST /api/travel/{travelId}/lock
     */
    @PostMapping("/{travelId}/lock")
    ResponseEntity<TravelLockDTO> acquireLock(
        @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );

    /**
     * Rilascia un lock su un viaggio
     * DELETE /api/travel/{travelId}/lock
     */
    @DeleteMapping("/{travelId}/lock")
    ResponseEntity<Void> releaseLock(
        @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );

    /**
     * Invia un heartbeat per mantenere il lock attivo
     * PUT /api/travel/{travelId}/lock/heartbeat
     */
    @PutMapping("/{travelId}/lock/heartbeat")
    ResponseEntity<TravelLockDTO> heartbeat(
        @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );

    /**
     * Ottiene lo stato del lock per un viaggio
     * GET /api/travel/{travelId}/lock/status
     */
    @GetMapping("/{travelId}/lock/status")
    ResponseEntity<TravelLockDTO> getLockStatus(
        @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}