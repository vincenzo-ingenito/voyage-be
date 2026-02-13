package it.voyage.ms.controller;

import it.voyage.ms.dto.response.PlaceAutocompleteDTO;
import it.voyage.ms.dto.response.PlaceDetailsDTO;
import it.voyage.ms.security.user.CustomUserDetails;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag; 

/**
 * Contratto per l'integrazione con servizi di geolocalizzazione (es. Google Places).
 */
@RequestMapping(path = "/api/places") 
@Tag(name = "Geolocation/Places API", description = "Endpoints proxy per l'autocompletamento e i dettagli di luoghi.")
public interface IPlacesCtl {

    @GetMapping(value = "/autocomplete", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Autocompletamento di luoghi", description = "Fornisce suggerimenti di luoghi in base al testo immesso.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Elenco di suggerimenti strutturati.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceAutocompleteDTO.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request: Testo di ricerca troppo corto o mancante."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error o errore di connessione all'API di Google.")
    })
    ResponseEntity<PlaceAutocompleteDTO> autocomplete(
        @Parameter(description = "Il testo parziale inserito dall'utente (minimo 3 caratteri).", required = true)
        @RequestParam String input,
        @AuthenticationPrincipal CustomUserDetails userDetails);

    @GetMapping(value = "/details", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Dettagli del luogo", description = "Recupera i dettagli completi di un luogo specifico, compresi i componenti dell'indirizzo e la geometria.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Dettagli completi del luogo strutturati.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PlaceDetailsDTO.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request: ID del luogo mancante."),
        @ApiResponse(responseCode = "500", description = "Internal Server Error o errore di connessione all'API di Google.")
    })
    ResponseEntity<PlaceDetailsDTO> getPlaceDetails(
        @Parameter(description = "L'ID univoco del luogo (placeId) restituito dall'autocompletamento.", required = true)
        @RequestParam String placeId,
        @AuthenticationPrincipal CustomUserDetails userDetails);
}
