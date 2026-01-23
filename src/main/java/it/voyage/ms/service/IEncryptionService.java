package it.voyage.ms.service;

import it.voyage.ms.dto.response.EncryptedData;
import it.voyage.ms.dto.response.EncryptionMetadata;

/**
 * Service per la crittografia/decrittazione di file
 * Utilizza AES-256 in modalità GCM per garantire confidenzialità e integrità
 */
public interface IEncryptionService {
    
    /**
     * Cripta i dati forniti
     * @param data Dati da criptare
     * @param userId ID utente (usato per derivare la chiave)
     * @return EncryptedData contenente dati criptati e metadata
     */
    EncryptedData encrypt(byte[] data, String userId);
    
    /**
     * Decripta i dati forniti
     * @param encryptedData Dati criptati
     * @param userId ID utente
     * @param metadata Metadata di crittografia (IV, algorithm)
     * @return Dati in chiaro
     */
    byte[] decrypt(byte[] encryptedData, String userId, EncryptionMetadata metadata);
    
    /**
     * Verifica se un utente può decriptare un file
     * @param userId ID utente
     * @param metadata Metadata di crittografia
     * @return true se l'utente può decriptare
     */
    boolean canDecrypt(String userId, EncryptionMetadata metadata);
}
