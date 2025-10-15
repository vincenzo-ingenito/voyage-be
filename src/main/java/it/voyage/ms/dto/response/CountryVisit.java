package it.voyage.ms.dto.response;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

		List<PointDTO> allPoints = travel.getItinerary().stream()
				.flatMap(di -> di.getPoints().stream())
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		if (allPoints.isEmpty()) {
			return null;
		}

		Optional<PointDTO> firstPoint = allPoints.stream().findFirst();

		CountryVisit cv = new CountryVisit();

		String countryName = firstPoint.map(PointDTO::getCountry).orElse("Nazione Sconosciuta");

		String countryIdentifier = countryName.replaceAll("\\s", "_").toUpperCase();

		cv.setIso(countryIdentifier); // Esempio: "FRANCIA", "STATI_UNITI"
		cv.setName(countryName);

		// tutte le date visitate (Logica invariata)
		Set<String> visitedDates = travel.getItinerary().stream()
				.map(DailyItineraryDTO::getDate)
				.filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
		cv.setVisitedDates(visitedDates);

		// coord principale: prima disponibile
		cv.setCoord(firstPoint.map(PointDTO::getCoord).orElse(null));

		// Raggruppo per regione (Logica invariata)
		Map<String, List<PointDTO>> pointsByRegion = allPoints.stream()
				.filter(p -> p.getRegion() != null) // Assicurati di scartare i punti senza regione se necessario
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

