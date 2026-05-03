package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

	/**
	 * Trova un viaggio per ID e user ID con itinerario, punti e file pre-caricati
	 * Ottimizzazione per evitare N+1 query quando serve il viaggio completo
	 */
	@Query("""
			    SELECT DISTINCT t FROM TravelEty t
			    LEFT JOIN FETCH t.itinerary i
			    LEFT JOIN FETCH i.points
			    LEFT JOIN FETCH t.files
			    WHERE t.id = :id AND t.user.id = :userId
			""")
	Optional<TravelEty> findByIdAndUserIdFull(
			@Param("id") Long id, 
			@Param("userId") String userId);

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

	/**
	 * Query ottimizzata per feed paginato con ordinamento DB-side
	 * Carica viaggi dell'utente + viaggi degli amici ACCEPTED
	 * Paginazione e ordinamento gestiti dal database
	 */
	@Query("""
			    SELECT DISTINCT t FROM TravelEty t
			    WHERE t.user.id = :userId
			       OR t.user.id IN (
			           SELECT CASE WHEN f.requesterId = :userId THEN f.receiverId ELSE f.requesterId END
			           FROM FriendRelationshipEty f
			           WHERE (f.requesterId = :userId OR f.receiverId = :userId)
			             AND f.status = 'ACCEPTED'
			       )
			    ORDER BY t.dateTo DESC NULLS LAST
			""")
	org.springframework.data.domain.Page<TravelEty> findFeedForUser(
			@Param("userId") String userId,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			WHERE t.user.id IN :userIds
			""")
	List<TravelEty> findByUserIdIn(@Param("userIds") List<String> userIds);

	@Query("""
			SELECT DISTINCT t FROM TravelEty t
			WHERE t.user.id = :userId
			OR t.user.id IN :friendIds
			ORDER BY t.dateTo DESC NULLS LAST
			""")
	Page<TravelEty> findFeedPageByUserIdAndFriendIds(
			@Param("userId") String userId,
			@Param("friendIds") List<String> friendIds,
			Pageable pageable
			);
}
