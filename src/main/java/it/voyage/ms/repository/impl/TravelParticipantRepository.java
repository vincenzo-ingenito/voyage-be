package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.ParticipantStatus;
import it.voyage.ms.repository.entity.TravelParticipantEty;

@Repository
public interface TravelParticipantRepository extends JpaRepository<TravelParticipantEty, Long> {

    /**
     * Trova tutti i partecipanti di un viaggio
     */
    List<TravelParticipantEty> findByTravelId(Long travelId);

    /**
     * Trova tutti i partecipanti di un viaggio con uno specifico stato
     */
    List<TravelParticipantEty> findByTravelIdAndStatus(Long travelId, ParticipantStatus status);

    /**
     * Trova un partecipante specifico in un viaggio
     */
    Optional<TravelParticipantEty> findByTravelIdAndUserId(Long travelId, String userId);

    /**
     * Trova tutti i viaggi a cui un utente partecipa (tutti gli stati)
     */
    @Query("SELECT p FROM TravelParticipantEty p " +
           "JOIN FETCH p.travel t " +
           "WHERE p.userId = :userId")
    List<TravelParticipantEty> findByUserId(@Param("userId") String userId);

    /**
     * Trova tutti i viaggi a cui un utente partecipa con uno specifico stato
     */
    @Query("SELECT p FROM TravelParticipantEty p " +
           "JOIN FETCH p.travel t " +
           "WHERE p.userId = :userId AND p.status = :status")
    List<TravelParticipantEty> findByUserIdAndStatus(
        @Param("userId") String userId,
        @Param("status") ParticipantStatus status
    );

    /**
     * Verifica se un utente è partecipante di un viaggio
     */
    boolean existsByTravelIdAndUserId(Long travelId, String userId);

    /**
     * Conta i partecipanti accettati di un viaggio
     */
    @Query("SELECT COUNT(p) FROM TravelParticipantEty p " +
           "WHERE p.travel.id = :travelId AND p.status = 'ACCEPTED'")
    long countAcceptedParticipants(@Param("travelId") Long travelId);

    /**
     * Elimina un partecipante da un viaggio
     */
    void deleteByTravelIdAndUserId(Long travelId, String userId);
}