package it.voyage.ms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.FriendRequestDto;
import it.voyage.ms.security.user.CustomUserDetails;

@RequestMapping("/api/friendrelationship")
@Tag(name = "Friend Management", description = "Endpoints per la gestione delle amicizie, ricerca e dati aggregati (es. viaggi)")
public interface IFriendRelationshipCtl {

	/**
	 * Recupera la lista degli amici accettati per l'utente loggato.
	 * * @param user Dettagli dell'utente autenticato.
	 * @return Una lista di UserDto che include l'utente loggato come primo elemento.
	 */
	@PostMapping("/send-request")
	@Operation(summary = "Invia una richiesta di amicizia", description = "Consente all'utente loggato di inviare una richiesta ad un altro utente. L'accettazione è automatica se il profilo destinatario è pubblico.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Richiesta inviata o amicizia accettata automaticamente.", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
			@ApiResponse(responseCode = "400", description = "Bad Request (Es.: Tentativo di inviare richiesta a sé stessi)"),
			@ApiResponse(responseCode = "401", description = "Non autorizzato (Autenticazione mancante o non valida)"),
			@ApiResponse(responseCode = "404", description = "Utente destinatario non trovato."),
			@ApiResponse(responseCode = "409", description = "Conflict (Relazione o richiesta di amicizia esiste già)."),
			@ApiResponse(responseCode = "500", description = "Errore interno del server")
	})
	ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto, @AuthenticationPrincipal CustomUserDetails userPrincipal);
	
	
	@PutMapping("/{requesterId}/{action}")
	@Operation(summary = "Gestione richiesta di amicizia", description = "Consente al destinatario di accettare o rifiutare una richiesta di amicizia in sospeso fornendo l'ID del mittente e l'azione.")
	@ApiResponses(value = {
	    @ApiResponse(responseCode = "200", description = "Richiesta gestita con successo.", content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE)),
	    @ApiResponse(responseCode = "400", description = "Bad Request (Azione non valida. E.g., 'action' non è 'accept' o 'decline')."),
	    @ApiResponse(responseCode = "401", description = "Non autorizzato (Autenticazione mancante o non valida)"),
	    @ApiResponse(responseCode = "404", description = "Richiesta di amicizia in sospeso non trovata per i due utenti specificati."),
	    @ApiResponse(responseCode = "500", description = "Errore interno del server")
	})
	ResponseEntity<String> handleFriendRequest(@PathVariable String requesterId, @PathVariable String action, @AuthenticationPrincipal CustomUserDetails userPrincipal);
}