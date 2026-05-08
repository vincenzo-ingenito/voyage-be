package it.voyage.ms.controller.impl;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import com.google.cloud.storage.Blob;

import it.voyage.ms.controller.IFileController;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Slf4j
@RestController
@RequiredArgsConstructor
public class FileCtl implements IFileController {

	private final IFirebaseStorageService storageService;

	@Override
	public ResponseEntity<Resource> downloadFile(String fileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
		String firebaseUid = userDetails.getUserId();
		log.info("Download file richiesto: {} per utente Firebase: {}", fileId, firebaseUid);

		if (fileId == null || fileId.trim().isEmpty()) {
			log.error("File ID vuoto o null!");
			return ResponseEntity.badRequest().build();
		}

		// Decodifica URL encoding (se necessario)
		String decodedFileId = java.net.URLDecoder.decode(fileId, java.nio.charset.StandardCharsets.UTF_8);
		log.debug("Path file decodificato: {}", decodedFileId);

		byte[] fileData = storageService.downloadFile(decodedFileId, firebaseUid);

		// Recupera i metadati originali del file
		Blob blob = storageService.getBlob(decodedFileId);

		if (blob == null) {
			log.error("Blob non trovato per: {}", decodedFileId);
			return ResponseEntity.notFound().build();
		}

		// Estrai nome file e content type dai metadata (con fallback per retrocompatibilità)
		String originalFileName = "download";
		String contentType = "application/octet-stream";
		
		if (blob.getMetadata() != null) {
			originalFileName = blob.getMetadata().getOrDefault("original-filename", extractFileNameFromPath(decodedFileId));
			contentType = blob.getMetadata().getOrDefault("content-type", blob.getContentType() != null ? blob.getContentType() : contentType);
		} else if (blob.getContentType() != null) {
			contentType = blob.getContentType();
			originalFileName = extractFileNameFromPath(decodedFileId);
		}

		log.info("File scaricato con successo: {} ({} bytes)", originalFileName, fileData.length);

		// Crea la risorsa per il download
		ByteArrayResource resource = new ByteArrayResource(fileData);

		// Restituisci il file con gli header corretti
		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
				.contentLength(fileData.length)
				.body(resource);
	}

	@Override
	public ResponseEntity<Resource> downloadPublicFile(String fileId, @AuthenticationPrincipal CustomUserDetails userDetails) {
		String firebaseUid = userDetails.getUserId();
		log.info("Download file pubblico richiesto: {} per utente Firebase: {}", fileId, firebaseUid);

		if (fileId == null || fileId.trim().isEmpty()) {
			log.error("File ID vuoto o null!");
			return ResponseEntity.badRequest().build();
		}

		// Decodifica URL encoding (se necessario)
		String decodedFileId = java.net.URLDecoder.decode(fileId, java.nio.charset.StandardCharsets.UTF_8);
		log.debug("Path file pubblico decodificato: {}", decodedFileId);

		// Download diretto senza decrittazione
		Blob blob = storageService.getBlob(decodedFileId);

		if (blob == null || !blob.exists()) {
			log.error("File pubblico non trovato: {}", decodedFileId);
			return ResponseEntity.notFound().build();
		}

		// Scarica il contenuto del file
		byte[] fileData = blob.getContent();

		// Estrai nome file e content type dai metadata
		String originalFileName = "download";
		String contentType = "application/octet-stream";
		
		if (blob.getMetadata() != null) {
			originalFileName = blob.getMetadata().getOrDefault("original-filename", originalFileName);
			contentType = blob.getMetadata().getOrDefault("content-type", contentType);
		}

		log.info("File pubblico scaricato con successo: {} ({} bytes)", originalFileName, fileData.length);

		// Crea la risorsa per il download
		ByteArrayResource resource = new ByteArrayResource(fileData);

		// Restituisci il file con gli header corretti
		return ResponseEntity.ok()
				.contentType(MediaType.parseMediaType(contentType))
				.contentLength(fileData.length)
				.body(resource);
	}
	
	/**
	 * Estrae il nome del file dal path completo
	 */
	private String extractFileNameFromPath(String path) {
		if (path == null || path.isEmpty()) {
			return "download";
		}
		return path.substring(path.lastIndexOf('/') + 1);
	}
}
