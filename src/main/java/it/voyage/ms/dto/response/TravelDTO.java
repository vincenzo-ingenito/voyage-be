package it.voyage.ms.dto.response;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class TravelDTO {
	private String travelId;
	private String travelName; 
	private List<DailyItineraryDTO> itinerary;
	private String dateFrom; 
	private String dateTo;
	private Boolean isCopied; // Indica se il viaggio è stato copiato da un altro utente
	private Boolean needsDateConfirmation; // Indica se le date devono essere confermate/aggiornate
	 
}