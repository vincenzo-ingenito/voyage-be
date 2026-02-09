package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entità che rappresenta la relazione di amicizia tra utenti
 * Relation: User N:M User via FriendRelationship
 */
@Entity
@Table(name = "friend_relationships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_requester_receiver", columnNames = {"requester_id", "receiver_id"})
    },
    indexes = {
        @Index(name = "idx_friend_requester_id", columnList = "requester_id"),
        @Index(name = "idx_friend_receiver_id", columnList = "receiver_id"),
        @Index(name = "idx_friend_status", columnList = "status"),
        @Index(name = "idx_friend_created_at", columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRelationshipEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Requester User ID = Firebase UID (String)
     */
    @Column(name = "requester_id", nullable = false, length = 255)
    private String requesterId;

    /**
     * Receiver User ID = Firebase UID (String)
     */
    @Column(name = "receiver_id", nullable = false, length = 255)
    private String receiverId;

    /**
     * Relation: User N:M User via FriendRelationship
     * L'utente che ha inviato la richiesta di amicizia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_friend_requester"))
    private UserEty requester;

    /**
     * Relation: User N:M User via FriendRelationship
     * L'utente che ha ricevuto la richiesta di amicizia
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_friend_receiver"))
    private UserEty receiver;

    /**
     * Status della relazione: PENDING, ACCEPTED, REJECTED, BLOCKED
     */
    @Column(name = "status", nullable = false, length = 50)
    private String status;

    /**
     * Nome/ID dell'utente che ha bloccato (usato quando status = BLOCKED)
     */
    @Column(name = "blocker_name", length = 255)
    private String blockerName;

    /**
     * Data di creazione della relazione
     */
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}