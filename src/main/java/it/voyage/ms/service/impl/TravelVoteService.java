package it.voyage.ms.service.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.TravelVoteRepository;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelVoteEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.entity.VoteType;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.INotificationService;
import it.voyage.ms.service.ITravelVoteService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TravelVoteService implements ITravelVoteService {
    
    @Autowired
    private TravelVoteRepository voteRepository;
    
    @Autowired
    private TravelRepository travelRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private INotificationService notificationService;
    
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
                log.info("Changing vote from {} to {} for user {} on travel {}", vote.getVoteType(), voteType, userId, travelId);
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
            
            // Invia notifica al proprietario del viaggio (solo per UPVOTE/LIKE)
            if (voteType == VoteType.UPVOTE) {
                try {
                    TravelEty travel = travelRepository.findById(travelId)
                        .orElseThrow(() -> new NotFoundException("Viaggio non trovato"));
                    
                    // Non inviare notifica se l'utente mette like al proprio viaggio
                    if (!travel.getUser().getId().equals(userId)) {
                        UserEty liker = userRepository.findById(userId).orElse(null);
                        if (liker != null) {
                            notificationService.sendTravelLikeNotification(
                                travel.getUser().getId(),
                                liker.getName(),
                                travel.getTravelName(),
                                travelId
                            );
                        }
                    }
                } catch (Exception e) {
                    log.error("❌ Errore invio notifica like", e);
                    // Non bloccare il voto se la notifica fallisce
                }
            }
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
        Long likes = voteRepository.countByTravelIdAndVoteType(travelId, VoteType.UPVOTE);
        log.info("Trovati {} likes per il viaggio {}", likes, travelId);
        
        VoteType userVote = null;
        Optional<TravelVoteEty> vote = voteRepository.findByTravelIdAndUserId(travelId, userId);
        
        if (vote.isPresent()) {
            TravelVoteEty voteEntity = vote.get();
            userVote = voteEntity.getVoteType();
            log.info("Trovato voto utente per travel {}: userId='{}', voteType={}, voteId={}", travelId, voteEntity.getUserId(), userVote, voteEntity.getId());
        }  
        return new VoteStatsDTO(likes, userVote);
    }
}