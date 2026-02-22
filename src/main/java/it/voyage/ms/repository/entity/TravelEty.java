package it.voyage.ms.repository.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import it.voyage.ms.dto.response.FileMetadata;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
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
    private Long id;
    
    @Column(name = "travel_name")
    private String travelName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, referencedColumnName = "id")
    private UserEty user;
    
    // Relazione 1:N con DailyItinerary
    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("day ASC")
    private List<DailyItineraryEty> itinerary = new ArrayList<>();

    @Column(name = "date_from")
    private String dateFrom;

    @Column(name = "date_to")
    private String dateTo;

    @OneToMany(mappedBy = "travel", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TravelFileEty> files = new ArrayList<>();
    
    @Column(name = "is_copied")
    private Boolean isCopied;

    @Column(name = "needs_date_confirmation")
    private Boolean needsDateConfirmation;
    
    @Transient
    public List<String> getAllFileIds() {
        return files.stream()
                .map(TravelFileEty::getFileId)
                .collect(Collectors.toList());
    }

    @Transient
    public List<FileMetadata> getFileMetadataList() {
        return files.stream()
                .map(f -> new FileMetadata(f.getFileId(),f.getFileName(), f.getMimeType()))
                .collect(Collectors.toList());
    }
    
    @Transient
    public void setAllFileIds(List<String> fileIds) {
        // Questo metodo ricrea la lista di TravelFileEty basandosi sui fileIds
        // NOTA: Questo mantiene solo gli ID, i metadati vanno persi
        if (fileIds == null || fileIds.isEmpty()) {
            this.files.clear();
            return;
        }
        
        this.files.clear();
        for (String fileId : fileIds) {
            TravelFileEty fileEty = new TravelFileEty();
            fileEty.setFileId(fileId);
            fileEty.setTravel(this);
            this.files.add(fileEty);
        }
    }
    
    @Transient
    public void setFileMetadataList(List<FileMetadataEty> metadataList) {
        // Questo metodo ricrea la lista di TravelFileEty dai metadati
        if (metadataList == null || metadataList.isEmpty()) {
            this.files.clear();
            return;
        }
        
        this.files.clear();
        for (FileMetadataEty metadata : metadataList) {
            TravelFileEty fileEty = new TravelFileEty();
            fileEty.setFileId(metadata.getFileId());
            fileEty.setFileName(metadata.getFileName());
            fileEty.setMimeType(metadata.getMimeType());
            fileEty.setTravel(this);
            this.files.add(fileEty);
        }
    }
         
}
