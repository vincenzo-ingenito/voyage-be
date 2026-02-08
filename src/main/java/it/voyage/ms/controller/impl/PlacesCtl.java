package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IPlacesCtl;
import it.voyage.ms.service.IPlacesService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class PlacesCtl implements IPlacesCtl {

	@Autowired
	private IPlacesService placesService;
	 
	@Override
	public ResponseEntity<String> autocomplete(String input) {
		log.info("Places API: autocomplete chiamato");
		
		// Validazione input
		if (input == null || input.length() < 3) {
			return ResponseEntity.badRequest()
				.body("{\"error\": \"Input must be at least 3 characters\"}");
		}

		// Ottieni userId dall'autenticazione
		String userId = getUserIdFromAuth();
		if (userId == null) {
			log.warn("Places API: Utente non autenticato");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body("{\"error\": \"Authentication required\"}");
		}
  
		String responseBody = placesService.getAutocompleteResults(input);
		return ResponseEntity.ok().body(responseBody);
	}

	@Override
	public ResponseEntity<String> getPlaceDetails(String placeId) {
		log.info("Places API: getPlaceDetails chiamato");
		
		// Validazione input
		if (placeId == null || placeId.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
		}

		// Ottieni userId dall'autenticazione
		String userId = getUserIdFromAuth();
		if (userId == null) {
			log.warn("Places API: Utente non autenticato");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body("{\"error\": \"Authentication required\"}");
		}
 
		// Esegui la richiesta Places API
		String responseBody = placesService.getPlaceDetails(placeId);
		
		return ResponseEntity.ok().body(responseBody);
	}

	/**
	 * Estrae l'userId dall'autenticazione Spring Security.
	 * 
	 * @return userId o null se non autenticato
	 */
	private String getUserIdFromAuth() {
		try {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication != null && authentication.isAuthenticated()) {
				return authentication.getName(); // Ritorna il Firebase UID
			}
		} catch (Exception e) {
			log.error("Errore nell'estrazione userId: {}", e.getMessage());
		}
		return null;
	}
}