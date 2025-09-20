package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.FriendRelationship;


@Repository
public interface IFriendRelationshipRepository extends MongoRepository<FriendRelationship, String> {
	List<FriendRelationship> findByRequesterIdAndStatus(String requesterId, String status);
	List<FriendRelationship> findByReceiverIdAndStatus(String receiverId, String status);
	Optional<FriendRelationship> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
	Optional<FriendRelationship> findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(String requesterId, String receiverId, String requesterId2, String receiverId2);
	List<FriendRelationship> findByRequesterIdAndStatusOrReceiverIdAndStatus(String requesterId, String requesterStatus, String receiverId, String receiverStatus);

	@Query(value = "{ '$or': [ " +
			"{ 'requesterId': ?0, 'receiverId': ?1 }, " +
			"{ 'requesterId': ?1, 'receiverId': ?0 } " +
			"] }", delete = true)
	void deleteFriendship(String userId, String friendId);

	List<FriendRelationship> findByRequesterIdAndReceiverIdAndStatus(String requesterId, String receiverId, String status);

	@Query("{ 'requesterId' : ?0, 'receiverId' : ?1, 'status' : 'PENDING' }")
	@Update("{ '$set' : { 'status' : ?2 } }")
	void updateRequestStatus(String requesterId, String receiverId, String newStatus);
}
