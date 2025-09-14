package it.voyage.ms.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class RegionVisit {

	 private String id;
     private String name;
     private Coords coord;
     private List<DailyItinerary> itinerary;
}
