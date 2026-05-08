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

    @Override
    public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category){
        log.info("Caricamento file per categoria: {}", category);

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

        try {
            storage.create(blobInfo, file.getBytes());
            log.info("File caricato con successo: {}", filePath);
            return new FileMetadata(filePath, originalFileName, contentType);
        } catch (IOException e) {
            log.error("Errore durante il caricamento del file {}: {}", filePath, e.getMessage());
            throw new BusinessException("Errore durante il caricamento del file");
        }
    }

    @Override
    public String getPublicUrl(String fileId) {
        if (fileId == null || fileId.isEmpty()) return null;

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

    @Override
    public Blob getBlob(String fileId) {
        if (fileId == null || fileId.isEmpty()) return null;

        try {
            Blob blob = storage.get(BlobId.of(BUCKET_NAME, fileId));
            if (blob == null) {
                log.warn("Blob non trovato per fileId: {}", fileId);
            }
            return blob;
        } catch (Exception e) {
            log.error("Errore nel recupero del blob {}: {}", fileId, e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] downloadFile(String fileId, String userId) {
        log.info("Download file: {}", fileId);
        try {
            Blob blob = getBlob(fileId);
            if (blob == null) {
                throw new BusinessException("File non trovato: " + fileId);
            }
            byte[] fileData = blob.getContent();
            log.info("File scaricato con successo, dimensione: {} bytes", fileData.length);
            return fileData;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante il download del file {}: {}", fileId, e.getMessage());
            throw new BusinessException("Errore durante il download del file: " + fileId);
        }
    }

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
                if (deleteBlob(blob)) {
                    deletedCount++;
                }
            }

            log.info("Cartella viaggio eliminata: {} ({} file)", folderPrefix, deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("Errore durante l'eliminazione della cartella {}: {}", folderPrefix, e.getMessage());
            return 0;
        }
    }

    // --- Metodi privati ---

    /**
     * Elimina un blob già recuperato da Storage.
     * Evita una seconda chiamata a storage.get() rispetto alla versione per path.
     */
    private boolean deleteBlob(Blob blob) {
        try {
            blob.delete();
            log.info("File eliminato: {}", blob.getName());
            return true;
        } catch (Exception e) {
            log.error("Impossibile eliminare file {}: {}", blob.getName(), e.getMessage());
            return false;
        }
    }

    private String fallbackUrl(String fileId) {
        return String.format("https://storage.googleapis.com/%s/%s", BUCKET_NAME, fileId);
    }
}