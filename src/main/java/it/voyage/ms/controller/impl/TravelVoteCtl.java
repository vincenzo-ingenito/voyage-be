package it.voyage.ms.controller.impl;

import it.voyage.ms.controller.ITravelVoteCtl;
import it.voyage.ms.dto.request.VoteRequest;
import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelVoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class TravelVoteCtl implements ITravelVoteCtl {
    
    @Autowired
    private ITravelVoteService voteService;
    
    @Override
    public ResponseEntity<VoteStatsDTO> voteTravel(Long travelId, VoteRequest voteRequest, CustomUserDetails userDetails) {
        log.info("User {} voting on travel {}: {}", userDetails.getUserId(), travelId, voteRequest.getVoteType());
        
        if (voteRequest.getVoteType() == null) {
            // Se voteType è null, rimuovi il voto
            VoteStatsDTO stats = voteService.removeVote(travelId, userDetails.getUserId());
            return ResponseEntity.ok(stats);
        }
        
        VoteStatsDTO stats = voteService.vote(travelId, userDetails.getUserId(), voteRequest.getVoteType());
        return ResponseEntity.ok(stats);
    }
    
    @Override
    public ResponseEntity<VoteStatsDTO> removeVote(Long travelId, CustomUserDetails userDetails) {
        log.info("User {} removing vote from travel {}", userDetails.getUserId(), travelId);
        VoteStatsDTO stats = voteService.removeVote(travelId, userDetails.getUserId());
        return ResponseEntity.ok(stats);
    }
    
    @Override
    public ResponseEntity<VoteStatsDTO> getVoteStats(Long travelId, CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        log.info("🎯 [Controller] getVoteStats - travelId: {}, userId: '{}', userId.length: {}", 
                 travelId, userId, userId != null ? userId.length() : 0);
        VoteStatsDTO stats = voteService.getVoteStats(travelId, userId);
        return ResponseEntity.ok(stats);
    }
}