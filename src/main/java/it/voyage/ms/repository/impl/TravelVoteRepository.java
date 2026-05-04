package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
     * Elimina il voto di un utente per un viaggio
     */
    void deleteByTravelIdAndUserId(Long travelId, String userId);
    
    
    /**
     * Conta i like per una lista di viaggi (batch query per performance)
     */
    @Query("SELECT tv.travelId, COUNT(tv) FROM TravelVoteEty tv WHERE tv.travelId IN :travelIds AND tv.voteType = 'UPVOTE' GROUP BY tv.travelId")
    List<Object[]> countLikesByTravelIds(@Param("travelIds") List<Long> travelIds);
    
    /**
     * Trova i voti dell'utente per una lista di viaggi (batch query per performance)
     */
    @Query("SELECT tv FROM TravelVoteEty tv WHERE tv.travelId IN :travelIds AND tv.userId = :userId")
    List<TravelVoteEty> findByTravelIdsAndUserId(@Param("travelIds") List<Long> travelIds, @Param("userId") String userId);
    
}
