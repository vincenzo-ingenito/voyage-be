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

    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        WHERE t.user.id = :userId
        """)
    List<TravelEty> findByUserId(@Param("userId") String userId);
    
    Optional<TravelEty> findByIdAndUserId(Long id, String userId);

    @Modifying
    @Transactional
    long deleteByIdAndUserId(Long id, String userId);

    // Query 1: solo itinerary (senza points)
    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.itinerary
        WHERE t.id = :id AND t.user.id = :userId
        """)
    Optional<TravelEty> findByIdAndUserIdWithItinerary(
        @Param("id") Long id, @Param("userId") String userId);

    // Query 2: itinerary con points (separata, stessa transazione)
    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.itinerary i
        LEFT JOIN FETCH i.points
        WHERE t.id = :id AND t.user.id = :userId
        """)
    Optional<TravelEty> findByIdAndUserIdWithPoints(
        @Param("id") Long id, @Param("userId") String userId);

    // Query 3: files
    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.files
        WHERE t.id = :id AND t.user.id = :userId
        """)
    Optional<TravelEty> findByIdAndUserIdWithFiles(
        @Param("id") Long id, @Param("userId") String userId);

    // --- versioni lista ---

    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.itinerary
        WHERE t.user.id = :userId
        """)
    List<TravelEty> findByUserIdWithItinerary(@Param("userId") String userId);

    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.itinerary i
        LEFT JOIN FETCH i.points
        WHERE t.user.id = :userId
        """)
    List<TravelEty> findByUserIdWithPoints(@Param("userId") String userId);

    @Query("""
        SELECT DISTINCT t FROM TravelEty t
        LEFT JOIN FETCH t.files
        WHERE t.user.id = :userId
        """)
    List<TravelEty> findByUserIdWithFiles(@Param("userId") String userId);
}