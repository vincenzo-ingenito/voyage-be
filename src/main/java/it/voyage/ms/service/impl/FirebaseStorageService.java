package it.voyage.ms.service.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;

import it.voyage.ms.dto.response.EncryptedData;
import it.voyage.ms.dto.response.EncryptionMetadata;
import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.service.IEncryptionService;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirebaseStorageService implements IFirebaseStorageService {

	private final Storage storage;
	private final IEncryptionService encryptionService;
	private final String bucketName = "voyage-ed2d0.firebasestorage.app";



	public FirebaseStorageService(Storage storage, @Autowired(required = false) IEncryptionService encryptionService) {
		this.storage = storage;
		this.encryptionService = encryptionService;
	}

	/**
	 * Carica un file su Firebase Storage e restituisce i metadati completi
	 * 
	 * @param file Il file da caricare
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @param category Categoria del file (es. "day-memory", "point-attachment")
	 * @return FileMetadata con fileId, fileName, mimeType e encryption info
	 */
	public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, Long travelId, String category) throws IOException {
		boolean shouldEncrypt = "point-attachment".equals(category);
		
		if (shouldEncrypt && encryptionService != null) {
			log.info("Caricamento file CRIPTATO per categoria: {}", category);
			return uploadEncryptedFile(file, userId, travelId, category);
		} else {
			log.info("Caricamento file STANDARD per categoria: {}", category);
			return uploadStandardFile(file, userId, travelId, category);
		}
	}
	
	/**
	 * Carica un file STANDARD (non criptato) su Firebase Storage
	 * Usato per foto ricordo (day-memory) e cover images
	 */
	private FileMetadata uploadStandardFile(MultipartFile file, String userId, Long travelId, String category) throws IOException {
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
	
	/**
	 * Carica un file CRIPTATO su Firebase Storage
	 * Utilizzato automaticamente per gli allegati (point-attachment)
	 * 
	 * Processo:
	 * 1. Cripta il file con AES-256 GCM
	 * 2. Salva file criptato su Firebase
	 * 3. Salva metadata crittografia in MongoDB
	 */
	private FileMetadata uploadEncryptedFile(MultipartFile file, String userId, Long travelId, String category) throws IOException {
		String originalFileName = file.getOriginalFilename();
		String originalContentType = file.getContentType();
		
		// 1. Cripta il file
		EncryptedData encryptedData = encryptionService.encrypt(file.getBytes(), userId);
		
		// 2. Crea path con estensione .encrypted
		String filePath = String.format("travel-files/%s/%s/%s/%s_%s.encrypted", userId, travelId, category, UUID.randomUUID().toString(), originalFileName);
		
		// 3. Prepara metadata per Firebase (inclusi dati crittografia)
		Map<String, String> metadata = new HashMap<>();
		metadata.put("original-mime-type", originalContentType);
		metadata.put("original-filename", originalFileName);
		metadata.put("encrypted", "true");
		metadata.put("algorithm", encryptedData.getMetadata().getAlgorithm());
		metadata.put("iv", encryptedData.getMetadata().getIv());
		metadata.put("key-id", encryptedData.getMetadata().getKeyId());
		
		// 4. Upload file criptato su Firebase (NO ACL pubblico!)
		BlobId blobId = BlobId.of(bucketName, filePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType("application/octet-stream")  // File criptato = binario generico
				.setMetadata(metadata)
				.build();  
		
		storage.create(blobInfo, encryptedData.getEncryptedBytes());
		
		log.info("File criptato caricato su: {}", filePath);
		
		// 5. Ritorna metadata completi (con encryption info)
		return new FileMetadata(filePath, originalFileName, originalContentType, encryptedData.getMetadata());
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
	 * Scarica e decripta un file (versione semplificata per l'endpoint download)
	 * 
	 * @param fileId Path del file su Firebase Storage
	 * @param userId ID dell'utente proprietario
	 * @return byte[] del file decriptato
	 */
	@Override
	public byte[] downloadAndDecryptFile(String fileId, String userId) {
		try {
			log.info("Download e decrittazione file: {}", fileId);
			
			// 1. Scarica file criptato da Firebase
			Blob blob = getBlob(fileId);
			
			if (blob == null) {
				throw new RuntimeException("File non trovato: " + fileId);
			}
			
			byte[] fileData = blob.getContent();
			
			// 2. Verifica se il file è criptato dai metadata
			Map<String, String> metadata = blob.getMetadata();
			boolean isEncrypted = metadata != null && "true".equals(metadata.get("encrypted"));
			
			if (!isEncrypted) {
				log.info("File non criptato, ritorno dati originali");
				return fileData;
			}
			
			// 3. Crea EncryptionMetadata dai metadati del blob
			EncryptionMetadata encryptionMeta = new EncryptionMetadata(true, metadata.get("algorithm"), metadata.get("iv"), metadata.get("key-id"));
			
			// 4. Decripta il file
			if (encryptionService == null) {
				throw new RuntimeException("EncryptionService non disponibile");
			}
			
			byte[] decryptedData = encryptionService.decrypt(fileData, userId, encryptionMeta);
			log.info("File decriptato con successo, dimensione: {} bytes", decryptedData.length);
			return decryptedData;
		} catch (Exception e) {
			log.error("Errore durante download e decrittazione di {}: {}", fileId, e.getMessage(), e);
			throw new RuntimeException("Errore durante download e decrittazione del file", e);
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