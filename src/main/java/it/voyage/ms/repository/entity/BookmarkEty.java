package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entità che rappresenta un segnalibro (bookmark) di un viaggio
 * Permette agli utenti di salvare i viaggi di altri utenti
 * Relation: User N:M Travel via Bookmark
 */
@Entity
@Table(name = "bookmarks", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_bookmark_user_travel", columnNames = {"user_id", "travel_id"})
    },
    indexes = {
        @Index(name = "idx_bookmark_user_id", columnList = "user_id"),
        @Index(name = "idx_bookmark_travel_id", columnList = "travel_id"),
        @Index(name = "idx_bookmark_created_at", columnList = "created_at")
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
    
    /**
     * User ID = Firebase UID (String)
     */
    @Column(name = "user_id", nullable = false, length = 255)
    private String userId;
    
//    /**
//     * Travel ID (Long auto-increment)
//     */
//    @Column(name = "travel_id", nullable = false)
//    private Long travelId;
    
    /**
     * Relation: User N:M Travel via Bookmark
     * L'utente che ha creato il segnalibro
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_bookmark_user"))
    private UserEty user;
    
    /**
     * Relation: User N:M Travel via Bookmark
     * Il viaggio che è stato salvato
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", insertable = false, updatable = false, foreignKey = @ForeignKey(name = "fk_bookmark_travel"))
    private TravelEty travel;
    
    /**
     * ID del proprietario del viaggio (denormalizzato per performance)
     */
    @Column(name = "travel_owner_id", length = 255)
    private String travelOwnerId;
    
    /**
     * Data di creazione del segnalibro
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
    
    public BookmarkEty(String userId, Long travelId, String travelOwnerId) {
        this.userId = userId;
//        this.travelId = travelId;
        this.travelOwnerId = travelOwnerId;
        this.createdAt = new Date();
    }
}