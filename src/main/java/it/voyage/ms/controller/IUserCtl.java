package it.voyage.ms.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.firebase.auth.FirebaseToken;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.response.PrivacyStatusResponse;

/**
 * Controller user.
 */
@RequestMapping(path = "/api/user")
@Tag(name = "Controller per la gestione dell'utente")
public interface IUserCtl {

	
	@PutMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class)))
	@Operation(summary = "Aggiornamento utente", description = "Consente di aggiornare alcuni campi dell'utente.")
	@ApiResponses(value = { 
	    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
	    @ApiResponse(responseCode = "400", description = "Bad Request"),
	    @ApiResponse(responseCode = "404", description = "Not found"),
	    @ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	ResponseEntity<UserDto>  updateUserDetails(@RequestBody UserDto updateDTO,@AuthenticationPrincipal FirebaseToken firebaseToken);

	@GetMapping("/privacy-status")
	@ApiResponse(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PrivacyStatusResponse.class)))
	@Operation(summary = "Ottenimento stato privacy", description = "Consente di ottenere lo stato della privacy scelto dall'utente.")
	@ApiResponses(value = { 
	    @ApiResponse(responseCode = "200", description = "Ok", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = PrivacyStatusResponse.class))),
	    @ApiResponse(responseCode = "400", description = "Bad Request"),
	    @ApiResponse(responseCode = "404", description = "Not found"),
	    @ApiResponse(responseCode = "500", description = "Internal Server Error")
	})
	public ResponseEntity<PrivacyStatusResponse> getPrivacyStatus(@AuthenticationPrincipal FirebaseToken firebaseToken);
}
