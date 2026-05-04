package it.voyage.ms.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseStorageService implements IFirebaseStorageService {

    private final Storage storage;

    private static final String BUCKET_NAME = "voyage-ed2d0.firebasestorage.app";


    /**
     * Carica un file su Firebase Storage e restituisce i metadati completi.
     *
     * Il path segue la struttura:
     * travel-files/{userId}/{travelId}/{category}/{uuid}_{fileName}
     *
     * L'accesso al file avviene tramite signed URL (getPublicUrl), non tramite
     * ACL pubbliche.
     */
    @Override
    public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category)
            throws IOException {
        log.info("Caricamento file per categoria: {}", category);

        // getOriginalFilename() e getContentType() possono restituire null
        String originalFileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

        String filePath = String.format("travel-files/%s/%s/%s/%s_%s", userId, travelId, category, UUID.randomUUID(), originalFileName);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-filename", originalFileName);
        metadata.put("content-type", contentType);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(BUCKET_NAME, filePath))
                .setContentType(contentType)
                .setMetadata(metadata)
                .build();

        storage.create(blobInfo, file.getBytes());

        log.info("File caricato con successo: {}", filePath);
        return new FileMetadata(filePath, originalFileName, contentType);
    }
 
    /**
     * Restituisce un URL firmato valido per 7 giorni.
     * In caso di errore restituisce un URL pubblico diretto come fallback.
     */
    @Override
    public String getPublicUrl(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return null;
        }
        try {
            Blob blob = storage.get(BlobId.of(BUCKET_NAME, fileId));
            if (blob == null) {
                log.warn("Blob non trovato per fileId: {}", fileId);
                return fallbackUrl(fileId);
            }
            return blob.signUrl(7, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature()).toString();
        } catch (Exception e) {
            log.error("Errore nella generazione dell'URL firmato per {}: {}", fileId, e.getMessage());
            return fallbackUrl(fileId);
        }
    }

    /**
     * Restituisce il blob grezzo di Firebase Storage, o null se non trovato.
     */
    @Override
    public Blob getBlob(String fileId) {
        if (fileId == null || fileId.isEmpty()) {
            return null;
        }
        try {
            return storage.get(BlobId.of(BUCKET_NAME, fileId));
        } catch (Exception e) {
            log.error("Errore nel recupero del blob {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    /**
     * Scarica il contenuto binario di un file da Firebase Storage.
     */
    @Override
    public byte[] downloadFile(String fileId, String userId) {
        log.info("Download file: {}", fileId);
        Blob blob = getBlob(fileId);
        if (blob == null) {
            throw new BusinessException("File non trovato: " + fileId);
        }
        byte[] fileData = blob.getContent();
        log.info("File scaricato con successo, dimensione: {} bytes", fileData.length);
        return fileData;
    }
 
    /**
     * Elimina tutti i file nella cartella travel-files/{userId}/{travelId}/.
     * Usato alla cancellazione di un intero viaggio.
     *
     * @return numero di file eliminati
     */
    @Override
    public int deleteTravelFolder(String userId, Long travelId) {
        if (userId == null || travelId == null) {
            log.warn("Tentativo di eliminare cartella viaggio con parametri null");
            return 0;
        }

        String folderPrefix = String.format("travel-files/%s/%s/", userId, travelId);
        log.info("Eliminazione cartella viaggio: {}", folderPrefix);

        try {
            Page<Blob> blobs = storage.list(BUCKET_NAME, BlobListOption.prefix(folderPrefix));
            int deletedCount = 0;

            for (Blob blob : blobs.iterateAll()) {
                if (deleteBlob(blob.getName(), "cartella viaggio")) {
                    deletedCount++;
                }
            }

            log.info("Cartella viaggio eliminata: {} ({} file)", folderPrefix, deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("Errore durante l'eliminazione della cartella viaggio {}: {}", folderPrefix, e.getMessage(), e);
            return 0;
        }
    }
 
    /**
     * Elimina un singolo blob da Firebase Storage tramite path diretto.
     * Operazione best-effort: logga warning in caso di errore senza propagarlo.
     *
     * @param path  path interno del blob nel bucket
     * @param label descrizione contestuale usata nel log
     * @return true se eliminato, false se non trovato o in caso di errore
     */
    private boolean deleteBlob(String path, String label) {
        try {
            Blob blob = storage.get(BlobId.of(BUCKET_NAME, path));
            if (blob != null) {
                blob.delete();
                log.info("Eliminato file ({}): {}", label, path);
                return true;
            } else {
                log.debug("File non trovato su Storage ({}): {}", label, path);
                return false;
            }
        } catch (Exception e) {
            log.warn("Impossibile eliminare file ({}): {} — {}", label, path, e.getMessage());
            return false;
        }
    }
 
    /**
     * URL pubblico diretto come fallback quando la generazione del signed URL fallisce.
     * Funziona solo su bucket con accesso pubblico uniforme abilitato.
     */
    private String fallbackUrl(String fileId) {
        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileId);
    }
}