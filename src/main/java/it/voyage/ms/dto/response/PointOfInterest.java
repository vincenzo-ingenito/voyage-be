package it.voyage.ms.dto.response;

import lombok.Data;

@Data
public class PointOfInterest {

    private String name;
    private String type;
    private String description;
    private Coords coord;
}
