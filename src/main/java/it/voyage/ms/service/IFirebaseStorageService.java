package it.voyage.ms.service;

import com.google.cloud.storage.Blob;
import it.voyage.ms.dto.response.FileMetadata;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Interfaccia per il servizio di storage Firebase
 */
public interface IFirebaseStorageService {

    /**
     * Carica un file su Firebase Storage con metadata encryption info
     * 
     * @param file File da caricare
     * @param userId ID utente proprietario
     * @param travelId ID del viaggio
     * @param category Categoria del file (es. "day-memory", "point-attachment")
     * @return FileMetadata con informazioni sul file caricato
     * @throws IOException in caso di errore
     */
    FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category) throws IOException;

    /**
     * Ottiene l'URL pubblico di un file (signed URL con scadenza)
     * 
     * @param fileId ID del file
     * @return URL pubblico temporaneo
     */
    String getPublicUrl(String fileId);

    /**
     * Scarica e decripta un file
     * 
     * @param fileId ID del file
     * @param userId ID dell'utente richiedente
     * @return Dati del file decriptati
     */
    byte[] downloadAndDecryptFile(String fileId, String userId);

    /**
     * Ottiene il blob di Firebase Storage
     * 
     * @param fileId ID del file
     * @return Blob di Firebase Storage
     */
    Blob getBlob(String fileId);

   
}