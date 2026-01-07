package it.voyage.ms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.voyage.ms.dto.response.BookmarkDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller per la gestione dei segnalibri
 */
@Tag(name = "Bookmark Controller", description = "API per la gestione dei segnalibri dei viaggi")
@RequestMapping("/api/bookmarks")
public interface IBookmarkCtl {
    
    @Operation(
        summary = "Aggiungi un viaggio ai segnalibri",
        description = "Permette all'utente di salvare un viaggio nei propri segnalibri"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bookmark creato con successo"),
        @ApiResponse(responseCode = "404", description = "Viaggio non trovato"),
        @ApiResponse(responseCode = "409", description = "Bookmark già esistente o tentativo di salvare il proprio viaggio")
    })
    @PostMapping("/{travelId}")
    ResponseEntity<BookmarkDTO> addBookmark(
        @Parameter(description = "ID del viaggio da salvare", required = true)
        @PathVariable String travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @Operation(
        summary = "Rimuovi un viaggio dai segnalibri",
        description = "Rimuove un viaggio salvato dai segnalibri dell'utente"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bookmark rimosso con successo"),
        @ApiResponse(responseCode = "404", description = "Bookmark non trovato")
    })
    @DeleteMapping("/{travelId}")
    ResponseEntity<Map<String, String>> removeBookmark(
        @Parameter(description = "ID del viaggio da rimuovere", required = true)
        @PathVariable String travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @Operation(
        summary = "Ottieni tutti i segnalibri dell'utente",
        description = "Restituisce la lista di tutti i viaggi salvati dall'utente nei segnalibri"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista recuperata con successo")
    })
    @GetMapping
    ResponseEntity<List<BookmarkDTO>> getUserBookmarks(
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @Operation(
        summary = "Verifica se un viaggio è salvato nei segnalibri",
        description = "Controlla se l'utente ha salvato un determinato viaggio nei segnalibri"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stato verificato con successo")
    })
    @GetMapping("/check/{travelId}")
    ResponseEntity<Map<String, Boolean>> isBookmarked(
        @Parameter(description = "ID del viaggio da verificare", required = true)
        @PathVariable String travelId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
