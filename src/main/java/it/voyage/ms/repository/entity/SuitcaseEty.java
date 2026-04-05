package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "suitcase")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuitcaseEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id")
    private TravelEty travel;

    @OneToMany(mappedBy = "suitcase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SuitcaseItemEty> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addItem(SuitcaseItemEty item) {
        items.add(item);
        item.setSuitcase(this);
    }

    public void removeItem(SuitcaseItemEty item) {
        items.remove(item);
        item.setSuitcase(null);
    }
}