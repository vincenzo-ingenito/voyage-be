package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IPlacesCtl;
import it.voyage.ms.service.IPlacesService;

@RestController
public class PlacesCtl implements IPlacesCtl {


	@Autowired
	private IPlacesService placesService; 

	@Override
	public ResponseEntity<String> autocomplete(@RequestParam String input) {
		if (input == null || input.length() < 3) {
			return ResponseEntity.badRequest().body("{\"error\": \"Input must be at least 3 characters\"}");
		}

		String responseBody = placesService.getAutocompleteResults(input);
		return ResponseEntity.ok(responseBody);
	}


	@Override
	public ResponseEntity<String> getPlaceDetails(@RequestParam String placeId) {
		if (placeId == null || placeId.isEmpty()) {
			return ResponseEntity.badRequest().body("{\"error\": \"Place ID is required\"}");
		}

		String responseBody = placesService.getPlaceDetails(placeId);
		return ResponseEntity.ok(responseBody);

	}
}
