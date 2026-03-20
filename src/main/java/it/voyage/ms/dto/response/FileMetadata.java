package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Metadati di un file caricato su Firebase Storage
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileMetadata {
    private String fileId;        // Il path del file su Firebase Storage
    private String fileName;      // Nome originale del file
    private String mimeType;      // Tipo MIME del file
}