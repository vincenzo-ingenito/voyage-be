package it.voyage.ms.dto.response;

import java.util.List;
import java.util.Set;

import lombok.Data;

@Data
public class CountryVisit {
    private String iso;
    private String name;
    private Set<String> visitedDates;
    private Coords coord;
    private List<RegionVisit> regions;
}

