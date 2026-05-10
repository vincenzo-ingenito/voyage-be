package it.voyage.ms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

/**
 * Proprietà di configurazione personalizzate per l'applicazione Voyage.
 * 
 * Queste proprietà sono caricate da application.properties con il prefisso "voyage".
 * 
 * @author Voyage Team
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "voyage")
@Data
public class VoyageConfigProperties {
    
    /**
     * Configurazioni relative al feed dei viaggi
     */
    private Feed feed = new Feed();
    
    /**
     * Configurazioni per il feed dei viaggi
     */
    @Data
    public static class Feed {
        /**
         * Se true, include automaticamente i viaggi degli utenti mock nel feed di tutti gli utenti.
         * 
         * Gli utenti mock sono identificati dal pattern ID 'mock-user-*'.
         * Quando questa proprietà è abilitata, tutti gli utenti vedranno i viaggi mock
         * nel loro feed senza necessità di relazioni di amicizia.
         * 
         * <p><b>Casi d'uso:</b></p>
         * <ul>
         *   <li>Demo e presentazioni con dati realistici</li>
         *   <li>Testing con itinerari di esempio</li>
         *   <li>Sviluppo locale con dati mock</li>
         *   <li>Onboarding nuovi utenti con contenuti di esempio</li>
         * </ul>
         * 
         * <p><b>Raccomandazioni per ambiente:</b></p>
         * <ul>
         *   <li><b>Development:</b> true (comodo per testing)</li>
         *   <li><b>Staging:</b> true (utile per demo)</li>
         *   <li><b>Production:</b> false (privacy e performance)</li>
         * </ul>
         * 
         * <p><b>Impatto sulle performance:</b></p>
         * Minimo - la query usa una UNION con condizione WHERE, quindi quando false
         * non aggiunge overhead significativo.
         * 
         * @default false (per sicurezza in produzione)
         */
        private boolean includeMockUsers = false;
    }
}