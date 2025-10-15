package it.voyage.ms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.security.user.CustomUserDetails;

/**
 * Controller auth.
 */
@RequestMapping(path = "/api/auth")
@Tag(name = "Auth Management", description = "Endpoints per l'autenticazione tramite Firebase e il rilascio di token interni")
public interface IAuthCtl {

	 
@PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    
	@Operation(summary = "Login e Provisioning Utente", description = "Registra l'utente sul database interno e aggiorna i dati di accesso.")
	@ApiResponses(value = { 
	    @ApiResponse(responseCode = "200", description = "Utente sincronizzato e login completato. Restituisce i dati dell'utente interno.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
	    @ApiResponse(responseCode = "401", description = "Unauthorized: Token Firebase non valido o scaduto (intercettato dal filtro di sicurezza)."),
	    @ApiResponse(responseCode = "500", description = "Internal Server Error: Errore del server durante il provisioning o la sincronizzazione.")
	})
	ResponseEntity<UserDto> login(@AuthenticationPrincipal CustomUserDetails customUserDetail);
 
}
