package it.voyage.ms.repository.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_name",  columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEty {

    /** ID utente = Firebase UID (stringa immutabile) */
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "avatar", length = 500)
    private String avatar;

    /** Valorizzato automaticamente in @PrePersist; mai aggiornato. */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "last_login")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "show_emergency_fab", nullable = false, columnDefinition = "boolean default true")
    private boolean showEmergencyFAB = true;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    /** Relation: User 1:N Travel */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TravelEty> travels = new ArrayList<>();

    /** Relation: User N:M Travel via Bookmark */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookmarkEty> bookmarks = new ArrayList<>();

    /** Relation: User N:M User via FriendRelationship — richieste inviate */
    @OneToMany(mappedBy = "requester", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FriendRelationshipEty> sentFriendRequests = new ArrayList<>();

    /** Relation: User N:M User via FriendRelationship — richieste ricevute */
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL,
               orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FriendRelationshipEty> receivedFriendRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}