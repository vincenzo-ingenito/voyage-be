package it.voyage.ms.service.impl;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirebaseStorageService {

	private final Storage storage;
	//    private final String bucketName = "voyage-ed2d0.appspot.com";
	private final String bucketName = "voyage-ed2d0.firebasestorage.app";



	public FirebaseStorageService(Storage storage) {
		this.storage = storage;
	}


	/**
	 * Carica un file su Firebase Storage e restituisce i metadati completi
	 * @param file Il file da caricare
	 * @param userId ID dell'utente
	 * @param travelId ID del viaggio
	 * @param category Categoria del file (es. "day-memory", "point-attachment")
	 * @return FileMetadata con fileId, fileName e mimeType
	 */
	public FileMetadata uploadFileWithMetadata(MultipartFile file, String userId, String travelId, String category) throws IOException {
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
	 * Metodo legacy per compatibilità - restituisce solo il fileId
	 * @deprecated Usare uploadFileWithMetadata per avere i metadati completi
	 */
	@Deprecated
	public String uploadFile(MultipartFile file, String userId, String travelId, String category) throws IOException {
		FileMetadata metadata = uploadFileWithMetadata(file, userId, travelId, category);
		return metadata.getFileId();
	}

	/**
	 * ✅ FIX: Genera un URL firmato (signed URL) con accesso pubblico per 7 giorni
	 * Gli URL firmati funzionano per il download senza autenticazione
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
				// Fallback all'URL semplice
				return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileId);
			}
			
			// ✅ FIX: Genera URL firmato valido per 7 giorni
			// Questo permette il download diretto senza autenticazione
			java.net.URL signedUrl = blob.signUrl(7, TimeUnit.DAYS, Storage.SignUrlOption.withV4Signature());
			return signedUrl.toString();
			
		} catch (Exception e) {
			log.error("Errore nella generazione dell'URL firmato per {}: {}", fileId, e.getMessage());
			// Fallback all'URL semplice in caso di errore
			return String.format("https://storage.googleapis.com/%s/%s", bucketName, fileId);
		}
	}

}
