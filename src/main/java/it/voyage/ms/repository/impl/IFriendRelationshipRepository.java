package it.voyage.ms.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import it.voyage.ms.repository.entity.FriendRelationshipEty;
import jakarta.transaction.Transactional;

@Repository
public interface IFriendRelationshipRepository
        extends JpaRepository<FriendRelationshipEty, String> {

    List<FriendRelationshipEty> findByRequesterIdAndStatus(String requesterId, String status);

    List<FriendRelationshipEty> findByReceiverIdAndStatus(String receiverId, String status);

    Optional<FriendRelationshipEty> findByRequesterIdAndReceiverId(String requesterId, String receiverId);

    Optional<FriendRelationshipEty>
    findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(
            String requesterId,
            String receiverId,
            String requesterId2,
            String receiverId2
    );

    List<FriendRelationshipEty>
    findByRequesterIdAndStatusOrReceiverIdAndStatus(
            String requesterId,
            String requesterStatus,
            String receiverId,
            String receiverStatus
    );

    // =========================
    // DELETE friendship (bidirezionale)
    // =========================
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM FriendRelationshipEty f
        WHERE (f.requesterId = :userId AND f.receiverId = :friendId)
           OR (f.requesterId = :friendId AND f.receiverId = :userId)
    """)
    int deleteFriendship(String userId, String friendId);

    List<FriendRelationshipEty>
    findByRequesterIdAndReceiverIdAndStatus(
            String requesterId,
            String receiverId,
            String status
    );

    // =========================
    // UPDATE request status (solo PENDING)
    // =========================
    @Modifying
    @Transactional
    @Query("""
        UPDATE FriendRelationshipEty f
        SET f.status = :newStatus
        WHERE f.requesterId = :requesterId
          AND f.receiverId = :receiverId
          AND f.status = 'PENDING'
    """)
    int updateRequestStatus(
            String requesterId,
            String receiverId,
            String newStatus
    );

    // =========================
    // Find all relevant relationships
    // =========================
    @Query("""
        SELECT f FROM FriendRelationshipEty f
        WHERE (f.requesterId = :currentUserId AND f.receiverId IN :userIds)
           OR (f.receiverId = :currentUserId AND f.requesterId IN :userIds)
    """)
    List<FriendRelationshipEty> findAllRelevantRelationships(
            String currentUserId,
            List<String> userIds
    );

    // =========================
    // Find friendship by users and status (bidirezionale)
    // =========================
    @Query("""
        SELECT f FROM FriendRelationshipEty f
        WHERE ((f.requesterId = :userId1 AND f.receiverId = :userId2)
            OR (f.requesterId = :userId2 AND f.receiverId = :userId1))
          AND f.status = :status
    """)
    Optional<FriendRelationshipEty> findFriendshipByUsersAndStatus(
            String userId1,
            String userId2,
            String status
    );

    // =========================
    // Update relationship status + blocker
    // =========================
    @Modifying
    @Transactional
    @Query("""
        UPDATE FriendRelationshipEty f
        SET f.status = :status,
            f.blockerName = :blockerName
        WHERE (f.requesterId = :user1Id AND f.receiverId = :user2Id)
           OR (f.requesterId = :user2Id AND f.receiverId = :user1Id)
    """)
    void updateRelationshipStatus(
            String user1Id,
            String user2Id,
            String status,
            String blockerName
    );

    // =========================
    // Find my blocked relationships
    // =========================
    @Query("""
        SELECT f FROM FriendRelationshipEty f
        WHERE f.blockerName = :currentUserId
          AND f.status = 'BLOCKED'
    """)
    List<FriendRelationshipEty> findMyBlockedRelationships(String currentUserId);

    // =========================
    // Delete relationship only if blocker matches
    // =========================
    @Modifying
    @Transactional
    @Query("""
        DELETE FROM FriendRelationshipEty f
        WHERE ((f.requesterId = :user1Id AND f.receiverId = :user2Id)
            OR (f.requesterId = :user2Id AND f.receiverId = :user1Id))
          AND f.blockerName = :blockerName
    """)
    void deleteRelationship(
            String user1Id,
            String user2Id,
            String blockerName
    );
}
