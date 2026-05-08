package it.voyage.ms.controller.impl;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IGroupTravelCtl;
import it.voyage.ms.dto.request.ParticipantInviteRequest;
import it.voyage.ms.dto.response.ParticipantDTO;
import it.voyage.ms.repository.entity.ParticipantRole;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IGroupTravelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller REST per la gestione dei viaggi di gruppo
 */
@RestController
@Slf4j
@AllArgsConstructor
public class GroupTravelCtl implements IGroupTravelCtl {

    private final IGroupTravelService groupTravelService;

    @Override
    public ResponseEntity<List<ParticipantDTO>> inviteParticipants(Long travelId, List<ParticipantInviteRequest> invites, CustomUserDetails userDetails) {
        log.info("POST /api/travel/{}/participants - Invito {} partecipanti", travelId, invites.size());
        List<ParticipantDTO> participants = groupTravelService.inviteParticipants(travelId, invites, userDetails.getUserId());
        return ResponseEntity.ok(participants);
    }

    @Override
    public ResponseEntity<List<ParticipantDTO>> getParticipants(Long travelId, CustomUserDetails userDetails) {
        log.info("GET /api/travel/{}/participants", travelId);
        List<ParticipantDTO> participants = groupTravelService.getParticipants(travelId);
        return ResponseEntity.ok(participants);
    }

    @Override
    public ResponseEntity<ParticipantDTO> respondToInvite(Long travelId, boolean accept, CustomUserDetails userDetails) {
        log.info("POST /api/travel/{}/invite/respond - accept={}", travelId, accept);
        ParticipantDTO participant = groupTravelService.respondToInvite(travelId, userDetails.getUserId(), accept);
        return ResponseEntity.ok(participant);
    }

    @Override
    public ResponseEntity<ParticipantDTO> updateParticipantRole(Long travelId, String userId, ParticipantRole newRole, CustomUserDetails userDetails) {
        log.info("PUT /api/travel/{}/participants/{} - newRole={}", travelId, userId, newRole);
        ParticipantDTO participant = groupTravelService.updateParticipantRole(travelId, userId, newRole, userDetails.getUserId());
        return ResponseEntity.ok(participant);
    }

    @Override
    public ResponseEntity<Void> removeParticipant(Long travelId, String userId, CustomUserDetails userDetails) {
        log.info("DELETE /api/travel/{}/participants/{}", travelId, userId);
        groupTravelService.removeParticipant(travelId, userId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<ParticipantDTO>> getMyGroupTravelParticipations(CustomUserDetails userDetails) {
        log.info("GET /api/travel/group/my-participations");
        List<ParticipantDTO> participations = groupTravelService.getUserGroupTravelParticipations(userDetails.getUserId());
        return ResponseEntity.ok(participations);
    }
}