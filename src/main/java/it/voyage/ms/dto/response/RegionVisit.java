package it.voyage.ms.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class RegionVisit {

	 private String id;
     private String name;
     private CoordsDto coord;
     private List<DailyItineraryDTO> itinerary;
}
