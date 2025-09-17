package it.voyage.ms.dto.response;

import java.util.List;

import it.voyage.ms.repository.entity.Point;
import lombok.Data;

@Data
public class DailyItinerary {
    private int day;
    private String date;
    private List<PointOfInterest> points;
    private List<Point> pointss;
}
