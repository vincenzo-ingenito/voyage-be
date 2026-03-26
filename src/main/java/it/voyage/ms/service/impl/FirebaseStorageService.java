package it.voyage.ms.service.impl;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
 *
 * NOTA — ACL pubbliche rimosse:
 * Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER) è deprecato nei bucket con
 * Uniform Bucket-Level Access attivo e causa errori silenziosi o eccezioni a
 * runtime. L'accesso ai file avviene esclusivamente tramite signed URL generati
 * da getPublicUrl(), senza esporre i file pubblicamente.
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
        String originalFileName = file.getOriginalFilename() != null
                ? file.getOriginalFilename()
                : "file";
        String contentType = file.getContentType() != null
                ? file.getContentType()
                : "application/octet-stream";

        String filePath = String.format("travel-files/%s/%s/%s/%s_%s",
                userId, travelId, category, UUID.randomUUID(), originalFileName);

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
            throw new BusinessException("File non trovato: " + fileId);
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

    // =========================================================================
    // DELETE — foto di un viaggio (per cancellazione account)
    // =========================================================================

    /**
     * Elimina tutte le foto associate a un viaggio (ricordi giornalieri + allFileIds).
     * Operazione best-effort: un errore su un singolo file non blocca gli altri.
     * Usato da UserService alla cancellazione dell'account.
     *
     * NOTA — memoryImageUrl vs fileId:
     * Con l'architettura attuale, le foto dei giorni sono referenziate tramite
     * memoryImageIndex e incluse in getAllFileIds(). Il ramo memoryImageUrl è
     * mantenuto per compatibilità con eventuali record creati con la vecchia
     * architettura che salvava l'URL diretto anziché l'indice.
     */
    @Override
    public void deletePhotosForTravel(TravelEty travel) {
        try {
            // Vecchia architettura: foto referenziate tramite URL diretto
            if (travel.getItinerary() != null) {
                travel.getItinerary().forEach(day -> {
                    if (day.getMemoryImageUrl() != null && !day.getMemoryImageUrl().isEmpty()) {
                        String path = extractStoragePath(day.getMemoryImageUrl());
                        if (path != null) {
                            deleteBlob(path, "foto ricordo giorno " + day.getDay());
                        } else {
                            log.warn("Impossibile estrarre il path dall'URL per foto ricordo giorno {}: {}",
                                    day.getDay(), day.getMemoryImageUrl());
                        }
                    }
                });
            }

            // Architettura corrente: tutti i file referenziati tramite fileId/path
            if (travel.getAllFileIds() != null && !travel.getAllFileIds().isEmpty()) {
                travel.getAllFileIds().forEach(fileId ->
                        deleteBlob(fileId, "file viaggio " + travel.getId()));
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
     * Estrae il path interno di Firebase Storage da un URL pubblico.
     *
     * Formato atteso:
     * https://firebasestorage.googleapis.com/v0/b/BUCKET/o/PATH?alt=media&token=TOKEN
     *
     * @return path decodificato, o null se il formato non è riconoscibile
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
     * URL pubblico diretto come fallback quando la generazione del signed URL fallisce.
     * Funziona solo su bucket con accesso pubblico uniforme abilitato.
     */
    private String fallbackUrl(String fileId) {
        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileId);
    }
}