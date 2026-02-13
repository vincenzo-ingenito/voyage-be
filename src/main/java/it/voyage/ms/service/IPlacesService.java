package it.voyage.ms.service;

import it.voyage.ms.dto.response.PlaceAutocompleteDTO;
import it.voyage.ms.dto.response.PlaceDetailsDTO;

public interface IPlacesService {
    PlaceAutocompleteDTO getAutocompleteResults(String input);
    PlaceDetailsDTO getPlaceDetails(String placeId);
}
