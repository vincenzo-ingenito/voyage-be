package it.voyage.ms.controller.impl;

import it.voyage.ms.dto.response.AiUserStats;
import it.voyage.ms.dto.response.DeletionResult;
import it.voyage.ms.dto.response.PopulationResult;
import it.voyage.ms.service.impl.AiUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller per la gestione degli utenti AI mock.
 * Fornisce endpoint per creare, eliminare e monitorare gli utenti AI.
 */
@RestController
@RequestMapping("/api/ai-users")
@RequiredArgsConstructor
@Slf4j
public class AiUserController {

    private final AiUserService aiUserService;

    /**
     * Ottiene statistiche sugli utenti AI presenti nel sistema.
     * 
     * GET /api/ai-users/stats
     * 
     * @return Statistiche sugli utenti AI
     */
    @GetMapping("/stats")
    public ResponseEntity<AiUserStats> getAiUserStats() {
        log.info("📊 Richiesta statistiche utenti AI");
        AiUserStats stats = aiUserService.getAiUserStats();
        log.info("Statistiche: {} utenti AI totali, {} pubblici, {} viaggi", 
            stats.getTotalAiUsers(), stats.getPublicAiUsers(), stats.getAiTravels());
        return ResponseEntity.ok(stats);
    }

    /**
     * Popola il database con utenti AI mock.
     * 
     * POST /api/ai-users/populate?count=100
     * 
     * @param count Numero di utenti AI da creare (default: 100, max: 200)
     * @return Risultato dell'operazione con statistiche
     */
    @PostMapping("/populate")
    public ResponseEntity<Map<String, Object>> populateAiUsers(
        @RequestParam(defaultValue = "100") int count
    ) {
        log.info("🤖 Richiesta popolazione di {} utenti AI", count);
        
        // Limita il numero massimo per evitare sovraccarichi
        if (count > 200) {
            log.warn("Numero richiesto ({}) supera il limite massimo (200). Limitato a 200.", count);
            count = 200;
        }
        
        if (count < 1) {
            log.warn("Numero richiesto ({}) non valido. Impostato a 1.", count);
            count = 1;
        }

        try {
            PopulationResult result = aiUserService.populateAiUsers(count);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Popolazione utenti AI completata");
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());
            response.put("errors", result.getErrors());
            
            log.info("✅ Popolazione completata: {} successi, {} errori", 
                result.getSuccessCount(), result.getErrorCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore durante la popolazione degli utenti AI", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Errore durante la popolazione: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Elimina tutti gli utenti AI dal database e da Firebase Auth.
     * 
     * DELETE /api/ai-users/all
     * 
     * ATTENZIONE: Questa operazione è irreversibile!
     * 
     * @return Risultato dell'operazione con statistiche
     */
    @DeleteMapping("/all")
    public ResponseEntity<Map<String, Object>> deleteAllAiUsers() {
        log.warn("🗑️ Richiesta eliminazione di TUTTI gli utenti AI");
        
        try {
            DeletionResult result = aiUserService.deleteAllAiUsers();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Eliminazione utenti AI completata");
            response.put("totalUsers", result.getTotalUsers());
            response.put("successCount", result.getSuccessCount());
            response.put("errorCount", result.getErrorCount());
            response.put("errors", result.getErrors());
            
            log.info("✅ Eliminazione completata: {}/{} utenti eliminati, {} errori", 
                result.getSuccessCount(), result.getTotalUsers(), result.getErrorCount());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Errore durante l'eliminazione degli utenti AI", e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Errore durante l'eliminazione: " + e.getMessage());
            response.put("error", e.getMessage());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Endpoint di test per verificare che il controller sia attivo.
     * 
     * GET /api/ai-users/health
     * 
     * @return Messaggio di conferma
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "AI User Controller is running");
        return ResponseEntity.ok(response);
    }
}