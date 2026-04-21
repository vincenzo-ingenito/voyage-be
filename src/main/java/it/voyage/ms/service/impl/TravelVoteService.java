package it.voyage.ms.service.impl;

import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.repository.TravelVoteRepository;
import it.voyage.ms.repository.entity.TravelVoteEty;
import it.voyage.ms.repository.entity.VoteType;
import it.voyage.ms.service.ITravelVoteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Slf4j
public class TravelVoteService implements ITravelVoteService {
    
    @Autowired
    private TravelVoteRepository voteRepository;
    
    @Override
    @Transactional
    public VoteStatsDTO vote(Long travelId, String userId, VoteType voteType) {
        log.info("User {} voting {} on travel {}", userId, voteType, travelId);
        
        Optional<TravelVoteEty> existingVote = voteRepository.findByTravelIdAndUserId(travelId, userId);
        
        if (existingVote.isPresent()) {
            TravelVoteEty vote = existingVote.get();
            if (vote.getVoteType() == voteType) {
                // Se vota la stessa cosa, rimuove il voto (toggle)
                log.info("Removing vote (toggle) for user {} on travel {}", userId, travelId);
                voteRepository.delete(vote);
            } else {
                // Cambia il voto
                log.info("Changing vote from {} to {} for user {} on travel {}", 
                         vote.getVoteType(), voteType, userId, travelId);
                vote.setVoteType(voteType);
                voteRepository.save(vote);
            }
        } else {
            // Nuovo voto
            log.info("Creating new {} vote for user {} on travel {}", voteType, userId, travelId);
            TravelVoteEty newVote = new TravelVoteEty();
            newVote.setTravelId(travelId);
            newVote.setUserId(userId);
            newVote.setVoteType(voteType);
            voteRepository.save(newVote);
        }
        
        return getVoteStats(travelId, userId);
    }
    
    @Override
    @Transactional
    public VoteStatsDTO removeVote(Long travelId, String userId) {
        log.info("Removing vote for user {} on travel {}", userId, travelId);
        voteRepository.deleteByTravelIdAndUserId(travelId, userId);
        return getVoteStats(travelId, userId);
    }
    
    @Override
    public VoteStatsDTO getVoteStats(Long travelId, String userId) {
        Long upvotes = voteRepository.countByTravelIdAndVoteType(travelId, VoteType.UPVOTE);
        Long downvotes = voteRepository.countByTravelIdAndVoteType(travelId, VoteType.DOWNVOTE);
        Long netScore = upvotes - downvotes;
        
        VoteType userVote = null;
        Optional<TravelVoteEty> vote = voteRepository.findByTravelIdAndUserId(travelId, userId);
        if (vote.isPresent()) {
            userVote = vote.get().getVoteType();
        }
        
        log.debug("Vote stats for travel {}: upvotes={}, downvotes={}, netScore={}, userVote={}", 
                  travelId, upvotes, downvotes, netScore, userVote);
        
        return new VoteStatsDTO(upvotes, downvotes, netScore, userVote);
    }
}