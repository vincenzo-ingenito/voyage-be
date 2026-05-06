package it.voyage.ms.script;

import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Script di verifica per controllare che gli utenti AI siano stati creati correttamente.
 * 
 * Per eseguirlo, decommentare @Component e riavviare l'applicazione.
 */
@Slf4j
@AllArgsConstructor
// @Component // DECOMMENTARE PER ESEGUIRE LA VERIFICA
public class VerifyAiUsersScript implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TravelRepository travelRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("🔍 VERIFICA UTENTI AI");
        log.info("========================================");

        // Conta utenti AI
        long aiUsersCount = userRepository.findAll().stream()
            .filter(u -> u.isAiUser())
            .count();
        
        log.info("📊 Utenti AI trovati: {}", aiUsersCount);

        // Conta utenti AI con profilo pubblico
        long publicAiUsers = userRepository.findAll().stream()
            .filter(u -> u.isAiUser() && !u.isPrivate())
            .count();
        
        log.info("📊 Utenti AI pubblici: {}", publicAiUsers);

        // Conta viaggi degli utenti AI
        long aiTravelsCount = travelRepository.findAll().stream()
            .filter(t -> t.getUser() != null && t.getUser().isAiUser())
            .count();
        
        log.info("📊 Viaggi degli utenti AI: {}", aiTravelsCount);

        // Mostra primi 5 utenti AI
        log.info("📋 Primi 5 utenti AI:");
        userRepository.findAll().stream()
            .filter(u -> u.isAiUser())
            .limit(5)
            .forEach(u -> log.info("  - {} ({}) - Pubblico: {}", 
                u.getName(), u.getEmail(), !u.isPrivate()));

        log.info("========================================");
        log.info("✅ VERIFICA COMPLETATA");
        log.info("========================================");
    }
}