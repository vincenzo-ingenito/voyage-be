package it.voyage.ms.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.TravelVoteEty;
import it.voyage.ms.repository.entity.VoteType;

@Repository
public interface TravelVoteRepository extends JpaRepository<TravelVoteEty, Long> {
    
    /**
     * Trova il voto di un utente per un viaggio specifico
     */
    Optional<TravelVoteEty> findByTravelIdAndUserId(Long travelId, String userId);
    
    /**
     * Conta i like per un viaggio
     */
    Long countByTravelIdAndVoteType(Long travelId, VoteType voteType);
    
    /**
     * Conta tutti i voti per un viaggio
     */
    Long countByTravelId(Long travelId);
    
    /**
     * Elimina il voto di un utente per un viaggio
     */
    void deleteByTravelIdAndUserId(Long travelId, String userId);
    
    /**
     * Trova tutti i voti di un utente
     */
    List<TravelVoteEty> findByUserId(String userId);
    
    /**
     * Trova tutti i voti per un viaggio
     */
    List<TravelVoteEty> findByTravelId(Long travelId);
    
}