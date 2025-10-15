package it.voyage.ms.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import it.voyage.ms.service.IPlacesService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlacesService implements IPlacesService{

	private static final String PLACES_AUTOCOMPLETE_URL = "https://maps.googleapis.com/maps/api/place/autocomplete/json";
	private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json";
	
	@Value("${google.places.api.key}")
	private String googleApiKey;

	private final RestTemplate restTemplate;

	@Override
	public String getAutocompleteResults(String input) {
		String url = String.format("%s?input=%s&key=%s&language=it", PLACES_AUTOCOMPLETE_URL, input, googleApiKey);
		return restTemplate.getForObject(url, String.class);
	}

	@Override
	public String getPlaceDetails(String placeId) {
		String fields = "geometry,types,address_components"; 
		String url = String.format("%s?place_id=%s&key=%s&fields=%s", PLACES_DETAILS_URL, placeId, googleApiKey, fields);
		return restTemplate.getForObject(url, String.class);
	}

}
