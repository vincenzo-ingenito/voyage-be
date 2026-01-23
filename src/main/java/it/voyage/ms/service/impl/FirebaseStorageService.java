package it.voyage.ms.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirebaseStorageService {

	private final Storage storage;
	private final IEncryptionService encryptionService;
	//    private final String bucketName = "voyage-ed2d0.appspot.com";
	private final String bucketName = "voyage-ed2d0.firebasestorage.app";



	public FirebaseStorageService(Storage storage, @Autowired(required = false) IEncryptionService encryptionService) {
		this.storage = storage;
		this.encryptionService = encryptionService;
	}


	/**
	 * Carica un file su Firebase Storage e restituisce i metadati completi
	 * ✅ CRIPTA AUTOMATICAMENTE gli allegati (point-attachment)
	 * ✅ Lascia in chiaro le foto ricordo (day-memory, cover)
	 * 
	 * @param file Il file da caricare
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @param category Categoria del file (es. "day-memory", "point-attachment")
	 * @return FileMetadata con fileId, fileName, mimeType e encryption info
	 */
	public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, String travelId, String category) throws IOException {
		// ✅ Determina se il file deve essere criptato in base alla categoria
		boolean shouldEncrypt = "point-attachment".equals(category);
		
		if (shouldEncrypt && encryptionService != null) {
			log.info("📎 Caricamento file CRIPTATO per categoria: {}", category);
			return uploadEncryptedFile(file, userId, travelId, category);
		} else {
			log.info("📸 Caricamento file STANDARD per categoria: {}", category);
			return uploadStandardFile(file, userId, travelId, category);
		}
	}
	
	/**
	 * Carica un file STANDARD (non criptato) su Firebase Storage
	 * Usato per foto ricordo (day-memory) e cover images
	 */
	private FileMetadata uploadStandardFile(MultipartFile file, String userId, String travelId, String category) throws IOException {
		String originalFileName = file.getOriginalFilename();
		String contentType = file.getContentType();

		String filePath = String.format("travel-files/%s/%s/%s/%s_%s", 
				userId, 
				travelId, 
				category, 
				UUID.randomUUID().toString(), 
				originalFileName);

		BlobId blobId = BlobId.of(bucketName, filePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType(contentType)
				.setAcl(Collections.singletonList(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER)))
				.build();

		storage.create(blobInfo, file.getBytes());

		return new FileMetadata(filePath, originalFileName, contentType);
	}
	
	/**
	 * 🔐 Carica un file CRIPTATO su Firebase Storage
	 * Utilizzato automaticamente per gli allegati (point-attachment)
	 * 
	 * Processo:
	 * 1. Cripta il file con AES-256 GCM
	 * 2. Salva file criptato su Firebase
	 * 3. Salva metadata crittografia in MongoDB
	 */
	private FileMetadata uploadEncryptedFile(MultipartFile file, String userId, String travelId, String category) throws IOException {
		String originalFileName = file.getOriginalFilename();
		String originalContentType = file.getContentType();
		
		// 1. Cripta il file
		EncryptedData encryptedData = encryptionService.encrypt(file.getBytes(), userId);
		
		// 2. Crea path con estensione .encrypted
		String filePath = String.format("travel-files/%s/%s/%s/%s_%s.encrypted", 
				userId, 
				travelId, 
				category, 
				UUID.randomUUID().toString(), 
				originalFileName);
		
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
				.build();  // ⚠️ NO setAcl → File privato, accessibile solo via backend
		
		storage.create(blobInfo, encryptedData.getEncryptedBytes());
		
		log.info("🔐 File criptato caricato su: {}", filePath);
		
		// 5. Ritorna metadata completi (con encryption info)
		return new FileMetadata(filePath, originalFileName, originalContentType, encryptedData.getMetadata());
	}

	/**
	 * Metodo legacy per compatibilità - restituisce solo il fileId
	 * @deprecated Usare uploadFileWithMetadata per avere i metadati completi
	 */
	@Deprecated
	public String uploadFile(MultipartFile file, String userId, String travelId, String category) throws IOException {
		FileMetadata metadata = uploadFileWithMetadata(file, userId, travelId, category);
		return metadata.getFileId();
	}

	/**
	 * ✅ Genera un URL firmato (signed URL) con accesso pubblico per 7 giorni
	 * ⚠️ NOTA: Per file criptati, l'URL punta al file criptato!
	 *    Usare getPublicUrlWithDecryption() per ottenere file decriptati
	 */
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
	 * 🗑️ Elimina un file da Firebase Storage
	 * 
	 * @param fileId Path del file su Firebase Storage
	 * @return true se eliminato con successo, false altrimenti
	 */
	public boolean deleteFile(String fileId) {
		if (fileId == null || fileId.isEmpty()) {
			log.warn("Tentativo di eliminare file con fileId null o vuoto");
			return false;
		}
		
		try {
			BlobId blobId = BlobId.of(bucketName, fileId);
			boolean deleted = storage.delete(blobId);
			
			if (deleted) {
				log.info("🗑️ File eliminato da Firebase Storage: {}", fileId);
			} else {
				log.warn("⚠️ File non trovato su Firebase Storage (potrebbe essere già stato eliminato): {}", fileId);
			}
			
			return deleted;
			
		} catch (Exception e) {
			log.error("❌ Errore durante l'eliminazione del file {}: {}", fileId, e.getMessage(), e);
			return false;
		}
	}
	
	/**
	 * 🗑️ Elimina multipli file da Firebase Storage in batch
	 * 
	 * @param fileIds Lista di path dei file da eliminare
	 * @return Numero di file eliminati con successo
	 */
	public int deleteFiles(List<String> fileIds) {
		if (fileIds == null || fileIds.isEmpty()) {
			log.info("Nessun file da eliminare");
			return 0;
		}
		
		int deletedCount = 0;
		log.info("🗑️ Inizio eliminazione batch di {} file da Firebase Storage", fileIds.size());
		
		for (String fileId : fileIds) {
			if (deleteFile(fileId)) {
				deletedCount++;
			}
		}
		
		log.info("✅ Eliminati {}/{} file da Firebase Storage", deletedCount, fileIds.size());
		return deletedCount;
	}
	
	/**
	 * 🔓 Scarica e decripta un file criptato
	 * Ritorna i byte del file decriptato pronto per l'uso
	 * 
	 * @param fileId Path del file su Firebase Storage
	 * @param userId ID dell'utente proprietario
	 * @param encryption Metadata di crittografia
	 * @return byte[] del file decriptato
	 */
	public byte[] downloadAndDecrypt(String fileId, String userId, EncryptionMetadata encryption) {
		try {
			log.info("🔓 Download e decrittazione file: {}", fileId);
			
			// 1. Scarica file criptato da Firebase
			BlobId blobId = BlobId.of(bucketName, fileId);
			Blob blob = storage.get(blobId);
			
			if (blob == null) {
				throw new RuntimeException("File non trovato: " + fileId);
			}
			
			byte[] encryptedData = blob.getContent();
			
			// 2. Se il file non è criptato, ritornalo direttamente
			if (encryption == null || !encryption.isEncrypted()) {
				log.info("File non criptato, ritorno dati originali");
				return encryptedData;
			}
			
			// 3. Decripta il file
			if (encryptionService == null) {
				throw new RuntimeException("EncryptionService non disponibile");
			}
			
			byte[] decryptedData = encryptionService.decrypt(encryptedData, userId, encryption);
			
			log.info("✅ File decriptato con successo, dimensione: {} bytes", decryptedData.length);
			
			return decryptedData;
			
		} catch (Exception e) {
			log.error("Errore durante download e decrittazione di {}: {}", fileId, e.getMessage(), e);
			throw new RuntimeException("Errore durante download e decrittazione del file", e);
		}
	}

}
