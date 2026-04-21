package it.voyage.ms.controller;

import it.voyage.ms.dto.request.VoteRequest;
import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Controller per la gestione dei voti sui viaggi (sistema tipo Reddit)
 */
@RequestMapping(path = "/api/travel")
@Tag(name = "Travel Votes", description = "Endpoints per il sistema di voto sui viaggi (upvote/downvote)")
public interface ITravelVoteCtl {
    
    @PostMapping(value = "/{travelId}/vote", 
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Vota un viaggio", 
               description = "Permette di votare un viaggio (upvote o downvote). Se l'utente vota due volte la stessa opzione, il voto viene rimosso (toggle).")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Voto registrato con successo"),
        @ApiResponse(responseCode = "400", description = "Bad Request: Dati non validi"),
        @ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato"),
        @ApiResponse(responseCode = "404", description = "Not Found: Viaggio non trovato")
    })
    ResponseEntity<VoteStatsDTO> voteTravel(
        @Parameter(description = "ID del viaggio da votare") @PathVariable Long travelId,
        @RequestBody VoteRequest voteRequest,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @DeleteMapping(value = "/{travelId}/vote", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Rimuovi voto", 
               description = "Rimuove il voto dell'utente per un viaggio specifico")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Voto rimosso con successo"),
        @ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato"),
        @ApiResponse(responseCode = "404", description = "Not Found: Viaggio non trovato")
    })
    ResponseEntity<VoteStatsDTO> removeVote(
        @Parameter(description = "ID del viaggio") @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @GetMapping(value = "/{travelId}/vote-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Ottieni statistiche di voto", 
               description = "Restituisce le statistiche di voto per un viaggio (upvotes, downvotes, punteggio netto e voto dell'utente)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistiche recuperate con successo"),
        @ApiResponse(responseCode = "401", description = "Unauthorized: Utente non autenticato"),
        @ApiResponse(responseCode = "404", description = "Not Found: Viaggio non trovato")
    })
    ResponseEntity<VoteStatsDTO> getVoteStats(
        @Parameter(description = "ID del viaggio") @PathVariable Long travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}