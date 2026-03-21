package it.voyage.ms.service.impl;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BlobListOption;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione unica per la gestione dei file su Firebase Storage.
 *
 * Unifica le responsabilità che erano distribuite tra FirebaseStorageService
 * e StorageService: upload, download, URL firmati, eliminazione singola
 * ed eliminazione massiva per viaggio.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FirebaseStorageService implements IFirebaseStorageService {

    private final Storage storage;

    private static final String BUCKET_NAME = "voyage-ed2d0.firebasestorage.app";

    // =========================================================================
    // UPLOAD
    // =========================================================================

    /**
     * Carica un file su Firebase Storage e restituisce i metadati completi.
     *
     * Il file viene reso pubblicamente accessibile tramite ACL e il path
     * segue la struttura: travel-files/{userId}/{travelId}/{category}/{uuid}_{fileName}
     */
    @Override
    public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category)
            throws IOException {
        log.info("Caricamento file per categoria: {}", category);

        String originalFileName = file.getOriginalFilename();
        String contentType = file.getContentType();
        String filePath = String.format("travel-files/%s/%s/%s/%s_%s",
                userId, travelId, category, UUID.randomUUID(), originalFileName);

        Map<String, String> metadata = new HashMap<>();
        metadata.put("original-filename", originalFileName);
        metadata.put("content-type", contentType);

        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(BUCKET_NAME, filePath))
                .setContentType(contentType)
                .setMetadata(metadata)
                .setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
                .build();

        storage.create(blobInfo, file.getBytes());

        return new FileMetadata(filePath, originalFileName, contentType);
    }

    // =========================================================================
    // DOWNLOAD / URL
    // =========================================================================

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
            throw new RuntimeException("File non trovato: " + fileId);
        }
        byte[] fileData = blob.getContent();
        log.info("File scaricato con successo, dimensione: {} bytes", fileData.length);
        return fileData;
    }

    // =========================================================================
    // DELETE — cartella viaggio
    // =========================================================================

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
                try {
                    if (storage.delete(blob.getBlobId())) {
                        deletedCount++;
                        log.debug("File eliminato: {}", blob.getName());
                    }
                } catch (Exception e) {
                    log.error("Errore eliminazione file {}: {}", blob.getName(), e.getMessage());
                }
            }

            log.info("Cartella viaggio eliminata: {} ({} file)", folderPrefix, deletedCount);
            return deletedCount;

        } catch (Exception e) {
            log.error("Errore durante l'eliminazione della cartella viaggio {}: {}", folderPrefix, e.getMessage(), e);
            return 0;
        }
    }

    // =========================================================================
    // DELETE — foto di un viaggio (per cancellazione account)
    // =========================================================================

    /**
     * Elimina tutte le foto associate a un viaggio (ricordi giornalieri + allFileIds).
     * Operazione best-effort: un errore su un singolo file non blocca gli altri.
     * Usato da UserService alla cancellazione dell'account.
     */
    @Override
    public void deletePhotosForTravel(TravelEty travel) {
        try {
            // Ricordi giornalieri (referenziati tramite URL pubblico)
            if (travel.getItinerary() != null) {
                travel.getItinerary().forEach(day -> {
                    if (day.getMemoryImageUrl() != null && !day.getMemoryImageUrl().isEmpty()) {
                        deleteFileByUrl(day.getMemoryImageUrl(), "foto ricordo giorno " + day.getDay());
                    }
                });
            }

            // Tutti gli altri file (referenziati tramite fileId/path diretto)
            if (travel.getAllFileIds() != null && !travel.getAllFileIds().isEmpty()) {
                travel.getAllFileIds().forEach(this::deleteFileById);
            }

        } catch (Exception e) {
            log.error("Errore durante l'eliminazione delle foto per il viaggio {}: {}",
                    travel.getId(), e.getMessage());
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Elimina un file a partire dal suo URL pubblico Firebase.
     * Estrae il path interno dall'URL prima di procedere.
     */
    private void deleteFileByUrl(String url, String label) {
        String path = extractStoragePath(url);
        if (path == null) {
            log.warn("Impossibile estrarre il path dall'URL per {}: {}", label, url);
            return;
        }
        try {
            Blob blob = storage.get(BlobId.of(BUCKET_NAME, path));
            if (blob != null) {
                blob.delete();
                log.info("Eliminato file ({}): {}", label, path);
            } else {
                log.debug("File non trovato su Storage ({}): {}", label, path);
            }
        } catch (Exception e) {
            log.warn("Impossibile eliminare file ({}): {} — {}", label, path, e.getMessage());
        }
    }

    /**
     * Elimina un file a partire dal suo fileId/path diretto.
     */
    private void deleteFileById(String fileId) {
        try {
            Blob blob = storage.get(BlobId.of(BUCKET_NAME, fileId));
            if (blob != null) {
                blob.delete();
                log.info("Eliminato file: {}", fileId);
            } else {
                log.debug("File non trovato su Storage: {}", fileId);
            }
        } catch (Exception e) {
            log.warn("Impossibile eliminare file {}: {}", fileId, e.getMessage());
        }
    }

    /**
     * Estrae il path interno di Firebase Storage da un URL pubblico.
     *
     * Formato atteso:
     * https://firebasestorage.googleapis.com/v0/b/BUCKET/o/PATH?alt=media&token=TOKEN
     */
    private String extractStoragePath(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            if (url.contains("/o/")) {
                String[] parts = url.split("/o/");
                if (parts.length > 1) {
                    String rawPath = parts[1].split("\\?")[0];
                    return URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
                }
            }
        } catch (Exception e) {
            log.error("Errore durante l'estrazione del path dall'URL {}: {}", url, e.getMessage());
        }
        return null;
    }

    /**
     * URL pubblico diretto come fallback quando la generazione dell'URL firmato fallisce.
     */
    private String fallbackUrl(String fileId) {
        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileId);
    }
}