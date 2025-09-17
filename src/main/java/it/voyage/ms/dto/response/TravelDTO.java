package it.voyage.ms.dto.response;

import java.util.List;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class TravelDTO {
    private String travelName; // Un nome per il viaggio (es. "Vacanza in Italia")
    private List<DailyItineraryDTO> itinerary;

    // Getters and Setters
    public String getTravelName() {
        return travelName;
    }

    public void setTravelName(String travelName) {
        this.travelName = travelName;
    }

    public List<DailyItineraryDTO> getItinerary() {
        return itinerary;
    }

    public void setItinerary(List<DailyItineraryDTO> itinerary) {
        this.itinerary = itinerary;
    }
}