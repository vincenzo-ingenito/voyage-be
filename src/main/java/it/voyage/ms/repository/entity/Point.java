package it.voyage.ms.repository.entity;

import lombok.Data;

@Data
public class Point {
    private String name;
    private String type;
    private String description;
    private String cost;
    private double lat;
    private double lng;
    private String country;
    private String region;
    private String city;

    // Getters and Setters...
}