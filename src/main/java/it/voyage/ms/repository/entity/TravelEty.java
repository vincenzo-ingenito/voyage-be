package it.voyage.ms.repository.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.voyage.ms.dto.response.FileMetadata;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "travel")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelEty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "travel_name")
    private String travelName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id",
                foreignKey = @ForeignKey(name = "fk_travel_user"))
    private UserEty user;

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("day ASC")
    private List<DailyItineraryEty> itinerary = new ArrayList<>();

    @Column(name = "date_from")
    private String dateFrom;

    @Column(name = "date_to")
    private String dateTo;

    /**
     * Unica relazione per i file allegati al viaggio.
     * FileMetadataEty è stata rimossa: TravelFileEty è l'unica fonte di verità.
     */
    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("uploadDate ASC")
    private List<TravelFileEty> files = new ArrayList<>();

    @Column(name = "is_copied")
    private Boolean isCopied;

    @Column(name = "needs_date_confirmation")
    private Boolean needsDateConfirmation;

    // -------------------------------------------------------------------------
    // Helper methods — NON annotati @Transient: quella annotazione vale solo
    // per i campi persistiti, non per i metodi di utilità.
    // -------------------------------------------------------------------------

    public List<String> getAllFileIds() {
        return files.stream()
                .map(TravelFileEty::getFileId)
                .collect(Collectors.toList());
    }

    public List<FileMetadata> getFileMetadataList() {
        return files.stream()
                .map(f -> new FileMetadata(f.getFileId(), f.getFileName(), f.getMimeType()))
                .collect(Collectors.toList());
    }

    /**
     * Sostituisce la lista dei file a partire dai soli fileId.
     * I metadati (nome, mime, url) vanno impostati separatamente se disponibili.
     */
    public void setAllFileIds(List<String> fileIds) {
        this.files.clear();
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        for (String fileId : fileIds) {
            TravelFileEty fileEty = new TravelFileEty();
            fileEty.setFileId(fileId);
            fileEty.setTravel(this);
            this.files.add(fileEty);
        }
    }

    /**
     * Sostituisce la lista dei file a partire dai DTO FileMetadata.
     */
    public void setFileMetadataList(List<FileMetadata> metadataList) {
        this.files.clear();
        if (metadataList == null || metadataList.isEmpty()) {
            return;
        }
        for (FileMetadata metadata : metadataList) {
            TravelFileEty fileEty = new TravelFileEty();
            fileEty.setFileId(metadata.getFileId());
            fileEty.setFileName(metadata.getFileName());
            fileEty.setMimeType(metadata.getMimeType());
            fileEty.setTravel(this);
            this.files.add(fileEty);
        }
    }
}