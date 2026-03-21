package it.voyage.ms.dto.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
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
        if (travel == null || travel.getItinerary() == null || travel.getItinerary().isEmpty()) {
            return null;
        }

        List<PointEty> allPoints = travel.getItinerary().stream()
                .filter(Objects::nonNull)
                .flatMap(di -> di.getPoints().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (allPoints.isEmpty()) {
            return null;
        }

        Optional<PointEty> firstPoint = allPoints.stream().findFirst();

        CountryVisit cv = new CountryVisit();

        String countryName = firstPoint
                .map(PointEty::getCountry)
                .filter(c -> c != null && !c.isEmpty())
                .orElse("Nazione Sconosciuta");

        cv.setIso(countryName.replaceAll("\\s", "_").toUpperCase());
        cv.setName(countryName);

        Set<String> visitedDates = travel.getItinerary().stream()
                .map(DailyItineraryEty::getDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        cv.setVisitedDates(visitedDates);

        cv.setCoord(firstPoint
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .map(p -> new CoordsDto(p.getLatitude(), p.getLongitude()))
                .orElse(null));

        Map<String, List<PointEty>> pointsByRegion = allPoints.stream()
                .filter(p -> p.getRegion() != null && !p.getRegion().isEmpty())
                .collect(Collectors.groupingBy(PointEty::getRegion));

        List<RegionVisit> regions = new ArrayList<>();

        for (Map.Entry<String, List<PointEty>> regionEntry : pointsByRegion.entrySet()) {
            String regionName = regionEntry.getKey();
            List<PointEty> regionPoints = regionEntry.getValue();

            RegionVisit rv = new RegionVisit();
            rv.setId(UUID.randomUUID().toString());
            rv.setName(regionName);

            rv.setCoord(regionPoints.stream()
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .findFirst()
                    .map(p -> new CoordsDto(p.getLatitude(), p.getLongitude()))
                    .orElse(null));

            List<DailyItineraryDTO> regionItinerary = travel.getItinerary().stream()
                    .map(dailyEty -> {
                        List<PointDTO> filteredPoints = dailyEty.getPoints().stream()
                                .filter(p -> regionName.equals(p.getRegion()))
                                .map(CountryVisit::convertPointEtyToDTO)
                                .collect(Collectors.toList());

                        if (filteredPoints.isEmpty()) {
                            return null;
                        }

                        DailyItineraryDTO newDi = new DailyItineraryDTO();
                        newDi.setDay(dailyEty.getDay());
                        newDi.setDate(dailyEty.getDate());
                        newDi.setMemoryImageIndex(dailyEty.getMemoryImageIndex());
                        newDi.setMemoryImageUrl(dailyEty.getMemoryImageUrl());
                        newDi.setPoints(filteredPoints);
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

    private static PointDTO convertPointEtyToDTO(PointEty pointEty) {
        PointDTO dto = new PointDTO();
        dto.setName(pointEty.getName());
        dto.setType(pointEty.getType());
        dto.setDescription(pointEty.getDescription());
        dto.setCost(pointEty.getCost());
        dto.setCountry(pointEty.getCountry());
        dto.setRegion(pointEty.getRegion());
        dto.setCity(pointEty.getCity());

        if (pointEty.getLatitude() != null && pointEty.getLongitude() != null) {
            dto.setCoord(new CoordsDto(pointEty.getLatitude(), pointEty.getLongitude()));
        }

        // attachmentIndices è già List<Integer> grazie all'AttributeConverter in PointEty
        List<Integer> indices = pointEty.getAttachmentIndices();
        dto.setAttachmentIndices(indices != null ? indices : Collections.emptyList());
        dto.setAttachmentUrls(Collections.emptyList());

        return dto;
    }
}