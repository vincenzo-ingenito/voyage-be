package it.voyage.ms.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirebaseStorageService implements IFirebaseStorageService {

	private final Storage storage;
	private final String bucketName = "voyage-ed2d0.firebasestorage.app";

	public FirebaseStorageService(Storage storage) {
		this.storage = storage;
	}

	/**
	 * Carica un file su Firebase Storage e restituisce i metadati completi
	 * 
	 * @param file Il file da caricare
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @param category Categoria del file (es. "day-memory", "point-attachment")
	 * @return FileMetadata con fileId, fileName, mimeType
	 */
	public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category) throws IOException {
		log.info("Caricamento file per categoria: {}", category);
		return uploadFile(file, userId, travelId, category);
	}
	
	/**
	 * Carica un file su Firebase Storage
	 * Tutti i file sono ora pubblicamente accessibili
	 */
	private FileMetadata uploadFile(MultipartFile file, String userId, Long travelId, String category) throws IOException {
		String originalFileName = file.getOriginalFilename();
		String contentType = file.getContentType();

		String filePath = String.format("travel-files/%s/%s/%s/%s_%s", userId, travelId, category, UUID.randomUUID().toString(), originalFileName);

		BlobId blobId = BlobId.of(bucketName, filePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType(contentType)
				.setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
				.build();

		storage.create(blobInfo, file.getBytes());

		return new FileMetadata(filePath, originalFileName, contentType);
	}

	public String getPublicUrl(String fileId) {
		if (fileId == null || fileId.isEmpty()) {
			return null;
		}
		
		try {
			// Ottieni il blob dal bucket
			BlobId blobId = BlobId.of(bucketName, fileId);
			Blob blob = storage.get(blobId);
			
			if (blob == null) {
				log.warn("Blob non trovato per fileId: {}", fileId);
				return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileId);
			}
			
			// Genera URL firmato valido per 7 giorni
			java.net.URL signedUrl = blob.signUrl(7, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature());
			return signedUrl.toString();
			
		} catch (Exception e) {
			log.error("Errore nella generazione dell'URL firmato per {}: {}", fileId, e.getMessage());
			return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileId);
		}
	}
	
	/**
	 * Ottiene il blob di Firebase Storage
	 * 
	 * @param fileId ID del file
	 * @return Blob di Firebase Storage
	 */
	@Override
	public Blob getBlob(String fileId) {
		if (fileId == null || fileId.isEmpty()) {
			return null;
		}
		
		try {
			BlobId blobId = BlobId.of(bucketName, fileId);
			return storage.get(blobId);
		} catch (Exception e) {
			log.error("Errore nel recupero del blob {}: {}", fileId, e.getMessage());
			return null;
		}
	}
	
	/**
	 * Scarica un file da Firebase Storage
	 * 
	 * @param fileId Path del file su Firebase Storage
	 * @param userId ID dell'utente proprietario
	 * @return byte[] del file
	 */
	@Override
	public byte[] downloadFile(String fileId, String userId) {
		try {
			log.info("Download file: {}", fileId);
			
			// Scarica file da Firebase
			Blob blob = getBlob(fileId);
			
			if (blob == null) {
				throw new RuntimeException("File non trovato: " + fileId);
			}
			
			byte[] fileData = blob.getContent();
			log.info("File scaricato con successo, dimensione: {} bytes", fileData.length);
			return fileData;
		} catch (Exception e) {
			log.error("Errore durante download di {}: {}", fileId, e.getMessage(), e);
			throw new RuntimeException("Errore durante download del file", e);
		}
	}
	
	
	/**
	 * Elimina tutti i file di un viaggio da Firebase Storage
	 * Cancella l'intera cartella travel-files/{userId}/{travelId}/
	 * 
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @return Numero di file eliminati
	 */
	public int deleteTravelFolder(String userId, Long travelId) {
		if (userId == null || travelId == null) {
			log.warn("Tentativo di eliminare cartella viaggio con parametri null");
			return 0;
		}
		
		String folderPrefix = String.format("travel-files/%s/%s/", userId, travelId);
		log.info("Eliminazione cartella viaggio: {}", folderPrefix);
		
		try {
			// Lista tutti i blob nella cartella del viaggio
			com.google.api.gax.paging.Page<Blob> blobs = storage.list(
				bucketName,
				com.google.cloud.storage.Storage.BlobListOption.prefix(folderPrefix)
			);
			
			int deletedCount = 0;
			for (Blob blob : blobs.iterateAll()) {
				try {
					boolean deleted = storage.delete(blob.getBlobId());
					if (deleted) {
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
	 
}