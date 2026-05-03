package it.voyage.ms.dto.response;

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
	private String userId;
	private String city;
	private String country;
	private String coverImageUri;
	private List<DailyItineraryDTO> itinerary = new ArrayList<>();
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
	
	/**
	 * Avatar del proprietario del viaggio
	 */
	private String ownerAvatar;
	
	/**
	 * Statistiche di voto (upvotes, downvotes, punteggio netto)
	 * Popolato opzionalmente quando richiesto (es. nel feed)
	 */
	private VoteStatsDTO voteStats;
}