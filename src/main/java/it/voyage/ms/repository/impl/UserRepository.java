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

	/**
	 * MODELLO ASIMMETRICO: Restituisce solo gli utenti che currentUserId SEGUE.
	 * 
	 * Un utente appare nella lista se:
	 * 1. Esiste un record requesterId=currentUserId, receiverId=utente, status=ACCEPTED
	 *    (= currentUserId segue l'utente)
	 * 2. Oppure è currentUserId stesso
	 * 
	 * NON include utenti che seguono currentUserId ma che currentUserId non segue.
	 */
	@Query("""
			SELECT DISTINCT u FROM UserEty u
			WHERE u.id IN (
			    SELECT r.receiverId FROM FriendRelationshipEty r 
			    WHERE r.requesterId = :userId AND r.status = :status
			)
			OR u.id = :userId
			""")
	List<UserEty> findAcceptedFriendsAndSelf(@Param("userId") String userId, @Param("status") FriendRelationshipEty.Status status);

	/**
	 * OTTIMIZZAZIONE MEMORIA: Limita candidati e calcola viaggi in query
	 * Evita di caricare tutti gli utenti con findAll()
	 */
	@Query("""
			SELECT u.id, u.name, u.avatar, u.email, u.isPrivate,
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
