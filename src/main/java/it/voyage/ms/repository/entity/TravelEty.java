package it.voyage.ms.repository.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import it.voyage.ms.dto.response.DailyItineraryDTO;
import lombok.Data;

@Document(collection = "travels")
@Data
public class TravelEty {
    @Id
    private String id;
    private String travelName;
    private String userId;
    private List<DailyItineraryDTO> itinerary;

}