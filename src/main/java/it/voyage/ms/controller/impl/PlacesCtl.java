package it.voyage.ms.controller.impl;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IPlacesCtl;
import it.voyage.ms.dto.response.PlaceAutocompleteDTO;
import it.voyage.ms.dto.response.PlaceDetailsDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IPlacesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PlacesCtl implements IPlacesCtl {

	private final IPlacesService placesService;
	 
	@Override
	public ResponseEntity<PlaceAutocompleteDTO> autocomplete(String input, CustomUserDetails userDetails) {
		log.info("Places API: autocomplete chiamato con input={}", input);
		
		// Validazione input
		if (input == null || input.length() < 3) {
			log.warn("Places API: Input troppo corto");
			PlaceAutocompleteDTO errorResponse = new PlaceAutocompleteDTO();
			errorResponse.setStatus("INVALID_REQUEST");
			return ResponseEntity.badRequest().body(errorResponse);
		}

		PlaceAutocompleteDTO result = placesService.getAutocompleteResults(input);
		log.info("Places API: Trovati {} suggerimenti", result.getPredictions() != null ? result.getPredictions().size() : 0);
		return ResponseEntity.ok(result);
	}

	@Override
	public ResponseEntity<PlaceDetailsDTO> getPlaceDetails(String placeId, CustomUserDetails userDetails) {
		log.info("Places API: getPlaceDetails chiamato con placeId={}", placeId);
		
		// Validazione input
		if (placeId == null || placeId.isEmpty()) {
			log.warn("Places API: PlaceId mancante");
			PlaceDetailsDTO errorResponse = new PlaceDetailsDTO();
			errorResponse.setStatus("INVALID_REQUEST");
			return ResponseEntity.badRequest().body(errorResponse);
		}

		PlaceDetailsDTO result = placesService.getPlaceDetails(placeId);
		log.info("Places API: Dettagli recuperati per placeId={}", placeId);
		return ResponseEntity.ok(result);
	}

}