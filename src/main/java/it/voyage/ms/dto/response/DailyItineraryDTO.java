package it.voyage.ms.dto.response;

//DailyItineraryDTO
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DailyItineraryDTO {
    private int day;
    private String date;
    private List<PointDTO> points;

    // Getters and Setters...
}