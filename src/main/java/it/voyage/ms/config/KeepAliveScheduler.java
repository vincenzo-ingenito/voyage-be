package it.voyage.ms.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableScheduling
@Slf4j
public class KeepAliveScheduler {

    /**
     * Keep-alive scheduler per evitare che Render metta in sleep l'istanza gratuita.
     * Esegue un log ogni 10 minuti (600000 ms).
     */
    @Scheduled(fixedRate = 600000) // 10 minuti in millisecondi
    public void keepAlive() {
        log.info("⏰ [KEEP-ALIVE] Server attivo - " + java.time.LocalDateTime.now());
    }
    
    /**
     * Health check alternativo ogni 5 minuti per maggiore sicurezza
     */
    @Scheduled(fixedRate = 300000) // 5 minuti
    public void healthCheck() {
        log.debug("💓 [HEARTBEAT] Health check - Memory: {}MB", 
            Runtime.getRuntime().totalMemory() / 1024 / 1024);
    }
}