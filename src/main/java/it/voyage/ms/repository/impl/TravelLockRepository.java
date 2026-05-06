package it.voyage.ms.repository.impl;

import it.voyage.ms.repository.entity.TravelLockEty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TravelLockRepository extends JpaRepository<TravelLockEty, Long> {

    /**
     * Trova il lock attivo per un viaggio
     */
    Optional<TravelLockEty> findByTravelId(Long travelId);

    /**
     * Trova il lock attivo per un viaggio che non sia scaduto
     */
    @Query("""
        SELECT l FROM TravelLockEty l
        WHERE l.travelId = :travelId
        AND l.expiresAt > :now
    """)
    Optional<TravelLockEty> findActiveLockByTravelId(
        @Param("travelId") Long travelId,
        @Param("now") LocalDateTime now
    );

    /**
     * Elimina i lock scaduti
     */
    @Modifying
    @Query("""
        DELETE FROM TravelLockEty l
        WHERE l.expiresAt < :now
    """)
    void deleteExpiredLocks(@Param("now") LocalDateTime now);

    /**
     * Elimina il lock di un viaggio
     */
    void deleteByTravelId(Long travelId);

    /**
     * Elimina il lock di un viaggio se appartiene a un determinato utente
     */
    @Modifying
    @Query("""
        DELETE FROM TravelLockEty l
        WHERE l.travelId = :travelId
        AND l.lockedByUserId = :userId
    """)
    void deleteByTravelIdAndUserId(
        @Param("travelId") Long travelId,
        @Param("userId") String userId
    );
}