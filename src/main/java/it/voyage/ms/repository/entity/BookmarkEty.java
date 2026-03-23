package it.voyage.ms.repository.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entità che rappresenta un segnalibro (bookmark) di un viaggio.
 * Permette agli utenti di salvare i viaggi di altri utenti.
 * Relation: User N:M Travel via Bookmark
 */
@Entity
@Table(
    name = "bookmarks",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_user_travel", columnNames = {"user_id", "travel_id"})
    },
    indexes = {
        @Index(name = "idx_bookmark_user_id",       columnList = "user_id"),
        @Index(name = "idx_bookmark_travel_id",     columnList = "travel_id"),
        @Index(name = "idx_bookmark_owner_id",      columnList = "travel_owner_id"),
        @Index(name = "idx_bookmark_created_at",    columnList = "created_at")
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookmarkEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /** User ID = Firebase UID */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;

    /** Travel ID (Long auto-increment) */
    @Column(name = "travel_id", nullable = false)
    private Long travelId;

    /**
     * Relation: User N:M Travel via Bookmark — l'utente che ha creato il segnalibro.
     * insertable/updatable = false: la FK è gestita dalla colonna user_id sopra.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_bookmark_user"))
    private UserEty user;

    /**
     * Relation: User N:M Travel via Bookmark — il viaggio salvato.
     * insertable/updatable = false: la FK è gestita dalla colonna travel_id sopra.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_bookmark_travel"))
    private TravelEty travel;

    /**
     * ID del proprietario del viaggio (denormalizzato per performance nelle query
     * "tutti i bookmark sui miei viaggi").
     *
     * ATTENZIONE: mantenere sincronizzato con TravelEty.user.id.
     * Non aggiornato automaticamente da JPA — aggiornare esplicitamente nel service
     * se il proprietario del viaggio può cambiare.
     */
    @Column(name = "travel_owner_id", nullable = false, length = 255)
    private String travelOwnerId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    public BookmarkEty(String userId, Long travelId, String travelOwnerId) {
        this.userId        = userId;
        this.travelId      = travelId;
        this.travelOwnerId = travelOwnerId;
    }
}