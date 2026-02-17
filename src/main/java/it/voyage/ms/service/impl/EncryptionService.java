package it.voyage.ms.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import it.voyage.ms.dto.response.EncryptedData;
import it.voyage.ms.dto.response.EncryptionMetadata;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.service.IEncryptionService;
import lombok.extern.slf4j.Slf4j;

 
@Service
@Slf4j
public class EncryptionService implements IEncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 12;  // 96 bits per GCM
    private static final int TAG_SIZE = 128; // 128 bits authentication tag
    
    // PBKDF2 parameters
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 100000; // OWASP raccomanda 100k+ per sicurezza
     
    @Value("${encryption.salt:VoyageApp2024SecureSalt_Change_In_Production}")
    private String applicationSalt;
     
    @Value("${encryption.cache.enabled:true}")
    private boolean cacheEnabled;
    
    // Cache delle chiavi derivate (opzionale, per performance)
    private final Map<String, SecretKey> keyCache = new ConcurrentHashMap<>();
    
    private final SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public EncryptedData encrypt(byte[] data, String firebaseUid) {
        try {
            log.info("Criptando file per utente Firebase: {}, dimensione: {} bytes", firebaseUid, data.length);
            
            SecretKey key = deriveKeyFromFirebaseUid(firebaseUid);
            
            byte[] iv = generateIV();
            
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] encryptedBytes = cipher.doFinal(data);
            
            // 4. Crea metadata per la decrittografia
            EncryptionMetadata metadata = new EncryptionMetadata(true, ALGORITHM, Base64.getEncoder().encodeToString(iv), generateKeyId(firebaseUid));
            
            log.info("File criptato con successo. Dimensione criptata: {} bytes", encryptedBytes.length);
            
            return new EncryptedData(encryptedBytes, metadata);
            
        } catch (Exception e) {
            log.error("Errore durante la crittografia per utente {}: {}", firebaseUid, e.getMessage(), e);
            throw new BusinessException("Errore durante la crittografia del file", e);
        }
    }
    
    @Override
    public byte[] decrypt(byte[] encryptedData, String firebaseUid, EncryptionMetadata metadata) {
        try {
            log.info("Decriptando file per utente Firebase: {}, dimensione: {} bytes", firebaseUid, encryptedData.length);
            
            if (!metadata.isEncrypted()) {
                throw new IllegalArgumentException("Il file non è criptato");
            }
            
            SecretKey key = deriveKeyFromFirebaseUid(firebaseUid);
            byte[] iv = Base64.getDecoder().decode(metadata.getIv());
            
            // 3. Decripta i dati usando AES-GCM
            Cipher cipher = Cipher.getInstance(metadata.getAlgorithm());
            GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] decryptedBytes = cipher.doFinal(encryptedData);
            
            log.info("File decriptato con successo. Dimensione originale: {} bytes", decryptedBytes.length);
            
            return decryptedBytes;
        } catch (Exception e) {
            log.error("Errore durante la decrittografia per utente {}: {}", firebaseUid, e.getMessage(), e);
            throw new BusinessException("Errore durante la decrittografia del file", e);
        }
    }
     
    private SecretKey deriveKeyFromFirebaseUid(String firebaseUid) {
        // Usa cache per performance (se abilitata)
        if (cacheEnabled) {
            return keyCache.computeIfAbsent(firebaseUid, this::deriveKey);
        } else {
            return deriveKey(firebaseUid);
        }
    }
    
    /**
     * Metodo interno che esegue effettivamente la derivazione della chiave
     */
    private SecretKey deriveKey(String firebaseUid) {
        try {
            log.debug("Derivando chiave AES-256 da Firebase UID: {}", firebaseUid);
            
            byte[] salt = (applicationSalt + firebaseUid).getBytes(StandardCharsets.UTF_8);
            
            // Configura PBKDF2
            KeySpec spec = new PBEKeySpec(firebaseUid.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE);
            
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            
            return new SecretKeySpec(keyBytes, "AES");
            
        } catch (Exception e) {
            log.error("Errore nella derivazione chiave per UID {}: {}", firebaseUid, e.getMessage());
            throw new BusinessException("Errore nella derivazione della chiave", e);
        }
    }
    
    /**
     * Genera un Initialization Vector casuale
     * Deve essere diverso per ogni operazione di encryption
     */
    private byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE];
        secureRandom.nextBytes(iv);
        return iv;
    }
    
    /**
     * Genera un ID univoco per la chiave dell'utente
     */
    private String generateKeyId(String firebaseUid) {
        return "firebase-key-" + firebaseUid;
    }
}
