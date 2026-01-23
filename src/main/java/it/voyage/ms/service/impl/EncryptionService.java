package it.voyage.ms.service.impl;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import it.voyage.ms.dto.response.EncryptedData;
import it.voyage.ms.dto.response.EncryptionMetadata;
import it.voyage.ms.service.IEncryptionService;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del servizio di crittografia
 * Utilizza AES-256 in modalità GCM per garantire confidenzialità e integrità
 * 
 * NOTA: In produzione, le chiavi dovrebbero essere gestite da un KMS (Key Management Service)
 * come AWS KMS, Google Cloud KMS, o HashiCorp Vault
 */
@Service
@Slf4j
public class EncryptionService implements IEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;  // 96 bits per GCM
    private static final int TAG_SIZE = 128; // 128 bits authentication tag
    
    // ⚠️ ATTENZIONE: In produzione, usare un KMS invece di memorizzare chiavi in memoria
    // Questa è solo un'implementazione di esempio per development
    private final Map<String, SecretKey> userKeys = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public EncryptedData encrypt(byte[] data, String userId) {
        try {
            log.info("Criptando file per utente: {}, dimensione: {} bytes", userId, data.length);
            
            // 1. Ottieni o crea chiave per l'utente
            SecretKey key = getUserKey(userId);
            
            // 2. Genera IV casuale
            byte[] iv = generateIV();
            
            // 3. Cripta i dati
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encryptedBytes = cipher.doFinal(data);
            
            // 4. Crea metadata
            EncryptionMetadata metadata = new EncryptionMetadata(
                true,
                ALGORITHM,
                Base64.getEncoder().encodeToString(iv),
                generateKeyId(userId)
            );
            
            log.info("File criptato con successo. Dimensione criptata: {} bytes", encryptedBytes.length);
            
            return new EncryptedData(encryptedBytes, metadata);
            
        } catch (Exception e) {
            log.error("Errore durante la crittografia per utente {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Errore durante la crittografia del file", e);
        }
    }
    
    @Override
    public byte[] decrypt(byte[] encryptedData, String userId, EncryptionMetadata metadata) {
        try {
            log.info("Decriptando file per utente: {}, dimensione: {} bytes", userId, encryptedData.length);
            
            if (!metadata.isEncrypted()) {
                throw new IllegalArgumentException("Il file non è criptato");
            }
            
            // 1. Ottieni chiave dell'utente
            SecretKey key = getUserKey(userId);
            
            // 2. Decodifica IV
            byte[] iv = Base64.getDecoder().decode(metadata.getIv());
            
            // 3. Decripta i dati
            Cipher cipher = Cipher.getInstance(metadata.getAlgorithm());
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);
            
            log.info("File decriptato con successo. Dimensione originale: {} bytes", decryptedBytes.length);
            
            return decryptedBytes;
            
        } catch (Exception e) {
            log.error("Errore durante la decrittografia per utente {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Errore durante la decrittografia del file", e);
        }
    }
    
    @Override
    public boolean canDecrypt(String userId, EncryptionMetadata metadata) {
        if (!metadata.isEncrypted()) {
            return true; // File non criptato, accessibile da tutti
        }
        
        // Verifica che l'utente abbia la chiave giusta
        String expectedKeyId = generateKeyId(userId);
        return expectedKeyId.equals(metadata.getKeyId());
    }
    
    /**
     * Ottiene o genera la chiave di crittografia per un utente
     * ⚠️ IN PRODUZIONE: Sostituire con chiamata a KMS (Key Management Service)
     */
    private SecretKey getUserKey(String userId) {
        return userKeys.computeIfAbsent(userId, id -> {
            try {
                log.info("Generando nuova chiave AES-256 per utente: {}", id);
                KeyGenerator keyGen = KeyGenerator.getInstance("AES");
                keyGen.init(KEY_SIZE, secureRandom);
                return keyGen.generateKey();
            } catch (Exception e) {
                throw new RuntimeException("Errore nella generazione della chiave", e);
            }
        });
    }
    
    /**
     * Genera un Initialization Vector casuale
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    /**
     * Genera un ID univoco per la chiave dell'utente
     */
    private String generateKeyId(String userId) {
        return "key-" + userId;
    }
    
    /**
     * Metodo di utilità per inizializzare chiavi da un KMS esterno
     * Da usare in produzione
     */
    public void loadKeyFromKMS(String userId, String base64EncodedKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(base64EncodedKey);
            SecretKey key = new SecretKeySpec(keyBytes, "AES");
            userKeys.put(userId, key);
            log.info("Chiave caricata da KMS per utente: {}", userId);
        } catch (Exception e) {
            log.error("Errore nel caricamento chiave da KMS per utente {}: {}", userId, e.getMessage());
            throw new RuntimeException("Errore nel caricamento chiave da KMS", e);
        }
    }
}
