package it.voyage.ms.service.impl;

import it.voyage.ms.dto.request.OptimizationRequest;
import it.voyage.ms.dto.response.OptimizationResult;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.enums.OptimizationScope;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.mapper.TravelMapper;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IGroupTravelService;
import it.voyage.ms.service.IItineraryOptimizerService;
import it.voyage.ms.service.ITravelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static it.voyage.ms.config.Constants.Optimizer.OPTIMIZER_MAX_POINTS_PER_DAY;

/**
 * Servizio per l'ottimizzazione dell'itinerario
 * 
 * Implementa l'algoritmo Nearest Neighbor per minimizzare la distanza percorsa
 * Complessità O(n²), adeguata per n ≤ 20 punti per giorno
 */
@Service
@Slf4j
@AllArgsConstructor
public class ItineraryOptimizerService implements IItineraryOptimizerService {

    private final TravelRepository travelRepository;
    private final IGroupTravelService groupTravelService;
    private final ITravelService travelService;
    private final TravelMapper travelMapper;

    /**
     * Ottimizza i punti di un itinerario minimizzando la distanza percorsa
     */
    @Override
    @Transactional
    public OptimizationResult optimize(Long travelId, OptimizationRequest request, String userId) {
        
        log.info("Ottimizzazione itinerario per travelId={}, scope={}, dayNumber={}, applyChanges={}", 
            travelId, request.getScope(), request.getDayNumber(), request.getApplyChanges());

        // 1. Verifica permessi
        if (!groupTravelService.canUserEditTravel(travelId, userId)) {
            log.warn("Utente {} non autorizzato a modificare il viaggio {}", userId, travelId);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Non autorizzato a modificare questo viaggio");
        }

        // 2. Carica viaggio
        TravelEty travel = travelRepository.findById(travelId)
            .orElseThrow(() -> new NotFoundException("Viaggio non trovato"));

        // 3. Valida scope
        if (request.getScope() == OptimizationScope.SINGLE_DAY) {
            if (request.getDayNumber() == null) {
                throw new IllegalArgumentException("dayNumber è obbligatorio quando scope=SINGLE_DAY");
            }
            boolean dayExists = travel.getItinerary().stream()
                .anyMatch(d -> d.getDay().equals(request.getDayNumber()));
            if (!dayExists) {
                throw new NotFoundException("Giorno " + request.getDayNumber() + " non trovato");
            }
        }

        // 4. Determina i giorni da ottimizzare
        List<DailyItineraryEty> daysToOptimize = getDaysToOptimize(travel, request);
        
        // 5. Calcola distanza totale prima dell'ottimizzazione
        double totalDistanceBefore = calculateTotalDistance(daysToOptimize);
        log.debug("Distanza totale prima: {} km", totalDistanceBefore);

        // 6. Applica algoritmo Nearest Neighbor a ciascun giorno
        for (DailyItineraryEty day : daysToOptimize) {
            if (day.getPoints().size() > OPTIMIZER_MAX_POINTS_PER_DAY) {
                log.warn("Giorno {} ha {} punti (> {}), l'ottimizzazione potrebbe richiedere tempo", 
                    day.getDay(), day.getPoints().size(), OPTIMIZER_MAX_POINTS_PER_DAY);
            }
            optimizeDay(day);
        }

        // 7. Calcola distanza totale dopo l'ottimizzazione
        double totalDistanceAfter = calculateTotalDistance(daysToOptimize);
        log.debug("Distanza totale dopo: {} km", totalDistanceAfter);

        // 8. Salva o costruisci DTO
        TravelDTO travelDTO;
        boolean applied = Boolean.TRUE.equals(request.getApplyChanges());
        
        if (applied) {
            // Salva sul DB e costruisci DTO completo
            TravelEty savedTravel = travelRepository.save(travel);
//            travelDTO = travelService.getTravelWithUrls(userId, savedTravel.getId());
            log.info("Ottimizzazione applicata e salvata sul DB");
        } else {
            // Solo anteprima, non salvare
            travelDTO = travelMapper.convertEtyToDTO(travel);
            log.info("Ottimizzazione calcolata (solo anteprima, non salvata)");
        }

        // 9. Costruisci risultato
        OptimizationResult result = new OptimizationResult();
//        result.setTravel(travelDTO);
        result.setScope(request.getScope());
        result.setDayOptimized(request.getScope() == OptimizationScope.SINGLE_DAY ? request.getDayNumber() : null);
        result.setTotalDistanceBefore(totalDistanceBefore);
        result.setTotalDistanceAfter(totalDistanceAfter);
        result.setApplied(applied);

        double improvement = totalDistanceBefore - totalDistanceAfter;
        double improvementPercent = totalDistanceBefore > 0 ? (improvement / totalDistanceBefore * 100) : 0;
        log.info("Ottimizzazione completata: -{} km ({:.1f}%)", improvement, improvementPercent);

        return result;
    }

    /**
     * Determina quali giorni ottimizzare in base allo scope
     */
    private List<DailyItineraryEty> getDaysToOptimize(TravelEty travel, OptimizationRequest request) {
        if (request.getScope() == OptimizationScope.SINGLE_DAY) {
            return travel.getItinerary().stream()
                .filter(d -> d.getDay().equals(request.getDayNumber()))
                .collect(Collectors.toList());
        } else {
            return new ArrayList<>(travel.getItinerary());
        }
    }

    /**
     * Calcola la distanza totale percorsa nei giorni specificati
     * Somma le distanze tra punti consecutivi usando Haversine
     */
    private double calculateTotalDistance(List<DailyItineraryEty> days) {
        double total = 0.0;
        
        for (DailyItineraryEty day : days) {
            List<PointEty> points = day.getPoints().stream()
                .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
                .collect(Collectors.toList());
            
            for (int i = 0; i < points.size() - 1; i++) {
                PointEty p1 = points.get(i);
                PointEty p2 = points.get(i + 1);
                total += haversineKm(p1.getLatitude(), p1.getLongitude(), p2.getLatitude(), p2.getLongitude());
            }
        }
        
        return total;
    }

    /**
     * Ottimizza un singolo giorno usando l'algoritmo Nearest Neighbor
     * Modifica direttamente gli orderIndex dei punti
     */
    private static void optimizeDay(DailyItineraryEty day) {
        List<PointEty> points = new ArrayList<>(day.getPoints());
        
        // 1. Separa punti con e senza coordinate
        List<PointEty> validPoints = points.stream()
            .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
            .collect(Collectors.toList());
        
        List<PointEty> invalidPoints = points.stream()
            .filter(p -> p.getLatitude() == null || p.getLongitude() == null)
            .sorted((a, b) -> Integer.compare(a.getOrderIndex(), b.getOrderIndex()))
            .collect(Collectors.toList());
        
        // 2. Se ci sono 0 o 1 punti validi, non c'è nulla da ottimizzare
        if (validPoints.size() <= 1) {
            log.debug("Giorno {} ha {} punti validi, skip ottimizzazione", day.getDay(), validPoints.size());
            return;
        }
        
        // 3. Applica algoritmo Nearest Neighbor
        List<PointEty> optimizedRoute = nearestNeighborRoute(validPoints);
        
        // 4. Aggiorna orderIndex: prima i punti ottimizzati, poi quelli senza coordinate
        int index = 0;
        for (PointEty point : optimizedRoute) {
            point.setOrderIndex(index++);
        }
        for (PointEty point : invalidPoints) {
            point.setOrderIndex(index++);
        }
        
        log.debug("Giorno {} ottimizzato: {} punti validi, {} senza coordinate", 
            day.getDay(), optimizedRoute.size(), invalidPoints.size());
    }

    /**
     * Algoritmo Nearest Neighbor per trovare un percorso ottimizzato
     * Complessità O(n²)
     * 
     * @param points lista di punti con coordinate valide
     * @return lista ordinata per minimizzare la distanza
     */
    private static List<PointEty> nearestNeighborRoute(List<PointEty> points) {
        if (points.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<PointEty> route = new ArrayList<>();
        Set<PointEty> visited = new HashSet<>();
        
        // Inizia dal primo punto
        PointEty current = points.get(0);
        route.add(current);
        visited.add(current);
        
        // Finché ci sono punti non visitati
        while (route.size() < points.size()) {
            PointEty nearest = null;
            double minDistance = Double.MAX_VALUE;
            
            // Trova il punto non visitato più vicino
            for (PointEty candidate : points) {
                if (visited.contains(candidate)) {
                    continue;
                }
                
                double distance = haversineKm(
                    current.getLatitude(), current.getLongitude(),
                    candidate.getLatitude(), candidate.getLongitude()
                );
                
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = candidate;
                }
            }
            
            // Aggiungi il punto più vicino al percorso
            if (nearest != null) {
                route.add(nearest);
                visited.add(nearest);
                current = nearest;
            } else {
                break; // Non dovrebbe mai accadere
            }
        }
        
        return route;
    }

    /**
     * Calcola la distanza in chilometri tra due coordinate usando la formula di Haversine
     * 
     * @param lat1 latitudine punto 1 in gradi
     * @param lon1 longitudine punto 1 in gradi
     * @param lat2 latitudine punto 2 in gradi
     * @param lon2 longitudine punto 2 in gradi
     * @return distanza in chilometri
     */
    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Raggio della Terra in km
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}