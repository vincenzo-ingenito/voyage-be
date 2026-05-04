package it.voyage.ms.repository.impl;

import java.util.List;
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
	
}
