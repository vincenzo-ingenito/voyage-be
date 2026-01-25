package it.voyage.ms.config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Configurazione e componente per il Rate Limiting delle Google Places API.
 * 
 * Limita il numero di richieste per utente per proteggere le API key di Google
 * e prevenire abusi.
 */
@Configuration
@EnableScheduling
public class RateLimitConfig {

    /**
     * Componente che gestisce il rate limiting per le Places API.
     * 
     * Limiti applicati:
     * - Max 50 richieste per utente al giorno
     * - Reset automatico ogni giorno a mezzanotte
     */
    @Component
    @Slf4j
    public static class PlacesRateLimiter {
        
        // Limiti configurabili
        private static final int MAX_REQUESTS_PER_USER_PER_DAY = 70;
        private static final int MAX_REQUESTS_PER_IP_PER_MINUTE = 10;
        
        // Storage in-memory per il conteggio richieste
        private final Map<String, UserRequestCounter> userRequests = new ConcurrentHashMap<>();
        private final Map<String, IpRequestCounter> ipRequests = new ConcurrentHashMap<>();
        
        /**
         * Verifica se un utente può effettuare una richiesta Places API.
         * 
         * @param userId ID dell'utente Firebase
         * @return true se la richiesta è permessa, false se il limite è stato raggiunto
         */
        public boolean allowUserRequest(String userId) {
            if (userId == null || userId.isEmpty()) {
                log.warn("Rate Limit: userId nullo o vuoto");
                return false;
            }
            
            UserRequestCounter counter = userRequests.computeIfAbsent(
                userId, 
                k -> new UserRequestCounter()
            );
            
            int currentCount = counter.incrementAndGet();
            
            if (currentCount > MAX_REQUESTS_PER_USER_PER_DAY) {
                log.warn("Rate Limit SUPERATO per user: {} - Richieste: {}/{}", 
                    userId, currentCount, MAX_REQUESTS_PER_USER_PER_DAY);
                return false;
            }
            
            if (currentCount % 10 == 0) {
                log.info("Rate Limit user {}: {}/{} richieste", 
                    userId, currentCount, MAX_REQUESTS_PER_USER_PER_DAY);
            }
            
            return true;
        }
        
        /**
         * Verifica se un IP può effettuare una richiesta (protezione DDoS).
         * 
         * @param ipAddress Indirizzo IP del client
         * @return true se la richiesta è permessa, false se il limite è stato raggiunto
         */
        public boolean allowIpRequest(String ipAddress) {
            if (ipAddress == null || ipAddress.isEmpty()) {
                return true; // Permetti se non possiamo determinare l'IP
            }
            
            IpRequestCounter counter = ipRequests.computeIfAbsent(
                ipAddress, 
                k -> new IpRequestCounter()
            );
            
            long now = System.currentTimeMillis();
            
            // Resetta il contatore se è passato più di 1 minuto
            if (now - counter.getLastReset() > 60000) {
                counter.reset(now);
            }
            
            int currentCount = counter.incrementAndGet();
            
            if (currentCount > MAX_REQUESTS_PER_IP_PER_MINUTE) {
                log.warn("Rate Limit IP SUPERATO per: {} - Richieste: {}/min", 
                    ipAddress, currentCount);
                return false;
            }
            
            return true;
        }
        
        /**
         * Ottiene il numero di richieste rimanenti per un utente.
         * 
         * @param userId ID dell'utente
         * @return numero di richieste rimanenti
         */
        public int getRemainingRequests(String userId) {
            UserRequestCounter counter = userRequests.get(userId);
            if (counter == null) {
                return MAX_REQUESTS_PER_USER_PER_DAY;
            }
            return Math.max(0, MAX_REQUESTS_PER_USER_PER_DAY - counter.getCount());
        }
        
        /**
         * Reset giornaliero dei contatori utente (eseguito ogni giorno a mezzanotte).
         */
        @Scheduled(cron = "0 0 0 * * *") // Ogni giorno a mezzanotte
        public void resetDailyCounters() {
            int usersCleared = userRequests.size();
            userRequests.clear();
            log.info("Rate Limit: Reset giornaliero completato. Contatori puliti per {} utenti", 
                usersCleared);
        }
        
        /**
         * Pulizia periodica dei contatori IP obsoleti (ogni 5 minuti).
         */
        @Scheduled(fixedDelay = 300000) // Ogni 5 minuti
        public void cleanupIpCounters() {
            final long now = System.currentTimeMillis();
            final int[] removed = {0}; // Array per permettere modifica in lambda
            
            ipRequests.entrySet().removeIf(entry -> {
                boolean isOld = now - entry.getValue().getLastReset() > 300000; // > 5 minuti
                if (isOld) removed[0]++;
                return isOld;
            });
            
            if (removed[0] > 0) {
                log.debug("Rate Limit: Rimossi {} contatori IP obsoleti", removed[0]);
            }
        }
        
        /**
         * Classe interna per contare le richieste per utente.
         */
        private static class UserRequestCounter {
            private int count = 0;
            
            public synchronized int incrementAndGet() {
                return ++count;
            }
            
            public synchronized int getCount() {
                return count;
            }
        }
        
        /**
         * Classe interna per contare le richieste per IP (con finestra temporale).
         */
        private static class IpRequestCounter {
            private int count = 0;
            private long lastReset = System.currentTimeMillis();
            
            public synchronized int incrementAndGet() {
                return ++count;
            }
            
            public synchronized void reset(long timestamp) {
                count = 0;
                lastReset = timestamp;
            }
            
            public synchronized long getLastReset() {
                return lastReset;
            }
        }
    }
}
