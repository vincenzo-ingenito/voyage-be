package it.voyage.ms.repository.entity;

import java.util.List;

import it.voyage.ms.dto.response.PointDTO;
import lombok.Data;

@Data
public class DailyItinerary {
    private String id;
    private int day;
    private String date;
    private List<PointDTO> points;

}