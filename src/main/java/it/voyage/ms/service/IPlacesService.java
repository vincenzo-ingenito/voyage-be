package it.voyage.ms.service;

import it.voyage.ms.dto.response.NearbyPlaceDTO;
import it.voyage.ms.dto.response.PlaceAutocompleteDTO;
import it.voyage.ms.dto.response.PlaceDetailsDTO;

import java.util.List;

public interface IPlacesService {
    PlaceAutocompleteDTO getAutocompleteResults(String input);
    PlaceDetailsDTO getPlaceDetails(String placeId);
    List<NearbyPlaceDTO> getNearbyPlaces(double latitude, double longitude, int radius, String type);
}
