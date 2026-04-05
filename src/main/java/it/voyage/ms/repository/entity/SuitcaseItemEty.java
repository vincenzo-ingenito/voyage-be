package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "suitcase_item")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuitcaseItemEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "suitcase_id", nullable = false)
    private SuitcaseEty suitcase;

    @Column(nullable = false)
    private String name;

    @Column(name = "is_checked")
    @Builder.Default
    private Boolean isChecked = false;

    @Column
    private Integer quantity;

    @Column
    private String category;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}