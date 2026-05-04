package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.TravelParticipantEty;

@Repository
public interface TravelParticipantRepository extends JpaRepository<TravelParticipantEty, Long> {

    /**
     * Trova un partecipante specifico in un viaggio
     */
    Optional<TravelParticipantEty> findByTravelIdAndUserId(Long travelId, String userId);
 

    /**
     * Verifica se un utente è partecipante di un viaggio
     */
    boolean existsByTravelIdAndUserId(Long travelId, String userId);

    /**
     * Elimina un partecipante da un viaggio
     */
    void deleteByTravelIdAndUserId(Long travelId, String userId);
    
    @Query("""
		    SELECT p FROM TravelParticipantEty p
		    LEFT JOIN FETCH p.travel t
		    LEFT JOIN FETCH t.user
		    WHERE p.userId = :userId
		""")
    List<TravelParticipantEty> findByUserIdWithTravelAndOwner(@Param("userId") String userId);
    
    /**
     * Trova tutti i partecipanti di un viaggio con travel e owner pre-caricati
     * Ottimizzazione per evitare N+1 query
     */
    @Query("""
        SELECT DISTINCT p FROM TravelParticipantEty p
        LEFT JOIN FETCH p.travel t
        LEFT JOIN FETCH t.user
        WHERE p.travel.id = :travelId
    """)
    List<TravelParticipantEty> findByTravelIdWithTravelAndOwner(@Param("travelId") Long travelId);
}
