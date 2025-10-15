package it.voyage.ms.service;

public interface IPlacesService {
    String getAutocompleteResults(String input);
    String getPlaceDetails(String placeId);
}