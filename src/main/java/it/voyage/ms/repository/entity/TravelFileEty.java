package it.voyage.ms.repository.entity;

import java.time.LocalDateTime;

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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel_files", indexes = {
    @Index(name = "idx_travel_file_travel_id", columnList = "travel_id"),
    @Index(name = "idx_travel_file_file_id",   columnList = "file_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelFileEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false,
                foreignKey = @ForeignKey(name = "fk_travel_file_travel"))
    private TravelEty travel;

    @Column(name = "file_id", nullable = false, length = 500)
    private String fileId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    /** URL pubblico/signed del file (es. Firebase Storage) */
    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @PrePersist
    protected void onCreate() {
        if (uploadDate == null) {
            uploadDate = LocalDateTime.now();
        }
    }
}