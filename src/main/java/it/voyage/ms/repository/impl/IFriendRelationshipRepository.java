package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.FriendRelationshipEty;


@Repository
public interface IFriendRelationshipRepository extends MongoRepository<FriendRelationshipEty, String> {
	List<FriendRelationshipEty> findByRequesterIdAndStatus(String requesterId, String status);
	List<FriendRelationshipEty> findByReceiverIdAndStatus(String receiverId, String status);
	Optional<FriendRelationshipEty> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
	Optional<FriendRelationshipEty> findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(String requesterId, String receiverId, String requesterId2, String receiverId2);
	List<FriendRelationshipEty> findByRequesterIdAndStatusOrReceiverIdAndStatus(String requesterId, String requesterStatus, String receiverId, String receiverStatus);

	@Query(value = "{ '$or': [ " +
			"{ 'requesterId': ?0, 'receiverId': ?1 }, " +
			"{ 'requesterId': ?1, 'receiverId': ?0 } " +
			"] }", delete = true)
	void deleteFriendship(String userId, String friendId);

	List<FriendRelationshipEty> findByRequesterIdAndReceiverIdAndStatus(String requesterId, String receiverId, String status);

	@Query("{ 'requesterId' : ?0, 'receiverId' : ?1, 'status' : 'PENDING' }")
	@Update("{ '$set' : { 'status' : ?2 } }")
	int updateRequestStatus(String requesterId, String receiverId, String newStatus);

	@Query("{$or: ["
			+ "{ 'requesterId': ?0, 'receiverId': { '$in': ?1 } },"
			+ "{ 'receiverId': ?0, 'requesterId': { '$in': ?1 } }"
			+ "]}")
	List<FriendRelationshipEty> findAllRelevantRelationships(String currentUserId, List<String> userIdsToCheck);


	@Query("{$or: ["
			+ "{ 'requesterId': ?0, 'receiverId': ?1, 'status': ?2 },"
			+ "{ 'receiverId': ?0, 'requesterId': ?1, 'status': ?2 }"
			+ "]}")
	Optional<FriendRelationshipEty> findFriendshipByUsersAndStatus(String userId1, String userId2, String status);
}
