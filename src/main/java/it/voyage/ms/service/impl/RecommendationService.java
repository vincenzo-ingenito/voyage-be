package it.voyage.ms.service.impl;

import it.voyage.ms.dto.response.NearbyPlaceDTO;
import it.voyage.ms.dto.response.NearbyRecommendationResponse;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IGroupTravelService;
import it.voyage.ms.service.IPlacesService;
import it.voyage.ms.service.IRecommendationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.voyage.ms.config.Constants.Google.NEARBY_DEFAULT_RADIUS;
import static it.voyage.ms.config.Constants.Google.NEARBY_MAX_RADIUS;

/**
 * Servizio per raccomandazioni di luoghi vicini
 * 
 * Funzionalità:
 * - Ricerca luoghi nelle vicinanze di un punto dell'itinerario
 * - Filtra duplicati già presenti nel viaggio
 * - Calcola distanze con formula Haversine
 * - Ordina per distanza crescente
 */
@Service
@Slf4j
@AllArgsConstructor
public class RecommendationService implements IRecommendationService {

    private final TravelRepository travelRepository;
    private final IGroupTravelService groupTravelService;
    private final IPlacesService placesService;

    /**
     * Ottiene raccomandazioni di luoghi vicini a un punto dell'itinerario
     */
    @Override
    @Transactional(readOnly = true)
    public NearbyRecommendationResponse getNearbyRecommendations(
            Long travelId,
            Integer dayNumber,
            Long pointId,
            Integer radius,
            String type,
            String userId) {

        log.info("Ricerca luoghi vicini per travelId={}, day={}, pointId={}, radius={}, type={}", 
            travelId, dayNumber, pointId, radius, type);

        // 1. Verifica permessi
        if (!groupTravelService.canUserViewTravel(travelId, userId)) {
            log.warn("Utente {} non autorizzato a visualizzare il viaggio {}", userId, travelId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a visualizzare questo viaggio");
        }

        // 2. Carica viaggio
        TravelEty travel = travelRepository.findById(travelId)
            .orElseThrow(() -> new NotFoundException("Viaggio non trovato"));

        // 3. Trova il giorno
        DailyItineraryEty day = travel.getItinerary().stream()
            .filter(d -> d.getDay().equals(dayNumber))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Giorno " + dayNumber + " non trovato"));

        // 4. Trova il punto
        PointEty referencePoint = day.getPoints().stream()
            .filter(p -> p.getId().equals(pointId))
            .findFirst()
            .orElseThrow(() -> new NotFoundException("Punto non trovato"));

        // 5. Verifica coordinate
        if (referencePoint.getLatitude() == null || referencePoint.getLongitude() == null) {
            log.warn("Punto {} senza coordinate", pointId);
            throw new BusinessException("Punto senza coordinate: impossibile cercare luoghi vicini");
        }

        // 6. Raccogli tutti i placeId già presenti nel viaggio
        Set<String> existingPlaceIds = collectExistingPlaceIds(travel);
        log.debug("Trovati {} luoghi già presenti nel viaggio", existingPlaceIds.size());

        // 7. Normalizza il raggio
        int effectiveRadius = radius != null ? Math.min(radius, NEARBY_MAX_RADIUS) : NEARBY_DEFAULT_RADIUS;

        // 8. Chiama PlacesService per ottenere luoghi vicini
        String effectiveType = type != null && !type.trim().isEmpty() ? type : "tourist_attraction";
        List<NearbyPlaceDTO> nearbyPlaces = placesService.getNearbyPlaces(
            referencePoint.getLatitude(),
            referencePoint.getLongitude(),
            effectiveRadius,
            effectiveType
        );

        log.debug("Ricevuti {} luoghi dall'API Google Places", nearbyPlaces.size());

        // 9. Filtra duplicati e calcola distanze
        List<NearbyPlaceDTO> filteredPlaces = nearbyPlaces.stream()
            .filter(place -> !existingPlaceIds.contains(place.getPlaceId()))
            .peek(place -> {
                // Calcola distanza con Haversine
                int distance = calculateDistanceMeters(
                    referencePoint.getLatitude(),
                    referencePoint.getLongitude(),
                    place.getLatitude(),
                    place.getLongitude()
                );
                place.setDistanceMeters(distance);
                place.setAlreadyInTravel(false);
            })
            .sorted(Comparator.comparingInt(NearbyPlaceDTO::getDistanceMeters))
            .collect(Collectors.toList());

        log.info("Restituiti {} luoghi dopo filtraggio e ordinamento", filteredPlaces.size());

        // 10. Costruisci risposta
        return new NearbyRecommendationResponse(
            filteredPlaces,
            referencePoint.getName(),
            referencePoint.getLatitude(),
            referencePoint.getLongitude(),
            effectiveRadius,
            filteredPlaces.size()
        );
    }

    /**
     * Raccoglie tutti i placeId già presenti nel viaggio (tutti i giorni, tutti i punti)
     */
    private Set<String> collectExistingPlaceIds(TravelEty travel) {
        Set<String> placeIds = new HashSet<>();
        
        for (DailyItineraryEty day : travel.getItinerary()) {
            for (PointEty point : day.getPoints()) {
                if (point.getPlaceId() != null && !point.getPlaceId().trim().isEmpty()) {
                    placeIds.add(point.getPlaceId());
                }
            }
        }
        
        return placeIds;
    }

    /**
     * Calcola la distanza in metri tra due coordinate usando la formula Haversine
     * 
     * @param lat1 latitudine punto 1
     * @param lon1 longitudine punto 1
     * @param lat2 latitudine punto 2
     * @param lon2 longitudine punto 2
     * @return distanza in metri
     */
    private int calculateDistanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000; // Raggio della Terra in metri
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return (int) Math.round(R * c);
    }
}