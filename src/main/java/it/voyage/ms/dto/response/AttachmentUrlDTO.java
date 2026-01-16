package it.voyage.ms.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO per rappresentare un allegato con tutti i suoi metadati
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentUrlDTO {
    private String url;
    private String fileName;
    private String mimeType;
}
