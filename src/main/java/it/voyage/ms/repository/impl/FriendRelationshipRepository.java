package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.repository.entity.FriendRelationship;


@Repository
public interface FriendRelationshipRepository extends MongoRepository<FriendRelationship, String> {
    List<FriendRelationship> findByRequesterIdAndStatus(String requesterId, String status);
    List<FriendRelationship> findByReceiverIdAndStatus(String receiverId, String status);
    Optional<FriendRelationship> findByRequesterIdAndReceiverId(String requesterId, String receiverId);
    Optional<FriendRelationship> findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(String requesterId, String receiverId, String requesterId2, String receiverId2);
    List<FriendRelationship> findByRequesterIdAndStatusOrReceiverIdAndStatus(String requesterId, String requesterStatus, String receiverId, String receiverStatus);
    
    @Transactional
    void deleteByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(String requesterId, String receiverId, String receiverId2, String requesterId2);
    
    List<FriendRelationship> findByRequesterIdAndReceiverIdAndStatus(String requesterId, String receiverId, String status);
}
