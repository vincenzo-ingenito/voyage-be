package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per rappresentare un allegato con tutti i suoi metadati
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AttachmentUrlDTO {
    private String url;          // URL firmato del file (può essere null se non generato)
    private String fileName;     // Nome originale del file
    private String mimeType;     // Tipo MIME del file
    private String fileId;       // Path completo del file su Firebase Storage (SEMPRE presente)
}
