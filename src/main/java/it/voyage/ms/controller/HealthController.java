package it.voyage.ms.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controller per health checks e keep-alive.
 * Questi endpoint sono pubblici e non richiedono autenticazione.
 */
@RestController
@RequestMapping("/health")
@Slf4j
public class HealthController {

    private LocalDateTime lastPingTime = LocalDateTime.now();
    private long pingCount = 0;

    /**
     * Endpoint di ping per verificare che il server sia attivo.
     * Chiamato dal GitHub Action ogni 10 minuti.
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        pingCount++;
        lastPingTime = LocalDateTime.now();
        
        log.info("🏓 [PING] Health check #{} received at {}", pingCount, lastPingTime);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", lastPingTime);
        response.put("pingCount", pingCount);
        response.put("message", "Server is alive and running");
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint di wake-up per riattivare il server.
     * Utile per forzare il risveglio dell'istanza Render.
     */
    @GetMapping("/wake")
    public ResponseEntity<Map<String, Object>> wake() {
        log.info("☕ [WAKE-UP] Wake-up call received at {}", LocalDateTime.now());
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "AWAKE");
        response.put("timestamp", LocalDateTime.now());
        response.put("message", "Server woke up successfully");
        response.put("lastPing", lastPingTime);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint standard di health check.
     * Fornisce informazioni dettagliate sullo stato del server.
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024;   // MB
        long usedMemory = totalMemory - freeMemory;
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("lastPing", lastPingTime);
        response.put("totalPings", pingCount);
        
        Map<String, Object> memory = new HashMap<>();
        memory.put("total", totalMemory + "MB");
        memory.put("used", usedMemory + "MB");
        memory.put("free", freeMemory + "MB");
        response.put("memory", memory);
        
        response.put("uptime", "Server is running");
        
        log.debug("💊 [HEALTH] Health check - Memory: {}MB used / {}MB total", usedMemory, totalMemory);
        
        return ResponseEntity.ok(response);
    }
}