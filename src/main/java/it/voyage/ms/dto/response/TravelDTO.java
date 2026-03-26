package it.voyage.ms.dto.response;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import it.voyage.ms.repository.entity.TravelType;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TravelDTO {
	private Long travelId;
	private String travelName;
	private List<DailyItineraryDTO> itinerary;
	private String dateFrom; 
	private String dateTo;
	private Boolean isCopied; 
	private Boolean needsDateConfirmation;
	
	/**
	 * Tipo di viaggio: SINGLE o GROUP
	 */
	private TravelType travelType;
	
	/**
	 * Lista dei partecipanti (solo per viaggi di gruppo)
	 */
	private List<ParticipantDTO> participants = new ArrayList<>();
	
	/**
	 * ID del proprietario del viaggio (Firebase UID)
	 */
	private String ownerId;
	
	/**
	 * Nome del proprietario del viaggio
	 */
	private String ownerName;
	
	/**
	 * Email del proprietario del viaggio
	 */
	private String ownerEmail;
}
