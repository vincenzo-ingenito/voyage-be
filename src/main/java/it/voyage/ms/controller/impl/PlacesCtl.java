package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import it.voyage.ms.controller.IPlacesCtl;
import it.voyage.ms.service.IPlacesService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class PlacesCtl implements IPlacesCtl {

	@Autowired
	private IPlacesService placesService;
	
//	@Autowired
//	private PlacesRateLimiter rateLimiter;

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

		// Verifica Rate Limit per utente
//		if (!rateLimiter.allowUserRequest(userId)) {
//			int remaining = rateLimiter.getRemainingRequests(userId);
//			log.warn("🚫 Rate Limit superato per user: {}", userId);
//			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//				.header("X-RateLimit-Remaining", String.valueOf(remaining))
//				.body("{\"error\": \"Rate limit exceeded. Max 50 requests per day.\", \"remaining\": " + remaining + "}");
//		}

		// Verifica Rate Limit per IP (protezione DDoS)
		String clientIp = getClientIpAddress();
//		if (!rateLimiter.allowIpRequest(clientIp)) {
//			log.warn("🚫 Rate Limit IP superato per: {}", clientIp);
//			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//				.body("{\"error\": \"Too many requests from your IP. Please try again later.\"}");
//		}

		// Esegui la richiesta Places API
		String responseBody = placesService.getAutocompleteResults(input);
		
		// Aggiungi header con richieste rimanenti
		int remaining = 100;//rateLimiter.getRemainingRequests(userId);
		return ResponseEntity.ok()
			.header("X-RateLimit-Remaining", String.valueOf(remaining))
			.body(responseBody);
	}

	@Override
	public ResponseEntity<String> getPlaceDetails(String placeId) {
		log.info("📍 Places API: getPlaceDetails chiamato");
		
		// Validazione input
		if (placeId == null || placeId.isEmpty()) {
			return ResponseEntity.badRequest()
				.body("{\"error\": \"Place ID is required\"}");
		}

		// Ottieni userId dall'autenticazione
		String userId = getUserIdFromAuth();
		if (userId == null) {
			log.warn("⚠️ Places API: Utente non autenticato");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body("{\"error\": \"Authentication required\"}");
		}

		// Verifica Rate Limit per utente
//		if (!rateLimiter.allowUserRequest(userId)) {
//			int remaining = rateLimiter.getRemainingRequests(userId);
//			log.warn("🚫 Rate Limit superato per user: {}", userId);
//			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//				.header("X-RateLimit-Remaining", String.valueOf(remaining))
//				.body("{\"error\": \"Rate limit exceeded. Max 50 requests per day.\", \"remaining\": " + remaining + "}");
//		}

		// Verifica Rate Limit per IP (protezione DDoS)
//		String clientIp = getClientIpAddress();
//		if (!rateLimiter.allowIpRequest(clientIp)) {
//			log.warn("🚫 Rate Limit IP superato per: {}", clientIp);
//			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
//				.body("{\"error\": \"Too many requests from your IP. Please try again later.\"}");
//		}

		// Esegui la richiesta Places API
		String responseBody = placesService.getPlaceDetails(placeId);
		
		// Aggiungi header con richieste rimanenti
		int remaining = 100;//rateLimiter.getRemainingRequests(userId);
		return ResponseEntity.ok()
			.header("X-RateLimit-Remaining", String.valueOf(remaining))
			.body(responseBody);
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

	/**
	 * Ottiene l'indirizzo IP del client considerando proxy e load balancer.
	 * 
	 * @return indirizzo IP del client
	 */
	private String getClientIpAddress() {
		try {
			ServletRequestAttributes attributes = 
				(ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			HttpServletRequest request = attributes.getRequest();
			
			// Controlla header X-Forwarded-For (proxy/load balancer)
			String xForwardedFor = request.getHeader("X-Forwarded-For");
			if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
				// Prendi il primo IP (client originale)
				return xForwardedFor.split(",")[0].trim();
			}
			
			// Controlla altri header comuni
			String xRealIp = request.getHeader("X-Real-IP");
			if (xRealIp != null && !xRealIp.isEmpty()) {
				return xRealIp;
			}
			
			// Fallback all'IP remoto
			return request.getRemoteAddr();
		} catch (Exception e) {
			log.warn("Impossibile determinare IP client: {}", e.getMessage());
			return "unknown";
		}
	}
}
