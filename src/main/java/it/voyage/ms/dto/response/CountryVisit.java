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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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

        // Estrai tutti i punti dall'itinerario (lavorando con Entity)
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

        // Nome paese dal primo punto disponibile
        String countryName = firstPoint
                .map(PointEty::getCountry)
                .filter(c -> c != null && !c.isEmpty())
                .orElse("Nazione Sconosciuta");

        String countryIdentifier = countryName.replaceAll("\\s", "_").toUpperCase();

        cv.setIso(countryIdentifier);
        cv.setName(countryName);

        // Tutte le date visitate
        Set<String> visitedDates = travel.getItinerary().stream()
                .map(DailyItineraryEty::getDate)
                .filter(Objects::nonNull)
                .filter(date -> !date.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        cv.setVisitedDates(visitedDates);

        // Coordinate principali: prima disponibile
        CoordsDto mainCoord = firstPoint
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .map(p -> {
                    CoordsDto coords = new CoordsDto(p.getLatitude(),p.getLongitude());
                    return coords;
                })
                .orElse(null);
        cv.setCoord(mainCoord);

        // Raggruppa per regione
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

            // Coordinate della regione: prima disponibile
            CoordsDto regionCoord = regionPoints.stream()
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .findFirst()
                    .map(p -> {
                        CoordsDto coords = new CoordsDto(p.getLatitude(),p.getLongitude());
                        return coords;
                    })
                    .orElse(null);
            rv.setCoord(regionCoord);

            // Ricostruisci gli itinerari filtrati per la regione (convertendo in DTO)
            List<DailyItineraryDTO> regionItinerary = travel.getItinerary().stream()
                    .map(dailyEty -> {
                        // Filtra i punti che appartengono a questa regione
                        List<PointDTO> filteredPoints = dailyEty.getPoints().stream()
                                .filter(p -> regionName.equals(p.getRegion()))
                                .map(CountryVisit::convertPointEtyToDTO)
                                .collect(Collectors.toList());

                        if (filteredPoints.isEmpty()) {
                            return null;
                        }

                        // Crea nuovo DailyItineraryDTO con solo i punti della regione
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

    /**
     * Metodo helper per convertire PointEty in PointDTO
     */
    private static PointDTO convertPointEtyToDTO(PointEty pointEty) {
        PointDTO dto = new PointDTO();
        dto.setName(pointEty.getName());
        dto.setType(pointEty.getType());
        dto.setDescription(pointEty.getDescription());
        dto.setCost(pointEty.getCost());
        dto.setCountry(pointEty.getCountry());
        dto.setRegion(pointEty.getRegion());
        dto.setCity(pointEty.getCity());

        // Coordinate
        if (pointEty.getLatitude() != null && pointEty.getLongitude() != null) {
            CoordsDto coords = new CoordsDto(pointEty.getLatitude(),pointEty.getLongitude());
            dto.setCoord(coords);
        }

        // Gestione attachment indices da JSON
        if (pointEty.getAttachmentIndicesJson() != null && !pointEty.getAttachmentIndicesJson().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                List<Integer> indices = mapper.readValue(
                    pointEty.getAttachmentIndicesJson(),
                    new TypeReference<List<Integer>>() {}
                );
                dto.setAttachmentIndices(indices);
            } catch (JsonProcessingException e) {
                dto.setAttachmentIndices(Collections.emptyList());
            }
        } else {
            dto.setAttachmentIndices(Collections.emptyList());
        }

        // AttachmentUrls: lista vuota per ora (puoi implementare la logica completa se necessario)
        dto.setAttachmentUrls(Collections.emptyList());

        return dto;
    }
}
