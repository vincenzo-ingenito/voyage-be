package it.voyage.ms.controller.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;

@RestController
@RequestMapping("/api/travels")
public class TravelCtl {

	@Autowired
	private TravelRepository travelRepo;

	@Value("${google.places.api.key}")
	private String googleApiKey;
	
	@Autowired
	private RestTemplate restTemplate;

	
	@GetMapping("")
	public ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal FirebaseToken userFirebase) {
		List<TravelEty> travelEty = travelRepo.findByUserId(userFirebase.getUid());
		List<TravelDTO> output = new ArrayList<>();
		for(TravelEty t : travelEty) {
			output.add(TravelDTO.convertToDTO(t));
		}
		return ResponseEntity.ok(output);
	}

	@DeleteMapping("/{travelId}")
	public ResponseEntity<String> deleteTravelById(@PathVariable String travelId, @AuthenticationPrincipal FirebaseToken userFirebase) {
		long deletedCount = travelRepo.deleteByIdAndUserId(travelId, userFirebase.getUid());
		return deletedCount > 0 ? ResponseEntity.ok("Viaggio eliminato con successo") : ResponseEntity.notFound().build();
	}

	
	private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
	private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";


	/**
	 * Endpoint proxy per Google Places Autocomplete.
	 * @param input Il testo di ricerca dell'utente.
	 * @return La risposta JSON dell'API di Google.
	 */
	@GetMapping("/autocomplete")
	public ResponseEntity<String> autocomplete(@RequestParam String input) {
		if (input == null || input.length() < 3) {
			return ResponseEntity.badRequest().body("{\"error\": \"Input must be at least 3 characters\"}");
		}

		String url = String.format("%s?input=%s&key=%s&language=it", 
				PLACES_AUTOCOMPLETE_URL, input, googleApiKey);

		// Esegue la chiamata all'API di Google
		String response = restTemplate.getForObject(url, String.class);

		return ResponseEntity.ok(response);
	}

	 /**
     * Endpoint proxy per Google Place Details (per ottenere Lat/Lng).
     * @param placeId L'ID del luogo restituito da Autocomplete.
     * @return La risposta JSON dell'API di Google.
     */
//	@GetMapping("/details")
//    public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
//        if (placeId == null || placeId.isEmpty()) {
//            return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
//        }
//        
//        // **MODIFICA QUI:** Aggiungi 'types' al campo 'fields'
//        String fields = "geometry,types"; 
//        
//        String url = String.format("%s?place_id=%s&key=%s&fields=%s",
//                                   PLACES_DETAILS_URL, placeId, googleApiKey, fields);
//        
//        // Esegue la chiamata all'API di Google
//        String response = restTemplate.getForObject(url, String.class);
//        
//        return ResponseEntity.ok(response);
//    }
	
	@GetMapping("/details")
	public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
	    if (placeId == null || placeId.isEmpty()) {
	        return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
	    }
	    
	    // MODIFICA CRUCIALE: Includi 'address_components'
	    String fields = "geometry,types,address_components"; 
	    
	    String url = String.format("%s?place_id=%s&key=%s&fields=%s",
	                               PLACES_DETAILS_URL, placeId, googleApiKey, fields);
	    
	    // Esegue la chiamata all'API di Google
	    String response = restTemplate.getForObject(url, String.class);
	    
	    return ResponseEntity.ok(response);
	}

}
