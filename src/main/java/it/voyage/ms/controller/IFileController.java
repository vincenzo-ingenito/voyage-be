package it.voyage.ms.controller;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import it.voyage.ms.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controller per la gestione del download di file criptati
 * Utilizza PBKDF2 per derivare le chiavi di decrittografia dal Firebase UID dell'utente
 */
@RequestMapping("/api/files")
@Tag(name = "File Management", description = "API per il download sicuro di file criptati")
public interface IFileController {

   
    @GetMapping("/download")
    @Operation(summary = "Download file criptato", description = "Scarica e decripta un file usando la chiave derivata dal Firebase UID dell'utente. " +
                     "La decrittografia usa PBKDF2 con 100,000 iterazioni per derivare la chiave AES-256.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File scaricato e decriptato con successo"),
        @ApiResponse(responseCode = "400", description = "File ID non valido o mancante"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato"),
        @ApiResponse(responseCode = "403", description = "Utente non autorizzato ad accedere al file"),
        @ApiResponse(responseCode = "404", description = "File non trovato"),
        @ApiResponse(responseCode = "500", description = "Errore durante la decrittografia del file")
    })
    ResponseEntity<Resource> downloadFile(
        @Parameter(description = "ID del file su Firebase Storage", required = true)
        @RequestParam("fileId") String fileId,
        @Parameter(hidden = true)
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
    
    @GetMapping("/public-download")
    @Operation(summary = "Download file pubblico (non criptato)", 
               description = "Scarica file pubblici come foto ricordo che non sono criptati. " +
                            "Usato per day-memory e cover images.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File scaricato con successo"),
        @ApiResponse(responseCode = "400", description = "File ID non valido o mancante"),
        @ApiResponse(responseCode = "401", description = "Utente non autenticato"),
        @ApiResponse(responseCode = "404", description = "File non trovato"),
        @ApiResponse(responseCode = "500", description = "Errore durante il download del file")
    })
    ResponseEntity<Resource> downloadPublicFile(
        @Parameter(description = "ID del file pubblico su Firebase Storage", required = true)
        @RequestParam("fileId") String fileId,
        @Parameter(hidden = true)
        @AuthenticationPrincipal CustomUserDetails userDetails
    );
}
