package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Contenitore per dati criptati e relativi metadata
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptedData {
    
    /**
     * Dati criptati in formato byte array
     */
    private byte[] encryptedBytes;
    
    /**
     * Metadata di crittografia (IV, algorithm, keyId)
     */
    private EncryptionMetadata metadata;
}
