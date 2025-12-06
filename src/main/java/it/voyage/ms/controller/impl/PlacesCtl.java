package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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
		log.info("Called autocomplete ep");
		if (input == null || input.length() < 3) {
			return ResponseEntity.badRequest().body("{\"error\": \"Input must be at least 3 characters\"}");
		}

		String responseBody = placesService.getAutocompleteResults(input);
		return ResponseEntity.ok(responseBody);
	}


	@Override
	public ResponseEntity<String> getPlaceDetails(String placeId) {
		log.info("Called get place details ep");
		if (placeId == null || placeId.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
		}

		String responseBody = placesService.getPlaceDetails(placeId);
		return ResponseEntity.ok(responseBody);

	}
}
