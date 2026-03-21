package it.voyage.ms.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Blob;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.repository.entity.TravelEty;

/**
 * Contratto unico per la gestione dei file su Firebase Storage.
 *
 * Responsabilità coperte:
 * - Upload di file (con metadati)
 * - Download di file
 * - Generazione URL pubblici/firmati
 * - Eliminazione di file singoli o intere cartelle di viaggio
 * - Eliminazione di foto associate a un viaggio (da URL o fileId)
 */
public interface IFirebaseStorageService {

    /**
     * Carica un file su Firebase Storage e restituisce i metadati completi.
     */
    FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category) throws IOException;

    /**
     * Restituisce l'URL firmato (valido 7 giorni) per accedere al file.
     */
    String getPublicUrl(String fileId);

    /**
     * Restituisce il blob grezzo di Firebase Storage.
     */
    Blob getBlob(String fileId);

    /**
     * Scarica il contenuto binario di un file.
     */
    byte[] downloadFile(String fileId, String userId);

    /**
     * Elimina tutti i file nella cartella travel-files/{userId}/{travelId}/.
     *
     * @return numero di file eliminati
     */
    int deleteTravelFolder(String userId, Long travelId);

    /**
     * Elimina tutte le foto associate a un viaggio (ricordi giornalieri + allFileIds).
     * Operazione best-effort: un errore su un singolo file non blocca gli altri.
     */
    void deletePhotosForTravel(TravelEty travel);
}