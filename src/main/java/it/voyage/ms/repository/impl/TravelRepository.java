package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.repository.entity.TravelEty;

@Repository
public interface TravelRepository extends JpaRepository<TravelEty, Long> {

    List<TravelEty> findByUserId(String userId);

    Optional<TravelEty> findByIdAndUserId(Long id, String userId);
    
    /**
     * Trova un viaggio con i suoi file caricati in una singola query (JOIN FETCH)
     * Ottimizzato per l'eliminazione
     */
    @Query("SELECT t FROM TravelEty t LEFT JOIN FETCH t.files WHERE t.id = :id AND t.user.id = :userId")
    Optional<TravelEty> findByIdAndUserIdWithFiles(@Param("id") Long id, @Param("userId") String userId);

    @Modifying
    @Transactional
    long deleteByIdAndUserId(Long id, String userId);
}
