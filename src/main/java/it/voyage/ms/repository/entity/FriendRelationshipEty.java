package it.voyage.ms.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entità che rappresenta la relazione di amicizia tra utenti.
 * Relation: User N:M User via FriendRelationship
 */
@Entity
@Table(
    name = "friend_relationships",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_friend_requester_receiver",
                          columnNames = {"requester_id", "receiver_id"})
    },
    indexes = {
        @Index(name = "idx_friend_requester_id", columnList = "requester_id"),
        @Index(name = "idx_friend_receiver_id",  columnList = "receiver_id"),
        @Index(name = "idx_friend_status",        columnList = "status"),
        @Index(name = "idx_friend_created_at",    columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FriendRelationshipEty {

    /**
     * Stato della relazione di amicizia.
     * Usare @Enumerated(EnumType.STRING) per leggibilità nel DB
     * e per evitare problemi se i valori dell'enum vengono riordinati.
     */
    public enum Status {
        PENDING,
        ACCEPTED,
        REJECTED,
        BLOCKED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** Requester User ID = Firebase UID */
    @Column(name = "requester_id", nullable = false, length = 255)
    private String requesterId;

    /** Receiver User ID = Firebase UID */
    @Column(name = "receiver_id", nullable = false, length = 255)
    private String receiverId;

    /**
     * L'utente che ha inviato la richiesta di amicizia.
     * insertable/updatable = false: la FK è gestita dalla colonna requester_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_friend_requester"))
    private UserEty requester;

    /**
     * L'utente che ha ricevuto la richiesta di amicizia.
     * insertable/updatable = false: la FK è gestita dalla colonna receiver_id.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_friend_receiver"))
    private UserEty receiver;

    /**
     * Stato della relazione. Salvato come stringa nel DB per leggibilità
     * e stabilità rispetto al riordinamento dei valori enum.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private Status status;

    /**
     * Firebase UID dell'utente che ha eseguito il blocco.
     * Valorizzato solo quando status = BLOCKED.
     * Deve corrispondere a requesterId o receiverId.
     */
    @Column(name = "blocker_id", length = 255)
    private String blockerId;

    /**
     * Indica se la relazione è unidirezionale (follow) o bidirezionale (amicizia).
     * - true: Follow unidirezionale (tipicamente per profili pubblici)
     * - false: Amicizia bidirezionale (default)
     * 
     * Nota: nullable=true per permettere a Hibernate di aggiungere la colonna
     * Il valore di default (false) verrà applicato alle nuove righe
     */
    @Column(name = "is_unidirectional", nullable = true, columnDefinition = "boolean default false")
    private boolean isUnidirectional = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}