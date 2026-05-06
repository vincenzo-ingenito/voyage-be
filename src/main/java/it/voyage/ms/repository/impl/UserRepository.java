package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.UserEty;

@Repository
public interface UserRepository extends JpaRepository<UserEty, String> {

	/**
	 * Trova un utente per email.
	 * @param email L'email dell'utente.
	 * @return Un Optional contenente l'utente se trovato.
	 */
	Optional<UserEty> findByEmail(String email);

	/**
	 * Esegue una ricerca flessibile per nome utente utilizzando ILIKE
	 * case-insensitive.
	 * @param query La stringa di ricerca.
	 * @return Una lista di utenti che corrispondono alla query.
	 */
	@Query("SELECT u FROM UserEty u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%'))")
	List<UserEty> findByNameRegex(@Param("query") String query);

	@Query("""
			    SELECT DISTINCT u FROM UserEty u
			    LEFT JOIN FriendRelationshipEty r1 ON r1.requesterId = :userId AND r1.receiverId = u.id AND r1.status = :status
			    LEFT JOIN FriendRelationshipEty r2 ON r2.receiverId = :userId AND r2.requesterId = u.id AND r2.status = :status
			    WHERE (
			        (
			            (u.isPrivate = false AND (
			                r1.id IS NOT NULL 
			                OR (r2.id IS NOT NULL AND r2.isUnidirectional = false)
			            ))
			            OR (u.isPrivate = true AND (
			                r1.id IS NOT NULL 
			                OR (r2.id IS NOT NULL AND r2.isUnidirectional = false)
			            ))
			        )
			    )
			    OR u.id = :userId
			""")
	List<UserEty> findAcceptedFriendsAndSelf(@Param("userId") String userId, @Param("status") FriendRelationshipEty.Status status);

	/**
	 * OTTIMIZZAZIONE MEMORIA: Limita candidati e calcola viaggi in query
	 * Evita di caricare tutti gli utenti con findAll()
	 */
	@Query("""
			SELECT u.id, u.name, u.avatar, u.bio, u.isAiUser,
			       (SELECT COUNT(t.id) FROM TravelEty t WHERE t.user.id = u.id)
			FROM UserEty u
			WHERE u.id NOT IN :excludedIds
			ORDER BY (SELECT COUNT(t.id) FROM TravelEty t WHERE t.user.id = u.id) DESC
			""")
	List<Object[]> findPotentialSuggestionsOptimized(
			@Param("excludedIds") List<String> excludedIds,
			org.springframework.data.domain.Pageable pageable);

	/**
	 * OTTIMIZZAZIONE: Carica nomi in batch invece di N query
	 */
	@Query("SELECT u.id, u.name FROM UserEty u WHERE u.id IN :userIds")
	List<Object[]> findNamesMapByIds(@Param("userIds") List<String> userIds);

	/**
	 * Helper per convertire risultati in mappa
	 */
	default java.util.Map<String, String> findNamesByIds(List<String> userIds) {
		if (userIds == null || userIds.isEmpty()) {
			return java.util.Collections.emptyMap();
		}
		return findNamesMapByIds(userIds).stream()
				.collect(java.util.stream.Collectors.toMap(
						arr -> (String) arr[0],
						arr -> (String) arr[1]
				));
	}
	
}
