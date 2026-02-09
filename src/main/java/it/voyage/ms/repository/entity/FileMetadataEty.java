package it.voyage.ms.repository.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * Entità che rappresenta i metadati dei file associati a un viaggio
 * Relation: Travel 1:N FileMetadata
 */
@Entity
@Table(name = "file_metadata", indexes = {
    @Index(name = "idx_file_travel_id", columnList = "travel_id"),
    @Index(name = "idx_file_file_id", columnList = "file_id"),
    @Index(name = "idx_file_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadataEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * ID del file (riferimento esterno, es. Firebase Storage)
     */
    @Column(name = "file_id", nullable = false, length = 500)
    private String fileId;

    /**
     * Nome originale del file
     */
    @Column(name = "file_name", length = 500)
    private String fileName;

    /**
     * Tipo MIME del file
     */
    @Column(name = "mime_type", length = 100)
    private String mimeType;

    /**
     * Dimensione del file in bytes
     */
    @Column(name = "file_size")
    private Long fileSize;

    /**
     * URL pubblico del file
     */
    @Column(name = "file_url", length = 1000)
    private String fileUrl;

    /**
     * Data di creazione del record
     */
    @Column(name = "created_at", nullable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * Relation: Travel 1:N FileMetadata
     * Il viaggio a cui appartiene questo file
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_id", nullable = false, foreignKey = @ForeignKey(name = "fk_file_travel"))
    private TravelEty travel;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }

    /**
     * Converte l'entità in DTO
     */
    @Transient
    public it.voyage.ms.dto.response.FileMetadata toDTO() {
        it.voyage.ms.dto.response.FileMetadata dto = new it.voyage.ms.dto.response.FileMetadata();
        dto.setFileId(this.fileId);
        dto.setFileName(this.fileName);
        dto.setMimeType(this.mimeType);
        return dto;
    }

    /**
     * Crea una entity da un DTO
     */
    public static FileMetadataEty fromDTO(it.voyage.ms.dto.response.FileMetadata dto, TravelEty travel) {
        FileMetadataEty entity = new FileMetadataEty();
        entity.setFileId(dto.getFileId());
        entity.setFileName(dto.getFileName());
        entity.setMimeType(dto.getMimeType());
        entity.setTravel(travel);
        entity.setCreatedAt(new Date());
        return entity;
    }
}
