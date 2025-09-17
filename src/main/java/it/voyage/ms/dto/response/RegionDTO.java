package it.voyage.ms.dto.response;

//RegionDTO
import java.util.List;

public class RegionDTO {
 private String id;
 private String name;
 private Coords coord;
 private List<DailyItineraryDTO> itinerary;

 // Getters and Setters...
 public String getId() {
     return id;
 }

 public void setId(String id) {
     this.id = id;
 }

 public String getName() {
     return name;
 }

 public void setName(String name) {
     this.name = name;
 }

 public Coords getCoord() {
     return coord;
 }

 public void setCoord(Coords coord) {
     this.coord = coord;
 }

 public List<DailyItineraryDTO> getItinerary() {
     return itinerary;
 }

 public void setItinerary(List<DailyItineraryDTO> itinerary) {
     this.itinerary = itinerary;
 }
}