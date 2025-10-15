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
	 
}