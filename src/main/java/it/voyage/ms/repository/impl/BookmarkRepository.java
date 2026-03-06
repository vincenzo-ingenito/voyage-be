package it.voyage.ms.repository.impl;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.repository.entity.BookmarkEty;

/**
 * Repository per la gestione dei segnalibri (bookmarks)
 */
@Repository
public interface BookmarkRepository extends JpaRepository<BookmarkEty, Long> {

	/**
	 * Verifica se esiste un bookmark per un utente e un viaggio
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @return true se esiste, false altrimenti
	 */
	boolean existsByUserIdAndTravelId(String userId, Long travelId);

	/**
	 * Elimina un bookmark specifico
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 */
	@Transactional
	@Modifying
	int deleteByUserIdAndTravelId(String userId, Long travelId);

	@Query("""
			SELECT b FROM BookmarkEty b
			JOIN FETCH b.travel t
			LEFT JOIN FETCH t.user
			WHERE b.userId = :userId
			ORDER BY b.createdAt DESC
			""")
	List<BookmarkEty> findByUserIdWithTravel(@Param("userId") String userId);


	@Modifying
	@Query("""
			DELETE FROM BookmarkEty b 
			WHERE NOT EXISTS (
			    SELECT 1 FROM TravelEty t WHERE t.id = b.travelId
			)
			""")
	int deleteOrphanedBookmarks();

	/**
	 * Elimina tutti i bookmark associati a un viaggio
	 * @param travelId ID del viaggio
	 * @return numero di bookmark eliminati
	 */
	@Transactional
	@Modifying
	int deleteByTravelId(Long travelId);
}
