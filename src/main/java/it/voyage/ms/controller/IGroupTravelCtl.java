package it.voyage.ms.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.voyage.ms.dto.request.ParticipantInviteRequest;
import it.voyage.ms.dto.response.ParticipantDTO;
import it.voyage.ms.repository.entity.ParticipantRole;
import it.voyage.ms.security.user.CustomUserDetails;
import jakarta.validation.Valid;

/**
 * Interfaccia controller per la gestione dei viaggi di gruppo
 */
@RequestMapping("/api/travel")
public interface IGroupTravelCtl {

    /**
     * Invita uno o più partecipanti a un viaggio di gruppo
     * POST /api/travel/{travelId}/participants
     */
    @PostMapping("/{travelId}/participants")
    ResponseEntity<List<ParticipantDTO>> inviteParticipants(@PathVariable Long travelId, @Valid @RequestBody List<ParticipantInviteRequest> invites, @AuthenticationPrincipal CustomUserDetails userDetails);

    /**
     * Ottiene tutti i partecipanti di un viaggio
     * GET /api/travel/{travelId}/participants
     */
    @GetMapping("/{travelId}/participants")
    ResponseEntity<List<ParticipantDTO>> getParticipants(@PathVariable Long travelId, @AuthenticationPrincipal CustomUserDetails userDetails);

    /**
     * Risponde a un invito (accetta o rifiuta)
     * POST /api/travel/{travelId}/invite/respond
     */
    @PostMapping("/{travelId}/invite/respond")
    ResponseEntity<ParticipantDTO> respondToInvite(@PathVariable Long travelId, @RequestParam boolean accept, @AuthenticationPrincipal CustomUserDetails userDetails);

    /**
     * Cambia il ruolo di un partecipante
     * PUT /api/travel/{travelId}/participants/{userId}
     */
    @PutMapping("/{travelId}/participants/{userId}")
    ResponseEntity<ParticipantDTO> updateParticipantRole(@PathVariable Long travelId, @PathVariable String userId, @RequestParam ParticipantRole newRole, @AuthenticationPrincipal CustomUserDetails userDetails);

    /**
     * Rimuove un partecipante da un viaggio
     * DELETE /api/travel/{travelId}/participants/{userId}
     */
    @DeleteMapping("/{travelId}/participants/{userId}")
    ResponseEntity<Void> removeParticipant(
        @PathVariable Long travelId,
        @PathVariable String userId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );

    /**
     * Ottiene tutti i viaggi di gruppo a cui l'utente corrente partecipa
     * GET /api/travel/group/my-participations
     */
    @GetMapping("/group/my-participations")
    ResponseEntity<List<ParticipantDTO>> getMyGroupTravelParticipations(
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}