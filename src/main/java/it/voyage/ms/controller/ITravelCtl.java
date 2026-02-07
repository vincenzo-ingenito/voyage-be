package it.voyage.ms.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.security.user.CustomUserDetails; 

/**
 * Contratto per la gestione delle risorse di Viaggio (Travels).
 */
@RequestMapping(path = "/api/travel")
@Tag(name = "Travel Management", description = "Endpoints per la gestione dei viaggi di un utente.")
public interface ITravelCtl {

	@GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Recupera i viaggi", description = "Restituisce l'elenco dei viaggi creati dall'utente autenticato.")
	@ApiResponses(value = { 
			@ApiResponse(responseCode = "200", description = "Elenco dei viaggi recuperato con successo.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal CustomUserDetails userDetails);


	@DeleteMapping(value = "/{travelId}", produces = MediaType.TEXT_PLAIN_VALUE)
	@Operation(summary = "Elimina un viaggio", description = "Elimina il viaggio specificato, solo se appartenente all'utente autenticato.")
	@ApiResponses(value = { 
			@ApiResponse(responseCode = "200", description = "Viaggio eliminato con successo. (Messaggio: Viaggio eliminato con successo)", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE, schema = @Schema(implementation = Void.class))),
			@ApiResponse(responseCode = "404", description = "Not Found: Il viaggio non esiste o non appartiene all'utente."),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato.")
	})
	ResponseEntity<Void> deleteTravelById(
			@Parameter(description = "ID del viaggio da eliminare") @PathVariable String travelId, 
			@AuthenticationPrincipal CustomUserDetails userDetails
			);


	@PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
	@Operation(summary = "Crea un nuovo viaggio con file allegati", description = "Permette di salvare un nuovo viaggio con multipart/form-data e file allegati.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", 
					description = "Viaggio creato con successo.", 
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request: Dati mancanti o formato non valido."),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "415", description = "Unsupported Media Type: Formato di richiesta non supportato.")
	})
	ResponseEntity<?> saveTravel(@RequestPart(value = "travelData") TravelDTO travelData, @RequestPart(value = "files", required = false) List<MultipartFile> files, @AuthenticationPrincipal CustomUserDetails userDetails);

	@PutMapping(value = "/{travelId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
	@Operation(summary = "Aggiorna un viaggio esistente", description = "Permette di modificare un viaggio esistente, accettando i dati del viaggio come parte JSON e un elenco di file (immagini) come parte binaria.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", 
					description = "Viaggio aggiornato con successo.", 
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "400", description = "Bad Request: Dati mancanti o formato non valido."),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "404", description = "Not Found: Il viaggio non esiste o non appartiene all'utente."),
			@ApiResponse(responseCode = "415", description = "Unsupported Media Type: Formato di richiesta non multipart/form-data.")
	})
	ResponseEntity<TravelDTO> updateTravel(@Parameter(description = "ID del viaggio da aggiornare") @PathVariable String travelId, @RequestPart("travelData") TravelDTO travelData, @RequestPart(value = "files", required = false) List<MultipartFile> files, @AuthenticationPrincipal CustomUserDetails userDetails);

	@GetMapping(value = "/{travelId}/with-urls", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Recupera un viaggio con URL signed", description = "Restituisce un singolo viaggio con gli URL firmati per allegati e immagini. Chiamata on-demand per ridurre il payload iniziale.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", 
					description = "Viaggio recuperato con successo con URL signed.", 
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "404", description = "Not Found: Il viaggio non esiste o non appartiene all'utente.")
	})
	ResponseEntity<TravelDTO> getTravelWithUrls(
			@Parameter(description = "ID del viaggio") @PathVariable String travelId,
			@AuthenticationPrincipal CustomUserDetails userDetails
	);

	@PutMapping(value = "/{travelId}/confirm-dates", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Conferma le date di un viaggio copiato", description = "Rimuove i flag isCopied e needsDateConfirmation da un viaggio copiato, attivandolo per la modifica.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", 
					description = "Viaggio attivato con successo.", 
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "404", description = "Not Found: Il viaggio non esiste o non appartiene all'utente.")
	})
	ResponseEntity<TravelDTO> confirmTravelDates(
			@Parameter(description = "ID del viaggio da attivare") @PathVariable String travelId,
			@AuthenticationPrincipal CustomUserDetails userDetails
	);

	@DeleteMapping(value = "/{travelId}/day/{dayNumber}/memory-photo", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Elimina la foto ricordo di un giorno", description = "Rimuove la foto ricordo (memoryImage) associata a un giorno specifico dell'itinerario.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", 
					description = "Foto ricordo eliminata con successo.", 
					content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = TravelDTO.class))),
			@ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato."),
			@ApiResponse(responseCode = "404", description = "Not Found: Il viaggio o il giorno non esistono.")
	})
	ResponseEntity<TravelDTO> deleteMemoryPhoto(
			@Parameter(description = "ID del viaggio") @PathVariable String travelId,
			@Parameter(description = "Numero del giorno") @PathVariable int dayNumber,
			@AuthenticationPrincipal CustomUserDetails userDetails
	);

}
