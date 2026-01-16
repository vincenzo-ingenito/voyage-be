package it.voyage.ms.repository.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.FileMetadata;
import lombok.Data;

@Document(collection = "travels")
@Data
public class TravelEty {
	@Id
	private String id;
	private String travelName;
	private String userId;
	private List<DailyItineraryDTO> itinerary;
	private String dateFrom; 
	private String dateTo;   
	private String coverImageUri;
	
	// ✅ FIX: Manteniamo allFileIds per compatibilità con codice esistente
	// ma aggiungiamo fileMetadataList per tracciare i metadati completi
	private List<String> allFileIds; 
	private List<FileMetadata> fileMetadataList; // Metadati completi dei file (fileName, mimeType)
	
	private Boolean isCopied; // Indica se il viaggio è stato copiato da un altro utente
	private Boolean needsDateConfirmation; // Indica se le date devono essere confermate/aggiornate

}
