package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadata di crittografia per un file
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EncryptionMetadata {
    
    /**
     * Indica se il file è criptato
     */
    private boolean encrypted;
    
    /**
     * Algoritmo di crittografia utilizzato (es. "AES/GCM/NoPadding")
     */
    private String algorithm;
    
    /**
     * Initialization Vector in Base64
     */
    private String iv;
    
    /**
     * ID della chiave utilizzata per la crittografia
     */
    private String keyId;
    
    /**
     * Costruttore per file non criptati
     */
    public static EncryptionMetadata unencrypted() {
        return new EncryptionMetadata(false, null, null, null);
    }
}
