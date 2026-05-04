package it.voyage.ms.service.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.AttachmentUrlDTO;
import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.FeedPageDTO;
import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.RegionVisit;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.dto.response.VoteStatsDTO;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.mapper.TravelMapper;
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelFileEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.BookmarkRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IGroupTravelService;
import it.voyage.ms.service.ITravelService;
import it.voyage.ms.service.ITravelVoteService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Servizio per la gestione dei viaggi.
 *
 * Responsabilità:
 * - CRUD dei viaggi
 * - Coordinamento upload file su Firebase Storage
 * - Risoluzione URL pubblici per allegati e immagini
 * - Aggregazione visite per paese/regione
 */
@Service
@Slf4j
@AllArgsConstructor
public class TravelService implements ITravelService {

    private final TravelRepository travelRepository;
    private final FirebaseStorageService storageService;
    private final TravelMapper travelMapper;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final IGroupTravelService groupTravelService;
    private final ITravelVoteService travelVoteService;


    @Override
    @Transactional
    public TravelDTO saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
        try {
            TravelEty travel = travelMapper.convertDtoToEty(travelData);

            if (Boolean.TRUE.equals(travelData.getIsCopied())) {
                log.info("Viaggio copiato rilevato, rimuovo tutti gli ID");
                clearAllIds(travel);
            }

            UserEty user = userRepository.findById(userDetails.getUserId()).orElseThrow(() -> new BusinessException("Utente non trovato"));
            travel.setUser(user);

            TravelEty savedTravel = travelRepository.save(travel);

            FileUploadResult uploadResult = (files != null && !files.isEmpty()) ? processAndUploadAttachmentsWithMetadata(savedTravel, files) : new FileUploadResult(Collections.emptyList(), Collections.emptyList());

            savedTravel.getFiles().clear();
            savedTravel.getFiles().addAll(toFileEntities(uploadResult.metadata, savedTravel));
            savedTravel = travelRepository.save(savedTravel);
            log.info("Viaggio salvato. ID: {}, Files: {}", savedTravel.getId(), savedTravel.getFiles().size());

            return buildTravelDtoWithUrls(savedTravel);

        } catch (IOException e) {
            log.error("Errore durante il caricamento dei file: {}", e.getMessage(), e);
            throw new BusinessException("Errore durante il caricamento dei file.", e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante il salvataggio del viaggio: {}", e.getMessage(), e);
            throw new BusinessException("Errore durante il salvataggio del viaggio.", e);
        }
    }

    @Override
    @Transactional
    public Boolean deleteTravelById(Long travelId, String userId) {
        Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserIdWithFiles(travelId, userId);
        if (travelOpt.isEmpty()) {
            log.warn("Tentativo di eliminare viaggio non esistente o non autorizzato: travelId={}, userId={}", travelId, userId);
            return false;
        }

        int deletedBookmarksCount = bookmarkRepository.deleteByTravelId(travelId);
        if (deletedBookmarksCount > 0) {
            log.info("Eliminati {} bookmark associati al viaggio {}", deletedBookmarksCount, travelId);
        }

        travelRepository.deleteById(travelId);
        log.info("Viaggio {} eliminato dal database", travelId);

        // Storage: best-effort, non blocca in caso di errore
        try {
            int deletedFilesCount = storageService.deleteTravelFolder(userId, travelId);
            log.info("Eliminati {} file dalla cartella del viaggio {} su Firebase Storage", deletedFilesCount, travelId);
        } catch (Exception e) {
            log.error("Viaggio {} eliminato dal DB ma errore su Storage: {}", travelId, e.getMessage());
        }
        return true;
    }


    @Override
    @Transactional
    public TravelDTO updateExistingTravel(String ownerUid, Long travelId, TravelDTO newTravelData, List<MultipartFile> files) {
        try {
            log.info("UPDATE TRAVEL - Inizio aggiornamento viaggio ID: {}", travelId);

            // Cerca il viaggio senza filtro owner
            TravelEty existingTravel = travelRepository.findById(travelId).orElseThrow(() -> new BusinessException("Viaggio non trovato."));

            // Verifica se l'utente può modificare questo viaggio (owner o editor)
            if (!groupTravelService.canUserEditTravel(travelId, ownerUid)) {
                log.warn("Utente {} non autorizzato a modificare il viaggio {}", ownerUid, travelId);
                throw new BusinessException("Non hai i permessi per modificare questo viaggio.");
            }

            log.info("Utente {} autorizzato a modificare il viaggio {}", ownerUid, travelId);

            List<TravelFileEty> existingFiles = new ArrayList<>(existingTravel.getFiles());
            int existingFilesCount = existingFiles.size();

            // Aggiorna i campi base
            existingTravel.setTravelName(newTravelData.getTravelName());
            existingTravel.setDateFrom(newTravelData.getDateFrom());
            existingTravel.setDateTo(newTravelData.getDateTo());

            // Ricrea l'itinerario
            existingTravel.getItinerary().clear();
            TravelEty updatedTravelFromDto = travelMapper.convertDtoToEty(newTravelData);
            if (updatedTravelFromDto.getItinerary() != null) {
                for (DailyItineraryEty newDay : updatedTravelFromDto.getItinerary()) {
                    newDay.setTravel(existingTravel);
                    log.debug("AGGIUNTA DAY {} - memoryImageIndex: {}", newDay.getDay(), newDay.getMemoryImageIndex());
                    existingTravel.getItinerary().add(newDay);
                }
            }

            // Upload nuovi file o validazione indici esistenti
            if (files != null && !files.isEmpty()) {
                log.info("Caricamento di {} nuovi file per il viaggio {}", files.size(), travelId);
                FileUploadResult uploadResult = processAndUploadAttachmentsForUpdate(existingTravel, files, existingFilesCount);
                existingFiles.addAll(toFileEntities(uploadResult.metadata, existingTravel));
            } else {
                log.info("Nessun nuovo file, valido solo gli indici esistenti per il viaggio {}", travelId);
                validateExistingFileIndices(existingTravel, existingFilesCount);
            }

            existingTravel.getFiles().clear();
            existingTravel.getFiles().addAll(existingFiles);

            TravelEty savedTravel = travelRepository.save(existingTravel);
            log.info("Viaggio aggiornato. ID: {}, Files: {}", savedTravel.getId(), savedTravel.getFiles().size());

            return buildTravelDtoWithUrls(savedTravel);

        } catch (IOException e) {
            log.error("Errore durante il caricamento dei file per il viaggio {}: {}", travelId, e.getMessage(), e);
            throw new BusinessException("Errore durante il caricamento dei file.", e);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Errore durante l'aggiornamento del viaggio {}: {}", travelId, e.getMessage(), e);
            throw new BusinessException("Errore durante l'aggiornamento del viaggio.", e);
        }
    }

    // =========================================================================
    // GET LIST
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<TravelDTO> getTravelsForUser(String userId) {
    	List<TravelDTO> out = new ArrayList<>();
    	List<TravelEty> travels = travelRepository.findByUserId(userId);
    	for(TravelEty travel : travels) {
    		TravelDTO dto = toTravelDtoWithMetadataOnly(travel);
    		out.add(dto);
    	}
    	// FIX N+1: Arricchisci tutti i viaggi con voteStats in batch
    	return enrichListWithVoteStats(out, userId);
    }
    
    /**
     * Arricchisce una lista di TravelDTO con i voteStats in batch
     * OTTIMIZZATO: 2 query totali invece di 2*N query
     */
    private List<TravelDTO> enrichListWithVoteStats(List<TravelDTO> dtos, String userId) {
        if (dtos == null || dtos.isEmpty()) {
            return dtos;
        }
        
        List<Long> travelIds = dtos.stream()
            .map(TravelDTO::getTravelId)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        
        if (travelIds.isEmpty()) {
            return dtos;
        }
        
        // Ottieni voteStats in batch usando il servizio
        Map<Long, VoteStatsDTO> voteStatsMap = travelVoteService.getVoteStatsBatch(travelIds, userId);
        
        // Applica i voteStats a ciascun DTO
        dtos.forEach(dto -> {
            if (dto.getTravelId() != null) {
                VoteStatsDTO stats = voteStatsMap.get(dto.getTravelId());
                if (stats != null) {
                    dto.setVoteStats(stats);
                }
            }
        });
        
        return dtos;
    }

    /**
     * Arricchisce un TravelDTO con i voteStats (per chiamate singole)
     */
    private TravelDTO enrichWithVoteStats(TravelDTO dto, String currentUserId) {
        if (dto.getTravelId() != null) {
            VoteStatsDTO voteStats = travelVoteService.getVoteStats(dto.getTravelId(), currentUserId);
            log.info("[TravelService] voteStats ricevuti: likes={}, userVote={}", voteStats.getLikes(), voteStats.getUserVote());
            dto.setVoteStats(voteStats);
        }
        return dto;
    }

    /**
     * Converte un'entity in DTO popolando gli allegati con i metadati (senza URL firmati).
     * Usato per le liste dove gli URL completi non sono necessari.
     */
    private TravelDTO toTravelDtoWithMetadataOnly(TravelEty entity) {
        TravelDTO dto = travelMapper.convertEtyToDTO(entity);
        List<FileMetadata> fileMetadataList = entity.getFileMetadataList();

        if (dto.getItinerary() == null || fileMetadataList == null) return dto;

        // [FIX] 4 livelli di for annidati estratti in metodo dedicato
        dto.getItinerary().forEach(dayDto ->
            populateDayAttachmentsWithMetadataOnly(dayDto, fileMetadataList, dto.getTravelId())
        );
        return dto;
    }

    private void populateDayAttachmentsWithMetadataOnly(DailyItineraryDTO dayDto,
            List<FileMetadata> fileMetadataList, Long travelId) {
        if (dayDto.getPoints() == null) return;

        dayDto.getPoints().stream()
            .filter(p -> p.getAttachmentIndices() != null)
            .forEach(pointDto -> {
                List<AttachmentUrlDTO> attachmentMetadata = pointDto.getAttachmentIndices().stream()
                    .filter(index -> index < fileMetadataList.size())
                    .map(index -> {
                        FileMetadata m = fileMetadataList.get(index);
                        // URL null: verrà risolto on-demand in getTravelWithUrls
                        return new AttachmentUrlDTO(null, m.getFileName(), m.getMimeType(), m.getFileId());
                    })
                    .collect(Collectors.toList());
                pointDto.setAttachmentUrls(attachmentMetadata);
            });
    }

    // =========================================================================
    // GET SINGLE WITH URLS
    // =========================================================================

    /**
     * Recupera un viaggio con gli URL delle foto.
     * 
     * @param currentUserId L'userId dell'utente corrente (per i voteStats e per verificare i permessi se targetUserId è null)
     * @param travelId L'ID del viaggio da recuperare
     * @param targetUserId L'userId del proprietario del viaggio (opzionale, usato per viaggi di amici)
     * @return Il viaggio con URL e voteStats dell'utente corrente
     */
    @Transactional(readOnly = true)
    public TravelDTO getTravelWithUrls(String currentUserId, Long travelId, String targetUserId) {
        log.info("GET TRAVEL WITH URLS - travelId: {}, currentUserId: '{}', targetUserId: '{}'", 
                 travelId, currentUserId, targetUserId);

        // Se targetUserId è null, significa che è un viaggio proprio
        String ownerUserId = (targetUserId != null && !targetUserId.isEmpty()) ? targetUserId : currentUserId;
        
        // FIX: Prima cerca se è il proprietario
        Optional<TravelEty> ownerTravel = travelRepository.findByIdAndUserId(travelId, ownerUserId);
        if (ownerTravel.isPresent()) {
            log.info("Viaggio {} trovato per proprietario: {}", travelId, ownerUserId);
            // FIX CRITICO: Usa SEMPRE currentUserId per i voteStats, non ownerUserId!
            return enrichWithVoteStats(buildTravelDtoWithUrls(ownerTravel.get()), currentUserId);
        }

        // FIX: Se non è il proprietario, verifica se è un partecipante ACCEPTED
        TravelEty entity = travelRepository.findById(travelId)
            .orElseThrow(() -> new BusinessException("Viaggio non trovato."));
        
        // Verifica se l'utente corrente è un partecipante accettato
        boolean isAcceptedParticipant = entity.getParticipants().stream()
            .anyMatch(p -> p.getUserId().equals(currentUserId) && 
                          p.getStatus() == it.voyage.ms.repository.entity.ParticipantStatus.ACCEPTED);
        
        if (!isAcceptedParticipant) {
            log.warn("Utente {} non autorizzato ad accedere al viaggio {}", currentUserId, travelId);
            throw new BusinessException("Viaggio non trovato o non autorizzato.");
        }
        
        log.info("Utente {} è un partecipante ACCEPTED del viaggio {}", currentUserId, travelId);
        // FIX CRITICO: Usa SEMPRE currentUserId per i voteStats!
        return enrichWithVoteStats(buildTravelDtoWithUrls(entity), currentUserId);
    }

    /**
     * [FIX] Metodo unificato che costruisce il DTO con URL firmati.
     * Sostituisce la duplicazione tra getTravelWithUrls e populateAllFileUrls.
     */
    private TravelDTO buildTravelDtoWithUrls(TravelEty entity) {
        TravelDTO dto = travelMapper.convertEtyToDTO(entity);
        List<String> allFileIds = entity.getAllFileIds();
        List<FileMetadata> fileMetadataList = entity.getFileMetadataList();

        if (allFileIds == null || allFileIds.isEmpty() || dto.getItinerary() == null) {
            return dto;
        }

        dto.getItinerary().forEach(dayDto -> populateDayUrls(dayDto, allFileIds, fileMetadataList, dto.getTravelId()));
        return dto;
    }

    private void populateDayUrls(DailyItineraryDTO dayDto, List<String> allFileIds,
            List<FileMetadata> fileMetadataList, Long travelId) {
        // Immagine ricordo del giorno
        if (dayDto.getMemoryImageIndex() != null) {
            safeGetFileId(allFileIds, dayDto.getMemoryImageIndex(), travelId, "memoria giorno")
                .map(storageService::getPublicUrl)
                .ifPresent(dayDto::setMemoryImageUrl);
        }

        // Allegati dei punti
        if (dayDto.getPoints() != null) {
            dayDto.getPoints().stream()
                .filter(p -> p.getAttachmentIndices() != null)
                .forEach(pointDto -> {
                    List<AttachmentUrlDTO> urls = pointDto.getAttachmentIndices().stream()
                        .map(index -> resolveAttachmentUrl(index, allFileIds, fileMetadataList, travelId))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                    pointDto.setAttachmentUrls(urls);
                });
        }
    }

    /**
     * try/catch IndexOutOfBoundsException ripetuto ~8 volte nel codice originale.
     */
    private Optional<String> safeGetFileId(List<String> allFileIds, int index, Long travelId, String context) {
        if (index < 0 || index >= allFileIds.size()) {
            log.warn("Indice {} fuori limite per {} - Travel ID: {}", index, context, travelId);
            return Optional.empty();
        }
        return Optional.ofNullable(allFileIds.get(index));
    }

    private AttachmentUrlDTO resolveAttachmentUrl(Integer index, List<String> allFileIds,
            List<FileMetadata> fileMetadataList, Long travelId) {
        return safeGetFileId(allFileIds, index, travelId, "allegato").map(fileId -> {
            String fileName = "file";
            String mimeType = "application/octet-stream";
            if (fileMetadataList != null && index < fileMetadataList.size()) {
                FileMetadata m = fileMetadataList.get(index);
                if (m != null) {
                    fileName = m.getFileName();
                    mimeType = m.getMimeType();
                }
            }
            return new AttachmentUrlDTO(storageService.getPublicUrl(fileId), fileName, mimeType, fileId);
        }).orElse(null);
    }

    

    // =========================================================================
    // CONFIRM DATES
    // =========================================================================

    @Override
    @Transactional
    public TravelDTO confirmTravelDates(String userId, String travelId) {
        Long travelIdLong = Long.parseLong(travelId);

        TravelEty travel = travelRepository.findByIdAndUserId(travelIdLong, userId)
                .orElseThrow(() -> new BusinessException("Viaggio non trovato o non autorizzato."));

        travel.setIsCopied(false);
        travel.setNeedsDateConfirmation(false);

        return travelMapper.convertEtyToDTO(travelRepository.save(travel));
    }

    // =========================================================================
    // COUNTRY VISITS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CountryVisit> getConsolidatedCountryVisits(String userId) {
        // FIX N+1: Usa findByUserIdWithPoints che fa eager fetch di itinerary e points
        List<TravelEty> travels = travelRepository.findByUserIdWithPoints(userId);
        return consolidateCountryVisits(travels.stream()
                .map(this::mapTravelToCountryVisit)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    // =========================================================================
    // FILE UPLOAD — SAVE
    // =========================================================================

    private FileUploadResult processAndUploadAttachmentsWithMetadata(TravelEty travelEty, List<MultipartFile> files)
            throws IOException {
        if (travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
            return new FileUploadResult(Collections.emptyList(), Collections.emptyList());
        }

        List<String> allFileIds = new ArrayList<>();
        List<FileMetadata> allFileMetadata = new ArrayList<>();
        FileSyncState state = FileSyncState.forSave(travelEty.getUser().getId(), travelEty.getId(),
                allFileIds, allFileMetadata);

        for (DailyItineraryEty dailyEty : travelEty.getItinerary()) {
            processDayMemoryImage(dailyEty, files, state);
            processPointAttachments(dailyEty, files, state);
        }

        return new FileUploadResult(allFileIds, allFileMetadata);
    }

    private void processDayMemoryImage(DailyItineraryEty dayEty, List<MultipartFile> files,
            FileSyncState state) throws IOException {
        Integer tempFileIndex = dayEty.getMemoryImageIndex();
        if (tempFileIndex == null || tempFileIndex < 0 || tempFileIndex >= files.size()) return;

        FileMetadata metadata = storageService.uploadFileWithMetadata(
                files.get(tempFileIndex), state.userId, state.travelId, "day-memory");
        dayEty.setMemoryImageIndex(state.nextIndex());
        state.allFileIds.add(metadata.getFileId());
        state.allFileMetadata.add(metadata);
    }

    private void processPointAttachments(DailyItineraryEty dayEty, List<MultipartFile> files,
            FileSyncState state) throws IOException {
        if (dayEty.getPoints() == null) return;

        for (PointEty pointEty : dayEty.getPoints()) {
            List<Integer> tempIndices = pointEty.getAttachmentIndices();
            if (tempIndices == null || tempIndices.isEmpty()) continue;

            List<Integer> finalIndices = new ArrayList<>();
            for (Integer tempFileIndex : tempIndices) {
                if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
                    FileMetadata metadata = storageService.uploadFileWithMetadata(
                            files.get(tempFileIndex), state.userId, state.travelId, "point-attachment");
                    finalIndices.add(state.nextIndex());
                    state.allFileIds.add(metadata.getFileId());
                    state.allFileMetadata.add(metadata);
                }
            }
            pointEty.setAttachmentIndices(finalIndices);
        }
    }

    // =========================================================================
    // FILE UPLOAD — UPDATE
    // =========================================================================

    private FileUploadResult processAndUploadAttachmentsForUpdate(TravelEty travelEty, List<MultipartFile> files,
            int existingFilesCount) throws IOException {
        if (travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
            return new FileUploadResult(Collections.emptyList(), Collections.emptyList());
        }

        List<String> newFileIds = new ArrayList<>();
        List<FileMetadata> newFileMetadata = new ArrayList<>();
        FileSyncState state = FileSyncState.forUpdate(travelEty.getUser().getId(), travelEty.getId(),
                newFileIds, newFileMetadata, existingFilesCount);

        for (DailyItineraryEty dailyEty : travelEty.getItinerary()) {
            processDayMemoryImageForUpdate(dailyEty, files, state);
            processPointAttachmentsForUpdate(dailyEty, files, state);
        }

        return new FileUploadResult(newFileIds, newFileMetadata);
    }

    private void processDayMemoryImageForUpdate(DailyItineraryEty dayEty, List<MultipartFile> files,
            FileSyncState state) throws IOException {
        Integer memoryImageIndex = dayEty.getMemoryImageIndex();
        if (memoryImageIndex == null) return;

        // Indice già esistente: non toccare
        if (memoryImageIndex < state.existingFilesCount) {
            log.debug("Preservo foto ricordo esistente, indice: {}", memoryImageIndex);
            return;
        }

        int fileArrayIndex = memoryImageIndex - state.existingFilesCount;
        if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
            FileMetadata metadata = storageService.uploadFileWithMetadata(
                    files.get(fileArrayIndex), state.userId, state.travelId, "day-memory");
            dayEty.setMemoryImageIndex(state.nextIndex());
            state.allFileIds.add(metadata.getFileId());
            state.allFileMetadata.add(metadata);
        } else {
            log.warn("Indice file non valido per foto ricordo: memoryImageIndex={}, files.size()={}", memoryImageIndex, files.size());
        }
    }

    private void processPointAttachmentsForUpdate(DailyItineraryEty dayEty, List<MultipartFile> files,
            FileSyncState state) throws IOException {
        if (dayEty.getPoints() == null) return;

        for (PointEty pointEty : dayEty.getPoints()) {
            List<Integer> attachmentIndices = pointEty.getAttachmentIndices();
            if (attachmentIndices == null || attachmentIndices.isEmpty()) continue;

            List<Integer> updatedIndices = new ArrayList<>();
            for (Integer index : attachmentIndices) {
                if (index < state.existingFilesCount) {
                    updatedIndices.add(index);
                    log.debug("Preservo allegato esistente, indice: {}", index);
                } else {
                    int fileArrayIndex = index - state.existingFilesCount;
                    if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
                        FileMetadata metadata = storageService.uploadFileWithMetadata(
                                files.get(fileArrayIndex), state.userId, state.travelId, "point-attachment");
                        updatedIndices.add(state.nextIndex());
                        state.allFileIds.add(metadata.getFileId());
                        state.allFileMetadata.add(metadata);
                    } else {
                        log.warn("Indice file non valido per allegato: index={}, files.size()={}", index, files.size());
                    }
                }
            }
            pointEty.setAttachmentIndices(updatedIndices);
        }
    }

    // =========================================================================
    // VALIDATION
    // =========================================================================

    private void validateExistingFileIndices(TravelEty travelEty, int existingFilesCount) {
        if (travelEty.getItinerary() == null) return;

        for (DailyItineraryEty dayEty : travelEty.getItinerary()) {
            Integer memoryIndex = dayEty.getMemoryImageIndex();
            if (memoryIndex != null && memoryIndex >= existingFilesCount) {
                log.warn("Foto ricordo con indice {} fuori range, rimuovo riferimento", memoryIndex);
                dayEty.setMemoryImageIndex(null);
            }

            if (dayEty.getPoints() != null) {
                for (PointEty pointEty : dayEty.getPoints()) {
                    List<Integer> indices = pointEty.getAttachmentIndices();
                    if (indices != null && !indices.isEmpty()) {
                        List<Integer> validIndices = indices.stream()
                                .filter(i -> {
                                    boolean valid = i < existingFilesCount;
                                    if (!valid) log.warn("Allegato con indice {} fuori range, rimuovo riferimento", i);
                                    return valid;
                                })
                                .collect(Collectors.toList());
                        pointEty.setAttachmentIndices(validIndices);
                    }
                }
            }
        }
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    /**
     * [FIX] Metodo estratto per la conversione FileMetadata → TravelFileEty,
     * duplicata sia in saveTravel che in updateExistingTravel nel codice originale.
     */
    private List<TravelFileEty> toFileEntities(List<FileMetadata> metadataList, TravelEty travel) {
        return metadataList.stream().map(metadata -> {
            TravelFileEty fileEty = new TravelFileEty();
            fileEty.setFileId(metadata.getFileId());
            fileEty.setFileName(metadata.getFileName());
            fileEty.setMimeType(metadata.getMimeType());
            fileEty.setUploadDate(LocalDateTime.now());
            fileEty.setTravel(travel);
            return fileEty;
        }).collect(Collectors.toList());
    }

    private void clearAllIds(TravelEty travel) {
        travel.setId(null);
        if (travel.getItinerary() != null) {
            for (DailyItineraryEty day : travel.getItinerary()) {
                day.setId(null);
                if (day.getPoints() != null) {
                    day.getPoints().forEach(p -> p.setId(null));
                }
            }
        }
        log.debug("Rimossi tutti gli ID dal viaggio copiato");
    }

    // =========================================================================
    // RESOLVE FILE URLS (usato da mapTravelToCountryVisit)
    // =========================================================================

    /**
     * Risolve gli URL dei file per un itinerario dato.
     * Usato internamente per le country visits; non esposto via interfaccia.
     */
    private List<DailyItineraryDTO> resolveFileUrls(List<DailyItineraryDTO> itinerary,
            List<String> allFileIds, Long travelId) {
        if (allFileIds == null || allFileIds.isEmpty() || itinerary == null) return itinerary;

        itinerary.forEach(dayDto -> {
            if (dayDto.getMemoryImageIndex() != null) {
                safeGetFileId(allFileIds, dayDto.getMemoryImageIndex(), travelId, "memoria giorno")
                    .map(storageService::getPublicUrl)
                    .ifPresent(dayDto::setMemoryImageUrl);
            }
            if (dayDto.getPoints() != null) {
                dayDto.getPoints().forEach(pointDto -> {
                    ensureValidCoordinates(pointDto);
                    if (pointDto.getAttachmentIndices() != null) {
                        List<AttachmentUrlDTO> urls = pointDto.getAttachmentIndices().stream()
                            .map(index -> resolveAttachmentUrl(index, allFileIds, null, travelId))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                        pointDto.setAttachmentUrls(urls);
                    }
                });
            }
        });
        return itinerary;
    }

    private void ensureValidCoordinates(PointDTO pointDto) {
        if (pointDto.getCoord() == null) {
            pointDto.setCoord(new CoordsDto(null, null));
        }
    }

    // =========================================================================
    // COUNTRY VISIT HELPERS
    // =========================================================================

    private List<CountryVisit> consolidateCountryVisits(List<CountryVisit> countryVisits) {
        Map<String, CountryVisit> consolidatedMap = new HashMap<>();
        for (CountryVisit cv : countryVisits) {
            consolidatedMap.merge(cv.getIso(), cv, (existing, newVisit) -> {
                mergeCountryVisits(existing, newVisit);
                return existing;
            });
        }
        return new ArrayList<>(consolidatedMap.values());
    }

    private void mergeCountryVisits(CountryVisit existing, CountryVisit newVisit) {
        existing.getVisitedDates().addAll(newVisit.getVisitedDates());
        mergeCountryRegions(existing, newVisit);
    }

    private void mergeCountryRegions(CountryVisit existing, CountryVisit newVisit) {
        Map<String, RegionVisit> existingRegionsMap = existing.getRegions().stream()
                .collect(Collectors.toMap(RegionVisit::getName, r -> r, (a, b) -> a));

        for (RegionVisit newRegion : newVisit.getRegions()) {
            if (existingRegionsMap.containsKey(newRegion.getName())) {
                mergeRegionItineraries(existingRegionsMap.get(newRegion.getName()), newRegion);
            } else {
                existing.getRegions().add(newRegion);
            }
        }
    }

    private void mergeRegionItineraries(RegionVisit existingRegion, RegionVisit newRegion) {
        if (newRegion.getItinerary() != null) {
            if (existingRegion.getItinerary() == null) existingRegion.setItinerary(new ArrayList<>());
            existingRegion.getItinerary().addAll(newRegion.getItinerary());
        }
    }

    private CountryVisit mapTravelToCountryVisit(TravelEty travelEty) {
        if (travelEty == null || travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) return null;

        TravelDTO travelDTO = travelMapper.convertEtyToDTO(travelEty);
        List<DailyItineraryDTO> resolvedItineraries = resolveFileUrls(
                travelDTO.getItinerary(), travelEty.getAllFileIds(), travelEty.getId());

        List<PointDTO> allPoints = resolvedItineraries.stream()
                .flatMap(di -> di.getPoints().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return allPoints.isEmpty() ? null
                : buildCountryVisit(resolvedItineraries, allPoints, travelEty.getId(), travelEty.getTravelName());
    }

    private CountryVisit buildCountryVisit(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints,
            Long travelId, String travelName) {
        PointDTO firstPoint = allPoints.get(0);

        CountryVisit cv = new CountryVisit();
        String countryName = Optional.ofNullable(firstPoint.getCountry()).orElse("Nazione Sconosciuta");
        cv.setIso(countryName.replaceAll("\\s", "_").toUpperCase());
        cv.setName(countryName);

        Set<String> visitedDates = itineraries.stream()
                .map(DailyItineraryDTO::getDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        cv.setVisitedDates(visitedDates);
        cv.setCoord(firstPoint.getCoord());
        cv.setRegions(buildRegionVisits(itineraries, allPoints, travelId, travelName));
        return cv;
    }

    private List<RegionVisit> buildRegionVisits(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints,
            Long travelId, String travelName) {
        return allPoints.stream()
                .filter(p -> p.getRegion() != null)
                .collect(Collectors.groupingBy(PointDTO::getRegion))
                .entrySet().stream()
                .map(e -> buildRegionVisit(e.getKey(), e.getValue(), itineraries, travelId, travelName))
                .collect(Collectors.toList());
    }

    private RegionVisit buildRegionVisit(String regionName, List<PointDTO> regionPoints,
            List<DailyItineraryDTO> allItineraries, Long travelId, String travelName) {
        RegionVisit rv = new RegionVisit();
        rv.setId(UUID.randomUUID().toString());
        rv.setName(regionName);
        rv.setCoord(regionPoints.stream().map(PointDTO::getCoord).filter(Objects::nonNull).findFirst().orElse(null));
        rv.setItinerary(allItineraries.stream()
                .map(di -> filterItineraryByRegion(di, regionName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        rv.setTravelId(String.valueOf(travelId));
        rv.setTravelName(travelName);
        return rv;
    }

    private DailyItineraryDTO filterItineraryByRegion(DailyItineraryDTO dayItinerary, String regionName) {
        List<PointDTO> filtered = dayItinerary.getPoints().stream()
                .filter(p -> regionName.equals(p.getRegion()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) return null;

        DailyItineraryDTO newDi = new DailyItineraryDTO();
        newDi.setDay(dayItinerary.getDay());
        newDi.setDate(dayItinerary.getDate());
        newDi.setPoints(filtered);
        newDi.setMemoryImageIndex(dayItinerary.getMemoryImageIndex());
        newDi.setMemoryImageUrl(dayItinerary.getMemoryImageUrl());
        return newDi;
    }

    // =========================================================================
    // FEED PAGINATO - OTTIMIZZATO
    // =========================================================================
    
    // FIX OOM: Limiti di sicurezza per prevenire OutOfMemory
    private static final int MAX_PAGE_SIZE = 20;
    private static final int DEFAULT_PAGE_SIZE = 15;
    private static final double MEMORY_WARNING_THRESHOLD = 0.75; // 75% heap
    
    @Override
    @Transactional(readOnly = true, timeout = 30) // FIX OOM: Timeout 30s per evitare query bloccate
    public FeedPageDTO getFeedPaginated(String userId, int pageSize, String cursor, boolean includePhotos) {
        long startTime = System.currentTimeMillis();
        
        // FIX OOM: Validazione e sanitizzazione pageSize
        int originalPageSize = pageSize;
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
            log.warn("[FEED] PageSize invalido ({}), uso default: {}", originalPageSize, DEFAULT_PAGE_SIZE);
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
            log.warn("[FEED] PageSize troppo grande ({}), limitato a: {}", originalPageSize, MAX_PAGE_SIZE);
        }
        
        // FIX OOM: Monitoring memoria prima di procedere
        checkMemoryStatus();
 
        // 1. Calcola il numero di pagina dal cursor
        int pageNumber = 0;
        if (cursor != null && !cursor.isEmpty()) {
            try {
                pageNumber = Integer.parseInt(cursor);
            } catch (NumberFormatException e) {
                log.warn("[FEED] Cursor non numerico ignorato: {}", cursor);
                pageNumber = 0;
            }
        }

        // 2. OTTIMIZZAZIONE: Query CTE - Elimina chiamata a friendshipService e IN condition
        //    Una sola query con JOIN diretta alla tabella friend_relationships
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        Page<TravelEty> page = travelRepository.findFeedPageOptimized(userId, pageable);
        
        log.info("[FEED DEBUG] Query CTE completata: pagina {} di {}, {} viaggi totali", 
                pageNumber, page.getTotalPages(), page.getTotalElements());
         

        // 3. OTTIMIZZAZIONE: Eager loading dei dettagli (itinerary, points, participants, files)
        //    Evita N+1 lazy loading queries durante la conversione DTO
        //    Diviso in 4 query per evitare MultipleBagFetchException
        List<Long> travelIds = page.getContent().stream()
                .map(TravelEty::getId)
                .collect(Collectors.toList());
        
        if (!travelIds.isEmpty()) {
            // STEP 1: Carica itinerari (senza punti)
            List<TravelEty> travelsWithDetails = travelRepository.fetchTravelDetailsForFeed(travelIds);
            
            // STEP 2: Carica punti degli itinerari separatamente
            travelRepository.fetchItineraryPointsForFeed(travelIds);
            
            // STEP 3: Carica participants separatamente per evitare MultipleBagFetchException
            travelRepository.fetchTravelParticipantsForFeed(travelIds);
            
            // STEP 4: Carica files separatamente per evitare MultipleBagFetchException
            travelRepository.fetchTravelFilesForFeed(travelIds);
            
            // Crea mappa per lookup veloce
            Map<Long, TravelEty> detailsMap = travelsWithDetails.stream()
                    .collect(Collectors.toMap(TravelEty::getId, t -> t));
            
            // Sostituisci le entity nella pagina con quelle complete
            List<TravelEty> enrichedContent = page.getContent().stream()
                    .map(t -> detailsMap.getOrDefault(t.getId(), t))
                    .collect(Collectors.toList());
            
            log.info("Eager loading completato: {} viaggi con dettagli caricati (4 query per evitare MultipleBagFetchException)", enrichedContent.size());
            
            // 4. Converti in DTO
            List<TravelDTO> travelDTOs = new ArrayList<>();
            for (TravelEty travel : enrichedContent) {
                TravelDTO dto;
                if (includePhotos) {
                    dto = buildTravelDtoWithUrls(travel);
                } else {
                    dto = toTravelDtoWithMetadataOnly(travel);
                }
                travelDTOs.add(dto);
            }
            
            // 5. OTTIMIZZAZIONE: Batch loading dei VoteStats
            //    Una sola chiamata con 2 query invece di 2*N query
            travelDTOs = enrichListWithVoteStats(travelDTOs, userId);
            
            // 6. Costruisci il cursor per la prossima pagina
            boolean hasMore = page.hasNext();
            String nextCursor = hasMore ? String.valueOf(pageNumber + 1) : null;
            
            // FIX OOM: Log performance e memoria
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            log.info("[FEED] Completato in {}ms: {} viaggi, pageSize richiesto={}, effettivo={}, includePhotos={}", 
                    executionTime, travelDTOs.size(), originalPageSize, pageSize, includePhotos);
            
            // FIX OOM: Warning se esecuzione lenta (possibile problema memoria/DB)
            if (executionTime > 2000) {
                log.warn("[FEED] PERFORMANCE DEGRADATA: {}ms per {} viaggi - verificare memoria e DB", 
                        executionTime, travelDTOs.size());
            }

            return FeedPageDTO.builder()
                    .travels(travelDTOs)
                    .nextCursor(nextCursor)
                    .hasMore(hasMore)
                    .pageSize(travelDTOs.size())
                    .totalCount((int) page.getTotalElements())
                    .build();
        } else {
            // Nessun viaggio trovato
            long endTime = System.currentTimeMillis();
            log.info("[FEED] Completato in {}ms: nessun viaggio trovato per utente {}", 
                    endTime - startTime, userId);
            return FeedPageDTO.builder()
                    .travels(Collections.emptyList())
                    .nextCursor(null)
                    .hasMore(false)
                    .pageSize(0)
                    .totalCount(0)
                    .build();
        }
    }
    
    /**
     * FIX OOM: Monitoring dello stato della memoria heap
     * Logga warning se la memoria utilizzata supera la soglia critica
     */
    private void checkMemoryStatus() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        double usedPercentage = (double) usedMemory / maxMemory;
        
        if (usedPercentage > MEMORY_WARNING_THRESHOLD) {
            log.warn("[FEED] MEMORIA CRITICA: {}/{} MB utilizzati ({:.1f}%) - Soglia: {:.0f}%",
                    usedMemory / 1024 / 1024,
                    maxMemory / 1024 / 1024,
                    usedPercentage * 100,
                    MEMORY_WARNING_THRESHOLD * 100);
            
            // FIX OOM: Suggerisci garbage collection se memoria critica
            if (usedPercentage > 0.85) {
                log.error("[FEED] MEMORIA MOLTO CRITICA (>85%) - Suggerisco GC");
                System.gc(); // Hint al GC, non garantito
            }
        } else {
            log.debug("[FEED] Memoria OK: {}/{} MB ({:.1f}%)",
                    usedMemory / 1024 / 1024,
                    maxMemory / 1024 / 1024,
                    usedPercentage * 100);
        }
    }
     
    // =========================================================================
    // INNER CLASSES
    // =========================================================================

    private static class FileUploadResult {
        final List<String> fileIds;
        final List<FileMetadata> metadata;

        FileUploadResult(List<String> fileIds, List<FileMetadata> metadata) {
            this.fileIds = fileIds;
            this.metadata = metadata;
        }
    }

    /**
     * [FIX] FileSyncStateWithMetadata e FileSyncStateForUpdate erano quasi identici.
     * Unificati in un'unica classe con factory methods statici per i due casi d'uso.
     */
    private static class FileSyncState {
        final String userId;
        final Long travelId;
        final List<String> allFileIds;
        final List<FileMetadata> allFileMetadata;
        final int existingFilesCount;
        private int fileCounter;

        private FileSyncState(String userId, Long travelId, List<String> allFileIds,
                List<FileMetadata> allFileMetadata, int existingFilesCount) {
            this.userId = userId;
            this.travelId = travelId;
            this.allFileIds = allFileIds;
            this.allFileMetadata = allFileMetadata;
            this.existingFilesCount = existingFilesCount;
            this.fileCounter = 0;
        }

        /** Stato per un nuovo salvataggio: gli indici partono da 0. */
        static FileSyncState forSave(String userId, Long travelId,
                List<String> fileIds, List<FileMetadata> metadata) {
            return new FileSyncState(userId, travelId, fileIds, metadata, 0);
        }

        /** Stato per un aggiornamento: i nuovi indici partono dopo quelli esistenti. */
        static FileSyncState forUpdate(String userId, Long travelId,
                List<String> fileIds, List<FileMetadata> metadata, int existingFilesCount) {
            return new FileSyncState(userId, travelId, fileIds, metadata, existingFilesCount);
        }

        /** Restituisce il prossimo indice globale (esistenti + nuovi). */
        int nextIndex() {
            return existingFilesCount + fileCounter++;
        }
    }
}