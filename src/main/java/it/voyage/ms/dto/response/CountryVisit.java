package it.voyage.ms.dto.response;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import it.voyage.ms.repository.entity.TravelEty;
import lombok.Data;

@Data
public class CountryVisit {
    private String iso;
    private String name;
    private Set<String> visitedDates;
    private CoordsDto coord;
    private List<RegionVisit> regions;
    
    public static CountryVisit mapToCountryVisit(TravelEty travel) {
        if (travel == null || travel.getItinerary() == null) {
            return null;
        }

        CountryVisit cv = new CountryVisit();
        cv.setIso(travel.getId());          // oppure iso da travelName/userId se hai mapping
        cv.setName(travel.getTravelName());

        // tutte le date visitate
        Set<String> visitedDates = travel.getItinerary().stream()
                .map(DailyItineraryDTO::getDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        cv.setVisitedDates(visitedDates);

        // coord principale: prima disponibile
        cv.setCoord(
            travel.getItinerary().stream()
                .flatMap(di -> di.getPoints().stream())
                .map(PointDTO::getCoord)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null)
        );

        // Raggruppo per regione
        Map<String, List<PointDTO>> pointsByRegion = travel.getItinerary().stream()
                .flatMap(di -> di.getPoints().stream())
                .collect(Collectors.groupingBy(PointDTO::getRegion));

        List<RegionVisit> regions = new ArrayList<>();
        for (Map.Entry<String, List<PointDTO>> regionEntry : pointsByRegion.entrySet()) {
            String regionName = regionEntry.getKey();
            List<PointDTO> regionPoints = regionEntry.getValue();

            RegionVisit rv = new RegionVisit();
            rv.setId(UUID.randomUUID().toString());
            rv.setName(regionName);
            rv.setCoord(regionPoints.stream()
                    .map(PointDTO::getCoord)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null));

            // ricostruisco gli itinerari filtrati per la regione
            List<DailyItineraryDTO> regionItinerary = travel.getItinerary().stream()
                    .map(di -> {
                        List<PointDTO> filtered = di.getPoints().stream()
                                .filter(p -> regionName.equals(p.getRegion()))
                                .collect(Collectors.toList());
                        if (filtered.isEmpty()) return null;

                        DailyItineraryDTO newDi = new DailyItineraryDTO();
                        newDi.setDay(di.getDay());
                        newDi.setDate(di.getDate());
                        newDi.setPoints(filtered);
                        return newDi;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            rv.setItinerary(regionItinerary);
            regions.add(rv);
        }

        cv.setRegions(regions);

        return cv;
    }


}

