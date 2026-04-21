package it.voyage.ms.repository;

import it.voyage.ms.repository.entity.TravelVoteEty;
import it.voyage.ms.repository.entity.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TravelVoteRepository extends JpaRepository<TravelVoteEty, Long> {
    
    /**
     * Trova il voto di un utente per un viaggio specifico
     */
    Optional<TravelVoteEty> findByTravelIdAndUserId(Long travelId, String userId);
    
    /**
     * Conta gli upvotes per un viaggio
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
    
    /**
     * Calcola il punteggio netto di un viaggio (upvotes - downvotes)
     */
    @Query("SELECT " +
           "(SELECT COUNT(v1) FROM TravelVoteEty v1 WHERE v1.travelId = :travelId AND v1.voteType = 'UPVOTE') - " +
           "(SELECT COUNT(v2) FROM TravelVoteEty v2 WHERE v2.travelId = :travelId AND v2.voteType = 'DOWNVOTE') " +
           "FROM TravelVoteEty v WHERE v.travelId = :travelId")
    Long calculateNetScore(@Param("travelId") Long travelId);
}