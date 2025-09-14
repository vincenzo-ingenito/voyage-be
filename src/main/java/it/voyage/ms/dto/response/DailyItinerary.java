package it.voyage.ms.dto.response;

import java.util.List;

import lombok.Data;

@Data
public class DailyItinerary {
    private int day;
    private String date;
    private List<PointOfInterest> points;
}
