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
			            (u.isPrivate = false AND r1.id IS NOT NULL)
			            OR (u.isPrivate = true AND r1.id IS NOT NULL AND r2.id IS NOT NULL)
			        )
			    )
			    OR u.id = :userId
			""")
	List<UserEty> findAcceptedFriendsAndSelf(@Param("userId") String userId, @Param("status") FriendRelationshipEty.Status status);
	
	@Query("SELECT u.isPrivate FROM UserEty u WHERE u.id = :id")
	Optional<Boolean> findPrivacyStatusById(@Param("id") String id);
}
