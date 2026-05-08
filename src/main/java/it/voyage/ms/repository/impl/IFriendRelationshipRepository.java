package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.FriendRelationshipEty.Status;
import jakarta.transaction.Transactional;

@Repository
public interface IFriendRelationshipRepository extends JpaRepository<FriendRelationshipEty, String> {


	List<FriendRelationshipEty> findByReceiverIdAndStatus(String receiverId, Status status);

	List<FriendRelationshipEty> findByRequesterIdAndStatus(String requesterId, Status status);

	Optional<FriendRelationshipEty> findByRequesterIdAndReceiverId(String requesterId, String receiverId);

	// -------------------------------------------------------------------------
	// DELETE friendship (bidirezionale)
	// -------------------------------------------------------------------------
//	@Modifying
//	@Transactional
//	@Query("""
//			    DELETE FROM FriendRelationshipEty f
//			    WHERE (f.requesterId = :userId AND f.receiverId = :friendId)
//			       OR (f.requesterId = :friendId AND f.receiverId = :userId)
//			""")
//	int deleteFriendship(@Param("userId")   String userId,
//			@Param("friendId") String friendId);
	@Modifying
	@Transactional
	@Query("""
	    DELETE FROM FriendRelationshipEty f
	    WHERE f.requesterId = :requesterId
	      AND f.receiverId  = :receiverId
	""")
	int deleteFriendship(@Param("requesterId") String requesterId,
	                     @Param("receiverId")  String receiverId);


	// -------------------------------------------------------------------------
	// Find all relevant relationships (bulk, evita N+1)
	// -------------------------------------------------------------------------
	@Query("""
			    SELECT f FROM FriendRelationshipEty f
			    WHERE (f.requesterId = :currentUserId AND f.receiverId  IN :userIds)
			       OR (f.receiverId  = :currentUserId AND f.requesterId IN :userIds)
			""")
	List<FriendRelationshipEty> findAllRelevantRelationships(
			@Param("currentUserId") String currentUserId,
			@Param("userIds")       List<String> userIds);


	// -------------------------------------------------------------------------
	// Update relationship status + blockerId
	// -------------------------------------------------------------------------
//	@Modifying
//	@Transactional
//	@Query("""
//			    UPDATE FriendRelationshipEty f
//			    SET f.status    = :status,
//			        f.blockerId = :blockerId
//			    WHERE (f.requesterId = :user1Id AND f.receiverId = :user2Id)
//			       OR (f.requesterId = :user2Id AND f.receiverId = :user1Id)
//			""")
//	void updateRelationshipStatus(@Param("user1Id")   String user1Id,
//			@Param("user2Id")   String user2Id,
//			@Param("status")    Status status,
//			@Param("blockerId") String blockerId);
	@Modifying
	@Transactional
	@Query("""
	    UPDATE FriendRelationshipEty f
	    SET f.status    = :status,
	        f.blockerId = :blockerId
	    WHERE f.requesterId = :blockerId
	      AND f.receiverId  = :targetId
	""")
	void updateRelationshipStatus(@Param("blockerId") String blockerId,
	                              @Param("targetId")  String targetId,
	                              @Param("status")    Status status);

	// -------------------------------------------------------------------------
	// Find my blocked relationships
	// -------------------------------------------------------------------------
	@Query("""
			    SELECT f FROM FriendRelationshipEty f
			    WHERE f.blockerId = :currentUserId
			      AND f.status    = it.voyage.ms.repository.entity.FriendRelationshipEty$Status.BLOCKED
			""")
	List<FriendRelationshipEty> findMyBlockedRelationships(
			@Param("currentUserId") String currentUserId);

	// -------------------------------------------------------------------------
	// Delete relationship only if blockerId matches
	// -------------------------------------------------------------------------
//	@Modifying
//	@Transactional
//	@Query("""
//			    DELETE FROM FriendRelationshipEty f
//			    WHERE ((f.requesterId = :user1Id AND f.receiverId = :user2Id)
//			        OR (f.requesterId = :user2Id AND f.receiverId = :user1Id))
//			      AND f.blockerId = :blockerId
//			""")
//	void deleteRelationship(@Param("user1Id")   String user1Id,
//			@Param("user2Id")   String user2Id,
//			@Param("blockerId") String blockerId);
	@Modifying
	@Transactional
	@Query("""
	    DELETE FROM FriendRelationshipEty f
	    WHERE f.requesterId = :blockerId
	      AND f.receiverId  = :targetId
	""")
	void deleteRelationship(@Param("blockerId") String blockerId,
	                        @Param("targetId")  String targetId);


	// -------------------------------------------------------------------------
	// Find pending requests with requester (fetch join)
	// -------------------------------------------------------------------------
	@Query("""
			    SELECT r FROM FriendRelationshipEty r
			    LEFT JOIN FETCH r.requester
			    WHERE r.receiverId = :receiverId
			      AND r.status     = :status
			""")
	List<FriendRelationshipEty> findPendingRequestsWithRequester(
			@Param("receiverId") String receiverId,
			@Param("status")     Status status);

	// -------------------------------------------------------------------------
	// Find all friendships by user ID and status (bidirectional)
	// Useful for getting all friends of a user
	// -------------------------------------------------------------------------
	@Query("""
			    SELECT f FROM FriendRelationshipEty f
			    WHERE (f.requesterId = :userId OR f.receiverId = :userId)
			      AND f.status = :status
			""")
	List<FriendRelationshipEty> findFriendshipsByUserIdAndStatus(
			@Param("userId") String userId,
			@Param("status") Status status);

	// -------------------------------------------------------------------------
	// Find all relationships involving a user (as requester OR receiver)
	// Useful for finding all relationships for suggestions filtering
	// -------------------------------------------------------------------------
	@Query("""
			    SELECT f FROM FriendRelationshipEty f
			    WHERE f.requesterId = :userId OR f.receiverId = :userId
			""")
	List<FriendRelationshipEty> findByRequesterIdOrReceiverId(
			@Param("userId") String userId);

	// -------------------------------------------------------------------------
	// Find friendships for multiple users (bulk query)
	// Ottimizzazione per evitare N+1 query nei suggerimenti di amicizia
	// -------------------------------------------------------------------------
	@Query("""
	    SELECT f FROM FriendRelationshipEty f
	    WHERE (f.requesterId IN :userIds OR f.receiverId IN :userIds)
	      AND f.status = :status
	""")
	List<FriendRelationshipEty> findFriendshipsByUserIdsAndStatus(
	    @Param("userIds") List<String> userIds,
	    @Param("status") Status status);

}
