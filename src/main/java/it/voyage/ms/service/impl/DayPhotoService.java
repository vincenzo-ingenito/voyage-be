package it.voyage.ms.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelFileEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IDayPhotoService;
import it.voyage.ms.service.IFirebaseStorageService;
import it.voyage.ms.service.IGroupTravelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class DayPhotoService implements IDayPhotoService {

    private final TravelRepository travelRepository;
    private final IFirebaseStorageService firebaseStorageService;
    private final IGroupTravelService groupTravelService;

    @Override
    @Transactional
    public String addOrReplacePhotoToDay(Long travelId, Integer dayNumber, MultipartFile file, String userId) {
        TravelEty travel = findTravelAndCheckPermission(travelId, userId);
        DailyItineraryEty targetDay = findDay(travel, dayNumber);

        // Rimuovi la vecchia foto se presente
        removeExistingPhoto(travel, targetDay, dayNumber);

        // Carica la nuova foto e aggiorna i riferimenti
        FileMetadata metadata = firebaseStorageService.uploadFileWithMetadata(file, userId, travelId, "day-memory");

        TravelFileEty travelFile = buildTravelFile(metadata, travel);
        travel.getFiles().add(travelFile);

        int newIndex = travel.getFiles().size() - 1;
        targetDay.setMemoryImageIndex(newIndex);
        targetDay.setMemoryImageUrl(null);

        travelRepository.save(travel);
        log.info("Foto aggiunta/sostituita per travel {} day {} all'indice {}: {}", travelId, dayNumber, newIndex, metadata.getFileId());

        return metadata.getFileId();
    }

    @Override
    @Transactional
    public void removePhotoFromDay(Long travelId, Integer dayNumber, String userId) {
        TravelEty travel = findTravelAndCheckPermission(travelId, userId);
        DailyItineraryEty targetDay = findDay(travel, dayNumber);

        removeExistingPhoto(travel, targetDay, dayNumber);

        targetDay.setMemoryImageIndex(null);
        targetDay.setMemoryImageUrl(null);

        travelRepository.save(travel);
        log.info("Foto rimossa con successo da travel {} day {}", travelId, dayNumber);
    }

    // --- Metodi privati ---

    private TravelEty findTravelAndCheckPermission(Long travelId, String userId) {
        TravelEty travel = travelRepository.findById(travelId)
                .orElseThrow(() -> new RuntimeException("Travel not found with id: " + travelId));

        if (!groupTravelService.canUserEditTravel(travelId, userId)) {
            throw new RuntimeException("User not authorized to modify this travel");
        }

        return travel;
    }

    private DailyItineraryEty findDay(TravelEty travel, Integer dayNumber) {
        return travel.getItinerary().stream()
                .filter(day -> day.getDay().equals(dayNumber))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Day " + dayNumber + " not found in travel"));
    }

    private void removeExistingPhoto(TravelEty travel, DailyItineraryEty targetDay, Integer dayNumber) {
        Integer oldIndex = targetDay.getMemoryImageIndex();
        if (oldIndex == null || oldIndex >= travel.getFiles().size()) return;

        TravelFileEty oldFile = travel.getFiles().get(oldIndex);
        deleteFromFirebase(oldFile.getFileId(), dayNumber);

        travel.getFiles().remove(oldIndex.intValue());
        updateIndexesAfterRemoval(travel.getItinerary(), oldIndex);

        log.info("Foto rimossa dalla lista per day {}", dayNumber);
    }

    private void deleteFromFirebase(String fileId, Integer dayNumber) {
        try {
            firebaseStorageService.getBlob(fileId).delete();
            log.info("Blob Firebase eliminato: {} per day {}", fileId, dayNumber);
        } catch (Exception e) {
            log.warn("Errore eliminazione blob Firebase {}: {}", fileId, e.getMessage());
        }
    }

    private void updateIndexesAfterRemoval(List<DailyItineraryEty> itinerary, int removedIndex) {
        itinerary.stream()
                .filter(day -> day.getMemoryImageIndex() != null && day.getMemoryImageIndex() > removedIndex)
                .forEach(day -> day.setMemoryImageIndex(day.getMemoryImageIndex() - 1));
    }

    private TravelFileEty buildTravelFile(FileMetadata metadata, TravelEty travel) {
        TravelFileEty travelFile = new TravelFileEty();
        travelFile.setFileId(metadata.getFileId());
        travelFile.setFileName(metadata.getFileName());
        travelFile.setMimeType(metadata.getMimeType());
        travelFile.setUploadDate(LocalDateTime.now());
        travelFile.setTravel(travel);
        return travelFile;
    }
}