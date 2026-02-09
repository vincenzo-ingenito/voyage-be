package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEty {

    /**
     * ID utente = Firebase UID (token)
     */
    @Id
    @Column(name = "id", length = 255)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "avatar", length = 500)
    private String avatar;

    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Column(name = "last_login")
    @Temporal(TemporalType.TIMESTAMP)
    private Date lastLogin;

    @Column(name = "is_private", nullable = false)
    private boolean isPrivate = false;

    @Column(name = "bio", columnDefinition = "TEXT")
    private String bio;

    // Relation: User 1:N Travel (un utente ha molti viaggi)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TravelEty> travels = new ArrayList<>();

    // Relation: User N:M Travel via Bookmark (utenti salvano viaggi)
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BookmarkEty> bookmarks = new ArrayList<>();

    // Relation: User N:M User via FriendRelationship (amicizie - richieste inviate)
    @OneToMany(mappedBy = "requester", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FriendRelationshipEty> sentFriendRequests = new ArrayList<>();

    // Relation: User N:M User via FriendRelationship (amicizie - richieste ricevute)
    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FriendRelationshipEty> receivedFriendRequests = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}