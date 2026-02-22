package it.voyage.ms.service.impl;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelFileEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IDayPhotoService;
import it.voyage.ms.service.IFirebaseStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayPhotoService implements IDayPhotoService {

    private final TravelRepository travelRepository;
    private final IFirebaseStorageService firebaseStorageService;

    @Override
    @Transactional
    public String addOrReplacePhotoToDay(Long travelId, Integer dayNumber, MultipartFile file, String userId) throws Exception {
        // 1. Trova il viaggio e verifica proprietà
        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Travel not found with id: " + travelId));
        
        if (!travel.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized to modify this travel");
        }

        // 2. Trova il giorno specifico
        List<DailyItineraryEty> itinerary = travel.getItinerary();
        DailyItineraryEty targetDay = itinerary.stream()
                .filter(day -> day.getDay().equals(dayNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Day " + dayNumber + " not found in travel"));

        // 3. Se esiste già una foto, ELIMINA quella vecchia da Firebase
        Integer oldIndex = targetDay.getMemoryImageIndex();
        if (oldIndex != null && oldIndex < travel.getFiles().size()) {
            try {
                TravelFileEty oldFile = travel.getFiles().get(oldIndex);
                String oldFileId = oldFile.getFileId();
                log.info("Eliminazione foto vecchia da Firebase: {} per day {}", oldFileId, dayNumber);
                
                // Elimina il file fisico da Firebase usando il metodo esistente
                try {
                    firebaseStorageService.getBlob(oldFileId).delete();
                } catch (Exception ex) {
                    log.warn("Errore eliminazione blob: {}", ex.getMessage());
                }
                
                // Rimuovi il file dalla lista
                travel.getFiles().remove(oldIndex.intValue());
                
                // Aggiorna gli indici di tutte le foto successive
                for (DailyItineraryEty day : itinerary) {
                    if (day.getMemoryImageIndex() != null && day.getMemoryImageIndex() > oldIndex) {
                        day.setMemoryImageIndex(day.getMemoryImageIndex() - 1);
                    }
                }
                
                log.info("Foto vecchia eliminata con successo da Firebase");
            } catch (Exception e) {
                log.warn("Errore eliminazione foto vecchia: {}", e.getMessage());
            }
        }

        // 4. Carica la nuova foto su Firebase Storage
        FileMetadata metadata = firebaseStorageService.uploadFileWithMetadata(
                file, 
                userId, 
                travelId, 
                "day-memory"
        );

        // 5. AGGIUNGI IL FILE ALLA LISTA travel_files
        TravelFileEty travelFile = new TravelFileEty();
        travelFile.setFileId(metadata.getFileId());
        travelFile.setFileName(metadata.getFileName());
        travelFile.setMimeType(metadata.getMimeType());
        travelFile.setUploadDate(java.time.LocalDateTime.now());
        travelFile.setTravel(travel);
        
        // Aggiungi alla collezione dei file
        travel.getFiles().add(travelFile);
        
        // 6. Calcola l'indice del nuovo file nella lista
        int newFileIndex = travel.getFiles().size() - 1;
        
        // 7. Aggiorna il giorno con l'indice del nuovo file
        targetDay.setMemoryImageIndex(newFileIndex);
        targetDay.setMemoryImageUrl(null); // Usa solo l'indice, non l'URL diretto

        // 8. Salva il viaggio aggiornato
        travelRepository.save(travel);
        
        log.info("Foto sostituita con successo per travel {} day {} at index {}: {}", 
                travelId, dayNumber, newFileIndex, metadata.getFileId());
        
        return metadata.getFileId();
    }

    @Override
    @Transactional
    public void removePhotoFromDay(Long travelId, Integer dayNumber, String userId) throws Exception {
        // 1. Trova il viaggio e verifica proprietà
        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Travel not found with id: " + travelId));
        
        if (!travel.getUser().getId().equals(userId)) {
            throw new RuntimeException("User not authorized to modify this travel");
        }

        // 2. Trova il giorno specifico
        List<DailyItineraryEty> itinerary = travel.getItinerary();
        DailyItineraryEty targetDay = itinerary.stream()
                .filter(day -> day.getDay().equals(dayNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Day " + dayNumber + " not found in travel"));

        // 3. Elimina il file fisico da Firebase
        Integer photoIndex = targetDay.getMemoryImageIndex();
        if (photoIndex != null && photoIndex < travel.getFiles().size()) {
            try {
                TravelFileEty fileToDelete = travel.getFiles().get(photoIndex);
                String fileId = fileToDelete.getFileId();
                log.info("Eliminazione foto da Firebase: {} per day {}", fileId, dayNumber);
                
                // Elimina il file fisico da Firebase
                try {
                    firebaseStorageService.getBlob(fileId).delete();
                } catch (Exception ex) {
                    log.warn("Errore eliminazione blob: {}", ex.getMessage());
                }
                
                // Rimuovi il file dalla lista
                travel.getFiles().remove(photoIndex.intValue());
                
                // Aggiorna gli indici di tutte le foto successive
                for (DailyItineraryEty day : itinerary) {
                    if (day.getMemoryImageIndex() != null && day.getMemoryImageIndex() > photoIndex) {
                        day.setMemoryImageIndex(day.getMemoryImageIndex() - 1);
                    }
                }
                
                log.info("Foto eliminata con successo da Firebase");
            } catch (Exception e) {
                log.warn("Errore eliminazione foto: {}", e.getMessage());
            }
        }

        // 4. Rimuovi il riferimento dal giorno
        targetDay.setMemoryImageUrl(null);
        targetDay.setMemoryImageIndex(null);

        // 5. Salva il viaggio aggiornato
        travelRepository.save(travel);
        
        log.info("Photo removed successfully from travel {} day {}", travelId, dayNumber);
    }
}