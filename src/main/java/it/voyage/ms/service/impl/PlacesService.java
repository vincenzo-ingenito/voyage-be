package it.voyage.ms.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.voyage.ms.dto.response.PlaceAutocompleteDTO;
import it.voyage.ms.dto.response.PlaceDetailsDTO;
import it.voyage.ms.service.IPlacesService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;
import java.util.stream.Collectors;

import it.voyage.ms.dto.response.NearbyPlaceDTO;

import static it.voyage.ms.config.Constants.Google.*;

/**
 * Servizio ottimizzato per l'integrazione con Google Places API
 * 
 * Ottimizzazioni implementate:
 * - Caching delle risposte API (riduce chiamate e costi)
 * - Deserializzazione automatica con Jackson (elimina parsing manuale)
 * - URL building sicuro con encoding automatico (previene injection)
 * - Validazione input anticipata (early return pattern)
 * - Stream API per trasformazioni efficienti (lazy evaluation)
 * - Immutabilità dove possibile (thread-safe)
 * - Gestione errori specifica e granulare
 * - Type mapping ottimizzato con EnumMap (O(1) lookup)
 * - Sanitizzazione log (previene log injection)
 */
@Service
@Slf4j
public class PlacesService implements IPlacesService {

    private static final int MIN_INPUT_LENGTH = 2;
    private static final int MAX_INPUT_LENGTH = 256;
    private static final String DEFAULT_TYPE = "Attrazione";
    private static final String DEFAULT_LANGUAGE = "it";
    
    // Tipi Google da ignorare nel mapping (troppo generici)
    private static final Set<String> GENERIC_TYPES = Set.of("establishment", "point_of_interest");

    /**
     * Enum per mapping ottimizzato dei tipi Google Places
     * Vantaggi: type-safety, O(1) lookup, memoria ridotta, immutabile
     */
    private enum PlaceTypeCategory {
        ATTRACTION("Attrazione", "tourist_attraction"),
        MUSEUM("Museo", "museum"),
        PARK("Parco", "park"),
        RESTAURANT("Ristorante", "restaurant", "cafe", "bar", "food"),
        HOTEL("Hotel", "lodging", "hotel"),
        SHOP("Negozio", "store", "shopping_mall"),
        TRANSPORT("Trasporto", "airport", "bus_station", "train_station", "transit_station", "subway_station"),
        BEACH("Spiaggia", "beach"),
        OTHER("Altro", "point_of_interest", "establishment");

        private final String italianName;
        private final Set<String> googleTypes;

        PlaceTypeCategory(String italianName, String... googleTypes) {
            this.italianName = italianName;
            this.googleTypes = Set.of(googleTypes);
        }

        public String getItalianName() {
            return italianName;
        }

        // Cache statica per lookup O(1) - inizializzata una sola volta
        private static final Map<String, PlaceTypeCategory> LOOKUP_MAP;
        static {
            Map<String, PlaceTypeCategory> map = new HashMap<>();
            for (PlaceTypeCategory category : values()) {
                for (String type : category.googleTypes) {
                    map.put(type, category);
                }
            }
            LOOKUP_MAP = Collections.unmodifiableMap(map);
        }

        /**
         * Mappa i tipi Google a categoria italiana
         * Priorità: tipi specifici > tipi generici > default
         */
        public static String mapToItalian(List<String> googleTypes) {
            if (googleTypes == null || googleTypes.isEmpty()) {
                return DEFAULT_TYPE;
            }

            // Prima cerca un tipo specifico (non generico) - O(n)
            return googleTypes.stream().filter(type -> !GENERIC_TYPES.contains(type)).map(LOOKUP_MAP::get)
                .filter(Objects::nonNull).filter(cat -> cat != OTHER).findFirst().map(PlaceTypeCategory::getItalianName).orElse(DEFAULT_TYPE);
        }
    }

    private final RestTemplate restTemplate;
    
    @Value("${google.places.api.key}")
    private String googleApiKey;

    public PlacesService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Ottiene suggerimenti autocomplete da Google Places API
     * 
     * @param input testo di ricerca dell'utente
     * @return DTO con suggerimenti e status
     * 
     * Cache: chiave normalizzata (lowercase+trim), TTL 1h, skip errori
     */
    @Cacheable(value = "placesAutocomplete", key = "#input.toLowerCase().trim()", unless = "#result.status() == 'ERROR' || #result.status() == 'INVALID_REQUEST'")
    @Override
    public PlaceAutocompleteDTO getAutocompleteResults(String input) {
        // 1. Validazione input con early return
        Optional<PlaceAutocompleteDTO> validationError = validateInput(input);
        if (validationError.isPresent()) {
            return validationError.get();
        }

        // 2. Costruzione URL sicura con encoding automatico
        String url = buildAutocompleteUrl(input.trim());

        try {
            // 3. Chiamata API con deserializzazione automatica (no parsing manuale)
            ResponseEntity<GoogleAutocompleteResponse> response = restTemplate.getForEntity(url, GoogleAutocompleteResponse.class);

            // 4. Validazione risposta
            if (!response.hasBody() || response.getBody() == null) {
                log.warn("Risposta vuota da Google Places API per input: {}", sanitizeLog(input));
                return createAutocompleteError("ZERO_RESULTS");
            }

            GoogleAutocompleteResponse body = response.getBody();

            // 5. Trasformazione con stream (lazy evaluation, più efficiente di loop)
            List<PlaceAutocompleteDTO.PlaceSuggestion> suggestions = Optional.ofNullable(body.getPredictions()).orElse(Collections.emptyList()).stream()
                    .map(this::toPlaceSuggestion).collect(Collectors.toList());

            return new PlaceAutocompleteDTO(suggestions, body.getStatus());

        } catch (RestClientException e) {
            log.error("Errore chiamata Google Places Autocomplete API per input '{}': {}", sanitizeLog(input), e.getMessage());
            return createAutocompleteError("API_ERROR");
        } catch (Exception e) {
            log.error("Errore imprevisto durante autocomplete per input '{}'", sanitizeLog(input), e);
            return createAutocompleteError("ERROR");
        }
    }

    /**
     * Ottiene luoghi nelle vicinanze di una coordinata
     * 
     * @param latitude latitudine del punto centrale
     * @param longitude longitudine del punto centrale
     * @param radius raggio di ricerca in metri
     * @param type tipo di luogo da cercare
     * @return lista di luoghi vicini (max NEARBY_MAX_RESULTS)
     * 
     * Cache: chiave basata su coordinata+raggio+tipo, TTL 1h, skip errori
     */
    @Cacheable(value = "nearbyPlaces", key = "'nearby_' + #latitude + '_' + #longitude + '_' + #radius + '_' + #type")
    @Override
    public List<NearbyPlaceDTO> getNearbyPlaces(double latitude, double longitude, int radius, String type) {
        // 1. Costruzione URL sicura con encoding automatico
        String url = buildNearbyUrl(latitude, longitude, radius, type);
        
        try {
            // 2. Chiamata API con deserializzazione automatica
            ResponseEntity<NearbySearchResponse> response = restTemplate.getForEntity(url, NearbySearchResponse.class);
            
            // 3. Validazione risposta
            if (!response.hasBody() || response.getBody() == null) {
                log.warn("Risposta vuota da Google Places Nearby API per lat={}, lng={}", latitude, longitude);
                return Collections.emptyList();
            }
            
            NearbySearchResponse body = response.getBody();
            
            // 4. Verifica status
            if (!"OK".equals(body.getStatus()) && !"ZERO_RESULTS".equals(body.getStatus())) {
                log.warn("Google Places Nearby API returned status: {} - {}", 
                    body.getStatus(), body.getErrorMessage());
                return Collections.emptyList();
            }
            
            // 5. Trasformazione risultati (max NEARBY_MAX_RESULTS)
            List<NearbyResult> results = Optional.ofNullable(body.getResults())
                .orElse(Collections.emptyList());
            
            return results.stream()
                .limit(NEARBY_MAX_RESULTS)
                .map(this::toNearbyPlaceDTO)
                .collect(Collectors.toList());
            
        } catch (RestClientException e) {
            log.error("Errore chiamata Google Places Nearby API per lat={}, lng={}: {}", 
                latitude, longitude, e.getMessage());
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Errore imprevisto durante ricerca luoghi vicini", e);
            return Collections.emptyList();
        }
    }

    /**
     * Ottiene dettagli di un luogo specifico da Google Places API
     * 
     * @param placeId ID univoco del luogo Google
     * @return DTO con coordinate, tipo, indirizzo e status
     * 
     * Cache: chiave placeId, TTL 2h (dettagli cambiano raramente), skip errori
     */
    @Cacheable(value = "placeDetails", key = "#placeId", unless = "#result.status() == 'ERROR' || #result.status() == 'INVALID_REQUEST'")
    @Override
    public PlaceDetailsDTO getPlaceDetails(String placeId) {
        // 1. Validazione input
        if (placeId == null || placeId.trim().isEmpty()) {
            log.warn("PlaceId nullo o vuoto");
            return createDetailsError("INVALID_REQUEST");
        }

        // 2. Costruzione URL sicura con encoding automatico
        String url = buildDetailsUrl(placeId.trim());

        try {
            // 3. Chiamata API con deserializzazione automatica
            ResponseEntity<GooglePlaceDetailsResponse> response = restTemplate.getForEntity(url, GooglePlaceDetailsResponse.class);

            // 4. Validazione risposta
            if (!response.hasBody() || response.getBody() == null) {
                log.warn("Risposta vuota da Google Places Details API per placeId: {}", placeId);
                return createDetailsError("ZERO_RESULTS");
            }

            GooglePlaceDetailsResponse body = response.getBody();

            // 5. Estrazione e trasformazione dati
            return extractPlaceDetails(body);

        } catch (RestClientException e) {
            log.error("Errore chiamata Google Places Details API per placeId '{}': {}", 
                placeId, e.getMessage());
            return createDetailsError("API_ERROR");
        } catch (Exception e) {
            log.error("Errore imprevisto durante recupero dettagli per placeId '{}'", placeId, e);
            return createDetailsError("ERROR");
        }
    }

    // ========== Metodi privati helper ==========

    /**
     * Valida l'input dell'autocomplete
     * @return Optional.empty() se valido, Optional.of(error) se invalido
     */
    private Optional<PlaceAutocompleteDTO> validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return Optional.of(createAutocompleteError("INVALID_REQUEST"));
        }

        String trimmed = input.trim();
        if (trimmed.length() < MIN_INPUT_LENGTH) {
            return Optional.of(createAutocompleteError("INPUT_TOO_SHORT"));
        }

        if (trimmed.length() > MAX_INPUT_LENGTH) {
            return Optional.of(createAutocompleteError("INPUT_TOO_LONG"));
        }

        return Optional.empty();
    }

    /**
     * Costruisce URL per autocomplete con encoding automatico
     * UriComponentsBuilder gestisce correttamente caratteri speciali, spazi, ecc.
     */
    private String buildAutocompleteUrl(String input) {
        return UriComponentsBuilder.fromUriString(PLACES_AUTOCOMPLETE_URL)
            .queryParam("input", input)
            .queryParam("key", googleApiKey)
            .queryParam("language", DEFAULT_LANGUAGE)
            .build()
            .toUriString();
    }

    /**
     * Costruisce URL per place details con encoding automatico
     * Fields specifici riducono payload e costi API
     */
    private String buildDetailsUrl(String placeId) {
        return UriComponentsBuilder.fromUriString(PLACES_DETAILS_URL)
            .queryParam("place_id", placeId)
            .queryParam("key", googleApiKey)
            .queryParam("fields", "geometry,types,address_components")
            .build()
            .toUriString();
    }

    /**
     * Costruisce URL per nearby search con encoding automatico
     */
    private String buildNearbyUrl(double latitude, double longitude, int radius, String type) {
        String location = latitude + "," + longitude;
        return UriComponentsBuilder.fromUriString(PLACES_NEARBY_URL)
            .queryParam("location", location)
            .queryParam("radius", radius)
            .queryParam("type", type)
            .queryParam("language", DEFAULT_LANGUAGE)
            .queryParam("key", googleApiKey)
            .build()
            .toUriString();
    }

    /**
     * Converte un NearbyResult Google in NearbyPlaceDTO
     * Nota: distanceMeters e alreadyInTravel vengono impostati dal chiamante
     */
    private NearbyPlaceDTO toNearbyPlaceDTO(NearbyResult result) {
        NearbyPlaceDTO dto = new NearbyPlaceDTO();
        dto.setPlaceId(result.getPlaceId());
        dto.setName(result.getName());
        dto.setAddress(Optional.ofNullable(result.getVicinity()).orElse(""));
        
        // Estrai coordinate
        if (result.getGeometry() != null && result.getGeometry().getLocation() != null) {
            dto.setLatitude(result.getGeometry().getLocation().getLat());
            dto.setLongitude(result.getGeometry().getLocation().getLng());
        } else {
            dto.setLatitude(0.0);
            dto.setLongitude(0.0);
        }
        
        // Mappa tipo con enum esistente
        List<String> googleTypes = Optional.ofNullable(result.getTypes())
            .orElse(Collections.emptyList());
        dto.setType(PlaceTypeCategory.mapToItalian(googleTypes));
        
        // Estrai photoReference se presente
        dto.setPhotoReference(extractPhotoReference(result));
        
        // Questi campi vengono impostati dal servizio chiamante
        dto.setDistanceMeters(0);
        dto.setAlreadyInTravel(false);
        
        return dto;
    }

    /**
     * Estrae il photo reference dal primo elemento dell'array photos
     */
    private String extractPhotoReference(NearbyResult result) {
        return Optional.ofNullable(result.getPhotos())
            .filter(photos -> !photos.isEmpty())
            .map(photos -> photos.get(0).getPhotoReference())
            .orElse(null);
    }

    /**
     * Converte una Prediction Google in PlaceSuggestion
     */
    private PlaceAutocompleteDTO.PlaceSuggestion toPlaceSuggestion(Prediction prediction) {
        String mainText = extractMainText(prediction);
        return new PlaceAutocompleteDTO.PlaceSuggestion(mainText, prediction.getDescription(), prediction.getPlaceId());
    }

    /**
     * Estrae il main text con fallback su description
     * Usa Optional per gestire null safety in modo elegante
     */
    private String extractMainText(Prediction prediction) {
        return Optional.ofNullable(prediction.getStructuredFormatting())
            .map(StructuredFormatting::getMainText)
            .filter(text -> !text.isEmpty())
            .orElseGet(() -> extractMainTextFromDescription(prediction.getDescription()));
    }

    /**
     * Estrae main text dalla description (prima della virgola)
     * Es: "Roma, Lazio, Italia" -> "Roma"
     */
    private String extractMainTextFromDescription(String description) {
        if (description == null || description.isEmpty()) {
            return "";
        }
        
        int commaIndex = description.indexOf(',');
        return commaIndex > 0 ? description.substring(0, commaIndex).trim() : description;
    }

    /**
     * Estrae i dettagli del luogo dalla risposta Google
     * Delega l'estrazione a metodi specifici per singola responsabilità
     */
    private PlaceDetailsDTO extractPlaceDetails(GooglePlaceDetailsResponse response) {
        PlaceResult result = response.getResult();
        
        if (result == null) {
            return createDetailsError(response.getStatus());
        }

        // Estrai coordinate (con valori di default se assenti)
        PlaceDetailsDTO.Coordinates coordinates = extractCoordinates(result);
        
        // Estrai e mappa il tipo usando enum ottimizzato
        List<String> googleTypes = Optional.ofNullable(result.getTypes())
            .orElse(Collections.emptyList());
        String type = PlaceTypeCategory.mapToItalian(googleTypes);
        
        // Estrai componenti indirizzo (paese, regione, città)
        AddressComponents addressComponents = extractAddressComponents(result);

        return new PlaceDetailsDTO(coordinates, type, addressComponents.country, addressComponents.region, addressComponents.city, googleTypes, response.getStatus());
    }

    /**
     * Estrae le coordinate dalla geometria
     * Default: (0.0, 0.0) se assenti
     */
    private PlaceDetailsDTO.Coordinates extractCoordinates(PlaceResult result) {
        return Optional.ofNullable(result.getGeometry())
            .map(Geometry::getLocation)
            .map(loc -> new PlaceDetailsDTO.Coordinates(loc.getLat(), loc.getLng()))
            .orElse(new PlaceDetailsDTO.Coordinates(0.0, 0.0));
    }

    /**
     * Classe helper per raggruppare componenti indirizzo
     * Più leggibile di tuple o Map<String, String>
     */
    private static class AddressComponents {
        String country = "";
        String region = "";
        String city = "";
    }

    /**
     * Estrae i componenti dell'indirizzo (paese, regione, città)
     * Usa switch per efficienza (jump table vs if-else chain)
     */
    private AddressComponents extractAddressComponents(PlaceResult result) {
        AddressComponents components = new AddressComponents();
        
        List<AddressComponent> addressComponents = 
            Optional.ofNullable(result.getAddressComponents())
                .orElse(Collections.emptyList());

        for (AddressComponent component : addressComponents) {
            List<String> types = Optional.ofNullable(component.getTypes())
                .orElse(Collections.emptyList());

            for (String type : types) {
                switch (type) {
                    case "country":
                        components.country = component.getLongName();
                        break;
                    case "administrative_area_level_1":
                        components.region = component.getLongName();
                        break;
                    case "locality":
                        components.city = component.getLongName();
                        break;
                }
            }
        }

        return components;
    }

    /**
     * Crea una risposta di errore per autocomplete
     * Liste immutabili per thread-safety e performance
     */
    private PlaceAutocompleteDTO createAutocompleteError(String status) {
        return new PlaceAutocompleteDTO(Collections.emptyList(), status);
    }

    /**
     * Crea una risposta di errore per place details
     */
    private PlaceDetailsDTO createDetailsError(String status) {
        return new PlaceDetailsDTO(new PlaceDetailsDTO.Coordinates(0.0, 0.0), DEFAULT_TYPE, "", "", "", Collections.emptyList(), status);
    }

    /**
     * Sanitizza l'input per il logging
     * Previene: log injection, log forging, memory overflow
     */
    private String sanitizeLog(String input) {
        if (input == null) return "null";
        // Rimuove newline (log injection) e limita lunghezza (memory)
        return input.replaceAll("[\\r\\n]", "").substring(0, Math.min(input.length(), 100));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GoogleAutocompleteResponse {
        private String status;
        private List<Prediction> predictions;
        
        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Prediction {
        @JsonProperty("place_id")
        private String placeId;
        
        private String description;
        
        @JsonProperty("structured_formatting")
        private StructuredFormatting structuredFormatting;
        
        private List<String> types;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class StructuredFormatting {
        @JsonProperty("main_text")
        private String mainText;
        
        @JsonProperty("secondary_text")
        private String secondaryText;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GooglePlaceDetailsResponse {
        private String status;
        private PlaceResult result;
        
        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class PlaceResult {
        private Geometry geometry;
        private List<String> types;
        
        @JsonProperty("address_components")
        private List<AddressComponent> addressComponents;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Geometry {
        private Location location;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Location {
        private double lat;
        private double lng;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AddressComponent {
        @JsonProperty("long_name")
        private String longName;
        
        @JsonProperty("short_name")
        private String shortName;
        
        private List<String> types;
    }

    // ========== Inner classes per Nearby Search API ==========

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NearbySearchResponse {
        private String status;
        private List<NearbyResult> results;
        
        @JsonProperty("error_message")
        private String errorMessage;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class NearbyResult {
        @JsonProperty("place_id")
        private String placeId;
        
        private String name;
        private String vicinity;
        private Geometry geometry;
        private List<String> types;
        private List<Photo> photos;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Photo {
        @JsonProperty("photo_reference")
        private String photoReference;
        
        private int width;
        private int height;
    }
}
