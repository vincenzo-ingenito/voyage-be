//package it.voyage.ms.controller;
//
//import org.springframework.core.io.ByteArrayResource;
//import org.springframework.core.io.Resource;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import it.voyage.ms.service.IFirebaseStorageService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//
///**
// * Controller per gestire il download di file criptati
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/files")
//@RequiredArgsConstructor
//public class FileController {
//
//    private final IFirebaseStorageService storageService;
//
//   
//    @GetMapping("/download")
//    public ResponseEntity<Resource> downloadFile(@RequestParam("fileId") String fileId, Authentication authentication) {
//        
//            String userId = authentication.getName();
//            
//            if (fileId == null || fileId.trim().isEmpty()) {
//                log.error("fileId è vuoto o null!");
//                return ResponseEntity.badRequest().build();
//            }
//
//            // Decodifica URL encoding
//            String decodedFileId = java.net.URLDecoder.decode(fileId, java.nio.charset.StandardCharsets.UTF_8);
//            log.info("Path file decodificato: {}", decodedFileId);
//            
//            // Scarica e decripta il file
//            byte[] fileData = storageService.downloadAndDecryptFile(decodedFileId, userId);
//            
//            // Recupera i metadati originali del file
//            com.google.cloud.storage.Blob blob = storageService.getBlob(decodedFileId);
//            
//            if (blob == null || blob.getMetadata() == null) {
//                log.error("Blob o metadata non trovati per: {}", decodedFileId);
//                return ResponseEntity.notFound().build();
//            }
//            
//            String originalFileName = blob.getMetadata().getOrDefault("original-filename", "download");
//            String contentType = blob.getMetadata().getOrDefault("content-type", "application/octet-stream");
//            
//            log.info("File scaricato e decriptato con successo: {} ({} bytes)", originalFileName, fileData.length);
//            
//            // Crea la risorsa per il download
//            ByteArrayResource resource = new ByteArrayResource(fileData);
//            
//            return ResponseEntity.ok()
//                    .contentType(MediaType.parseMediaType(contentType))
//                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalFileName + "\"")
//                    .contentLength(fileData.length)
//                    .body(resource);
//    }
//}