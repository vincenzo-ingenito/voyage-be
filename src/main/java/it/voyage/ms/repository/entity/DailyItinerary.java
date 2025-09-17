package it.voyage.ms.repository.entity;

import java.util.List;

import lombok.Data;

@Data
public class DailyItinerary {
    private String id;
    private int day;
    private String date;
    private List<Point> points;

    // Getters and Setters...
}