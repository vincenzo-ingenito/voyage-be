package it.voyage.ms.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.SearchRequest;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.security.user.CustomUserDetails;

@RequestMapping(path = "/api/friends")
@Tag(name = "Friend Management", description = "Endpoints per la gestione delle amicizie, ricerca e dati aggregati (es. viaggi)")
public interface IFriendCtl {

	/**
	 * Recupera la lista degli amici accettati per l'utente loggato.
	 * * @param user Dettagli dell'utente autenticato.
	 * @return Una lista di UserDto che include l'utente loggato come primo elemento.
	 */
	@GetMapping(path = "/accepted", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Recupera la lista degli amici accettati", description = "Restituisce una lista di UserDto contenente gli amici dell'utente corrente. L'utente loggato è inserito come primo elemento della lista.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lista di amici accettati recuperata con successo.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UserDto.class)))),
			@ApiResponse(responseCode = "401", description = "Non autorizzato (Autenticazione mancante o non valida)"),
			@ApiResponse(responseCode = "500", description = "Errore interno del server")
	})
	ResponseEntity<List<UserDto>> getAcceptedFriends(@AuthenticationPrincipal CustomUserDetails user);
	
	/**
	 * Recupera tutte le richieste di amicizia pendenti ricevute dall'utente loggato.
	 * @param user Dettagli dell'utente autenticato.
	 * @return Una lista di FriendRelationshipDto.
	 */
	@Operation(summary = "Recupera le richieste di amicizia pendenti", description = "Restituisce una lista di richieste di amicizia che l'utente corrente deve ancora accettare o rifiutare.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Lista di richieste pendenti recuperata con successo.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = FriendRelationshipDto.class)))),
			@ApiResponse(responseCode = "401", description = "Non autorizzato")
	})
	@GetMapping(path = "/requests/pending", produces = MediaType.APPLICATION_JSON_VALUE)
	ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(@AuthenticationPrincipal CustomUserDetails user);
	
	/**
	 * Esegue la ricerca di utenti e determina lo stato della relazione con l'utente corrente.
	 * @param searchRequest Contiene la stringa di ricerca (query).
	 * @param user Dettagli dell'utente autenticato.
	 * @return Una lista di UserSearchResult, inclusa l'informazione sullo stato di amicizia/blocco.
	 */
	@PostMapping(path = "/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Ricerca di utenti e stato di amicizia", description = "Esegue una ricerca fuzzy/regex e restituisce i risultati arricchiti con lo stato della relazione (Amico, Bloccato, Richiesta inviata, Disponibile, ecc.).")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Ricerca eseguita con successo.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = UserSearchResult.class)))),
			@ApiResponse(responseCode = "400", description = "Richiesta non valida (es. query vuota)"),
			@ApiResponse(responseCode = "401", description = "Non autorizzato")
	})
	ResponseEntity<List<UserSearchResult>> searchUsers(@RequestBody SearchRequest searchRequest, @AuthenticationPrincipal CustomUserDetails user);
	
	/**
	 * Recupera la lista consolidata e arricchita dei Paesi visitati da un utente o amico.
	 *
	 * @param friendId L'ID dell'utente di cui si vogliono visualizzare i viaggi.
	 * @param user Dettagli dell'utente autenticato (utilizzato per la verifica dell'amicizia).
	 * @return Una lista di CountryVisit consolidate, raggruppate per paese.
	 */
	@GetMapping(path = "/{friendId}/visited", produces = MediaType.APPLICATION_JSON_VALUE)
	@Operation(summary = "Paesi visitati consolidati", description = "Recupera i dati di viaggio consolidati e raggruppati per Paese/Regione per l'utente specificato, verificando prima che esista una relazione di amicizia.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Dati di viaggio consolidati restituiti con successo.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = CountryVisit.class)))),
			@ApiResponse(responseCode = "401", description = "Non autorizzato (token mancante/non valido)."),
			@ApiResponse(responseCode = "403", description = "Accesso negato. L'utente specificato non è l'utente corrente e non è un amico accettato."),
			@ApiResponse(responseCode = "404", description = "Utente non trovato.")
	})
	ResponseEntity<List<CountryVisit>> getVisitedCountries(@PathVariable("friendId") String friendId, @AuthenticationPrincipal CustomUserDetails user);

}