package it.voyage.ms.service.impl;

import java.io.IOException;
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

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.AttachmentUrlDTO;
import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.FileMetadata;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.RegionVisit;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.mapper.TravelMapper;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class TravelService implements ITravelService {

	private final TravelRepository travelRepository;
	private final FirebaseStorageService storageService;
	private final TravelMapper travelMapper;


	@Override
	public TravelDTO updateExistingTravel(String ownerUid, String travelId, TravelDTO newTravelData, List<MultipartFile> files){
		Optional<TravelEty> existingTravelOpt = travelRepository.findByIdAndUserId(travelId, ownerUid);
		if (!existingTravelOpt.isPresent()) {
		    throw new BusinessException("Viaggio non trovato o non autorizzato.");
		}

		TravelEty existingTravel = existingTravelOpt.get();
		
		// ✅ FIX: Preserva l'array dei file esistenti E i metadati PRIMA di aggiornare l'itinerario
		List<String> existingFileIds = existingTravel.getAllFileIds() != null 
		    ? new ArrayList<>(existingTravel.getAllFileIds()) 
		    : new ArrayList<>();
		List<FileMetadata> existingMetadata = existingTravel.getFileMetadataList() != null
		    ? new ArrayList<>(existingTravel.getFileMetadataList())
		    : new ArrayList<>();
		
		existingTravel.setTravelName(newTravelData.getTravelName());
		existingTravel.setDateFrom(newTravelData.getDateFrom());
		existingTravel.setDateTo(newTravelData.getDateTo());
		List<DailyItineraryDTO> updatedItinerary = mapDayDTOListToDayList(newTravelData.getItinerary());
		existingTravel.setItinerary(updatedItinerary);
		
		// Gestisci i nuovi file se presenti
		if (files != null && !files.isEmpty()) {
		    try {
		        // ✅ FIX: Processa gli upload E aggiorna gli indici nell'itinerario E i metadati
		        FileUploadResult result = processAndUploadAttachmentsForUpdateWithMetadata(existingTravel, files, existingFileIds, existingMetadata);
		        existingTravel.setAllFileIds(result.fileIds);
		        existingTravel.setFileMetadataList(result.metadata);
		    } catch (IOException e) {
		        log.error("Errore durante il caricamento dei file per il viaggio {}: {}", travelId, e.getMessage(), e);
		        throw new BusinessException("Errore durante il caricamento dei file.", e);
		    }
		} else {
		    // ✅ FIX: Se non ci sono nuovi file, mantieni i file E metadati esistenti
		    existingTravel.setAllFileIds(existingFileIds);
		    existingTravel.setFileMetadataList(existingMetadata);
		}
		
		TravelEty savedTravel = travelRepository.save(existingTravel);
		TravelDTO resultDTO = travelMapper.convertEtyToDTO(savedTravel);
		
		// ✅ FIX: Popola TUTTI gli URL (foto ricordo E allegati) dopo il salvataggio
		// Questo assicura che il frontend riceva gli URL quando riapre il dialog di modifica
		populateAllFileUrls(resultDTO, savedTravel.getAllFileIds(), savedTravel.getFileMetadataList());
		
		return resultDTO;
	}

	protected List<DailyItineraryDTO> mapDayDTOListToDayList(List<DailyItineraryDTO> itineraryDTOs) {
		if (itineraryDTOs == null) {
			return List.of();
		}

		return itineraryDTOs.stream()
				.map(dayDTO -> {
					// Questa logica è quasi identica a quella che hai in convertToDocument
					DailyItineraryDTO dayEty = new DailyItineraryDTO(); 
					dayEty.setDay(dayDTO.getDay());
					dayEty.setDate(dayDTO.getDate());
					
					// ✅ FIX: Preserva l'indice dell'immagine ricordo durante l'aggiornamento
					dayEty.setMemoryImageIndex(dayDTO.getMemoryImageIndex());

					List<PointDTO> pointEties = dayDTO.getPoints().stream()
							.map(pointDTO -> {
								PointDTO pointEty = new PointDTO();
								pointEty.setName(pointDTO.getName());
								pointEty.setType(pointDTO.getType());
								pointEty.setDescription(pointDTO.getDescription());
								pointEty.setCost(pointDTO.getCost());
								pointEty.setCoord(new CoordsDto(pointDTO.getCoord().getLat(), pointDTO.getCoord().getLng()));
								pointEty.setCountry(pointDTO.getCountry());
								pointEty.setRegion(pointDTO.getRegion());
								pointEty.setCity(pointDTO.getCity());
								
								// ✅ FIX: Preserva gli indici degli allegati durante l'aggiornamento
								pointEty.setAttachmentIndices(pointDTO.getAttachmentIndices());
								
								return pointEty;
							}).collect(Collectors.toList());

					dayEty.setPoints(pointEties);
					return dayEty;
				}).collect(Collectors.toList());
	}

	@Override
	public List<TravelDTO> getTravelsForUser(String userId) {
		List<TravelEty> travelEntities = travelRepository.findByUserId(userId);
		List<TravelDTO> travelDTOs = new ArrayList<>();

		for (TravelEty entity : travelEntities) {
			TravelDTO dto = travelMapper.convertEtyToDTO(entity);
			List<FileMetadata> fileMetadataList = entity.getFileMetadataList();

			// ✅ OTTIMIZZAZIONE: NON generiamo gli URL signed qui per ridurre il payload
			// Gli URL verranno generati on-demand quando l'utente espande il viaggio
			// Popoliamo solo i metadati (nome e tipo) senza l'URL
			if (dto.getItinerary() != null && fileMetadataList != null) {
				for (DailyItineraryDTO dayDto : dto.getItinerary()) {
					if (dayDto.getPoints() != null) {
						for (PointDTO pointDto : dayDto.getPoints()) {
							if (pointDto.getAttachmentIndices() != null) {
								List<AttachmentUrlDTO> attachmentMetadata = new ArrayList<>();
								
								for (Integer index : pointDto.getAttachmentIndices()) {
									try {
										if (index < fileMetadataList.size()) {
											FileMetadata metadata = fileMetadataList.get(index);
											// ✅ URL vuoto - verrà popolato on-demand
											attachmentMetadata.add(new AttachmentUrlDTO(
												null, // URL non generato
												metadata.getFileName(),
												metadata.getMimeType()
											));
										}
									} catch (IndexOutOfBoundsException e) {
										log.error("Indice allegato fuori limite per Travel ID: {}", dto.getTravelId());
									}
								}
								pointDto.setAttachmentUrls(attachmentMetadata);
							}
						}
					}
				}
			}

			travelDTOs.add(dto);
		}

		return travelDTOs;
	}

	/**
	 * ✅ NUOVO: Genera gli URL signed on-demand per un singolo viaggio
	 * Questo metodo viene chiamato solo quando l'utente espande il viaggio
	 * ✅ FIX CRITICO: Preserva anche i memoryImageIndex per permettere modifiche successive
	 */
	public TravelDTO getTravelWithUrls(String userId, String travelId) {
		Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelId, userId);
		
		if (!travelOpt.isPresent()) {
			throw new BusinessException("Viaggio non trovato o non autorizzato.");
		}

		TravelEty entity = travelOpt.get();
		TravelDTO dto = travelMapper.convertEtyToDTO(entity);
		List<String> allFileIds = entity.getAllFileIds();
		List<FileMetadata> fileMetadataList = entity.getFileMetadataList();

		// Se non ci sono file, ritorna subito
		if (allFileIds == null || allFileIds.isEmpty()) {
			return dto;
		}

		// Genera gli URL signed per questo viaggio
		if (dto.getItinerary() != null) {
			for (DailyItineraryDTO dayDto : dto.getItinerary()) {
				// A) URL Immagine Ricordo del Giorno
				// ✅ FIX: Preserva memoryImageIndex (già presente nel DTO dal mapper)
				// e aggiungi anche l'URL
				if (dayDto.getMemoryImageIndex() != null) {
					try {
						int index = dayDto.getMemoryImageIndex();
						String fileId = allFileIds.get(index);
						String dayUrl = storageService.getPublicUrl(fileId);
						dayDto.setMemoryImageUrl(dayUrl);
						// memoryImageIndex è già impostato dal mapper, non serve rifarlo
					} catch (IndexOutOfBoundsException e) {
						log.error("Indice immagine giorno fuori limite per Travel ID: {}", dto.getTravelId());
					}
				}

				// B) URL Allegati Punti
				// ✅ FIX: Preserva attachmentIndices (già presenti nel DTO dal mapper)
				if (dayDto.getPoints() != null) {
					for (PointDTO pointDto : dayDto.getPoints()) {
						if (pointDto.getAttachmentIndices() != null) {
							List<AttachmentUrlDTO> attachmentUrls = new ArrayList<>();

							for (Integer index : pointDto.getAttachmentIndices()) {
								try {
									String fileId = allFileIds.get(index);
									String attachmentUrl = storageService.getPublicUrl(fileId);
									
									String fileName = "file";
									String mimeType = "application/octet-stream";
									
									if (fileMetadataList != null && index < fileMetadataList.size()) {
										FileMetadata metadata = fileMetadataList.get(index);
										if (metadata != null) {
											fileName = metadata.getFileName();
											mimeType = metadata.getMimeType();
										}
									}
									
									attachmentUrls.add(new AttachmentUrlDTO(
										attachmentUrl,
										fileName,
										mimeType
									));
								} catch (IndexOutOfBoundsException e) {
									log.error("Indice allegato fuori limite per Travel ID: {}", dto.getTravelId());
								}
							}
							pointDto.setAttachmentUrls(attachmentUrls);
							// attachmentIndices è già impostato dal mapper, non serve rifarlo
						}
					}
				}
			}
		}

		return dto;
	}


	@Override
	public Boolean deleteTravelById(String travelId, String userId) {
		Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelId, userId);
		
		if (!travelOpt.isPresent()) {
			log.warn("Tentativo di eliminare viaggio non esistente o non autorizzato: travelId={}, userId={}", travelId, userId);
			return false;
		}
		
		TravelEty travel = travelOpt.get();
		List<String> fileIds = travel.getAllFileIds();
		
		// 2. Elimina i file da Firebase Storage (se presenti)
		if (fileIds != null && !fileIds.isEmpty()) {
			log.info("Eliminazione di {} file associati al viaggio {}", fileIds.size(), travelId);
			int deletedFilesCount = storageService.deleteFiles(fileIds);
			log.info("Eliminati {}/{} file da Firebase Storage per il viaggio {}", deletedFilesCount, fileIds.size(), travelId);
		} 
		
		long deletedCount = travelRepository.deleteByIdAndUserId(travelId, userId);
		if (deletedCount > 0) {
			log.info("Viaggio {} eliminato con successo (viaggi eliminati: {})", travelId, deletedCount);
		} 

		return deletedCount > 0;
	}
 
	@Override
	public TravelDTO saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
		TravelEty travel = travelMapper.convertDtoToEty(travelData);  
		travel.setUserId(userDetails.getUserId());
		TravelEty savedTravel;

		try {
			if (StringUtils.isBlank(travel.getId())) {
				savedTravel = travelRepository.save(travel); 
			} else {
				savedTravel = travel; 
			}

			// ✅ FIX: Gestisci i file E i metadati solo se presenti
			List<String> uploadedFileIds;
			List<FileMetadata> fileMetadataList;
			if (files != null && !files.isEmpty()) {
				FileUploadResult result = processAndUploadAttachmentsWithMetadata(savedTravel, files);
				uploadedFileIds = result.fileIds;
				fileMetadataList = result.metadata;
			} else {
				uploadedFileIds = Collections.emptyList();
				fileMetadataList = Collections.emptyList();
			}
			savedTravel.setAllFileIds(uploadedFileIds); 
			savedTravel.setFileMetadataList(fileMetadataList); // ✅ FIX: Salva i metadati

			TravelEty finalSavedTravel = travelRepository.save(savedTravel);
			TravelDTO resultDTO = travelMapper.convertEtyToDTO(finalSavedTravel);
			
			// ✅ FIX: Popola TUTTI gli URL (foto ricordo E allegati) dopo il salvataggio
			// Questo assicura che il frontend riceva gli URL quando riapre il dialog di modifica
			populateAllFileUrls(resultDTO, finalSavedTravel.getAllFileIds(), finalSavedTravel.getFileMetadataList());
			
			return resultDTO;

		} catch (Exception e) {
			log.error("Errore generico durante il salvataggio del viaggio {}: {}", travel.getId(), e.getMessage(), e);
			throw new BusinessException("Errore sconosciuto durante il salvataggio del viaggio.", e);
		}
	}
 
	/**
	 * ✅ FIX: Nuova versione che raccoglie anche i metadati dei file
	 */
	private FileUploadResult processAndUploadAttachmentsWithMetadata(TravelEty travelEty, List<MultipartFile> files) throws IOException {
		if (travelEty.getItinerary() == null) {
			return new FileUploadResult(Collections.emptyList(), Collections.emptyList());
		}

		List<String> allFileIds = new ArrayList<>();
		List<FileMetadata> allFileMetadata = new ArrayList<>();
		FileSyncStateWithMetadata state = new FileSyncStateWithMetadata(
			travelEty.getUserId(), 
			travelEty.getId(), 
			allFileIds, 
			allFileMetadata
		);

		for (DailyItineraryDTO dayDto : travelEty.getItinerary()) {
			processDayMemoryImageWithMetadata(dayDto, files, state);
			processPointAttachmentsWithMetadata(dayDto, files, state);
		}

		return new FileUploadResult(allFileIds, allFileMetadata);
	}

	/**
	 * Classe helper per contenere il risultato dell'upload
	 */
	private static class FileUploadResult {
		final List<String> fileIds;
		final List<FileMetadata> metadata;

		FileUploadResult(List<String> fileIds, List<FileMetadata> metadata) {
			this.fileIds = fileIds;
			this.metadata = metadata;
		}
	}

	/**
	 * ✅ FIX: Nuova versione con metadati per aggiornamenti
	 * Tiene traccia dei file consumati dall'array files[] per gestire correttamente
	 * la numerazione sequenziale quando ci sono sia foto ricordo che allegati
	 */
	private FileUploadResult processAndUploadAttachmentsForUpdateWithMetadata(
			TravelEty travelEty, 
			List<MultipartFile> files, 
			List<String> existingFileIds,
			List<FileMetadata> existingMetadata) throws IOException {
		
		if (travelEty.getItinerary() == null) {
			return new FileUploadResult(existingFileIds, existingMetadata);
		}

		List<String> updatedFileIds = new ArrayList<>(existingFileIds);
		List<FileMetadata> updatedMetadata = new ArrayList<>(existingMetadata);
		
		// ✅ FIX CRITICO: Traccia il numero di file già consumati dall'array files[]
		// Questo è necessario perché il frontend assegna indici sequenziali globali
		// a TUTTI i nuovi file (foto ricordo + allegati), ma noi li processiamo separatamente
		FileConsumptionTracker tracker = new FileConsumptionTracker(files.size());
		
		for (DailyItineraryDTO dayDto : travelEty.getItinerary()) {
			// Processa l'immagine ricordo del giorno con metadati
			processDayMemoryImageForUpdateWithMetadata(dayDto, files, updatedFileIds, updatedMetadata, 
				travelEty.getUserId(), travelEty.getId(), existingFileIds.size(), tracker);
			
			// Processa gli allegati dei punti con metadati
			processPointAttachmentsForUpdateWithMetadata(dayDto, files, updatedFileIds, updatedMetadata, 
				travelEty.getUserId(), travelEty.getId(), existingFileIds.size(), tracker);
		}

		return new FileUploadResult(updatedFileIds, updatedMetadata);
	}
	
	/**
	 * ✅ Helper class per tracciare i file consumati dall'array files[]
	 */
	private static class FileConsumptionTracker {
		private int consumedCount = 0;
		private final int totalFiles;
		
		FileConsumptionTracker(int totalFiles) {
			this.totalFiles = totalFiles;
		}
		  
	}


	/**
	 * ✅ FIX DEFINITIVO: Processa immagine ricordo durante update CON metadati
	 * LOGICA CORRETTA:
	 * - Se memoryImageIndex < existingFilesCount → File GIÀ ESISTENTE (preserva l'indice)
	 * - Se memoryImageIndex >= existingFilesCount → NUOVO file da caricare dall'array files[]
	 * - Usa il tracker per calcolare correttamente la posizione nell'array files[]
	 */
	private void processDayMemoryImageForUpdateWithMetadata(
			DailyItineraryDTO dayDto, 
			List<MultipartFile> files,
			List<String> updatedFileIds,
			List<FileMetadata> updatedMetadata,
			String userId, 
			String travelId,
			int existingFilesCount,
			FileConsumptionTracker tracker) throws IOException {
		
		Integer memoryImageIndex = dayDto.getMemoryImageIndex();
		
		if (memoryImageIndex == null) {
			return;
		}
		
		// ✅ LOGICA CORRETTA:
		// Se l'indice punta a un file già esistente in updatedFileIds, preservalo
		if (memoryImageIndex < updatedFileIds.size()) {
			log.debug("📌 Preservo foto ricordo esistente per giorno {}, indice: {}", dayDto.getDay(), memoryImageIndex);
			return; // File già presente, mantieni l'indice
		}
		
		// ✅ Se l'indice è >= updatedFileIds.size(), è un NUOVO file da caricare
		// Calcola la posizione nell'array files[] sottraendo la dimensione degli esistenti
		int fileArrayIndex = memoryImageIndex - updatedFileIds.size();
		
		if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
			MultipartFile fileToUpload = files.get(fileArrayIndex);
			log.debug("📤 Caricamento NUOVA foto giorno {}, indice temporaneo: {}, posizione file: {}", 
				dayDto.getDay(), memoryImageIndex, fileArrayIndex);
			
			FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, userId, travelId, "day-memory");
			
			// Aggiungi alla fine dell'array
			int newIndex = updatedFileIds.size();
			updatedFileIds.add(metadata.getFileId());
			updatedMetadata.add(metadata);
			dayDto.setMemoryImageIndex(newIndex);
			log.debug("✅ Foto ricordo caricata per giorno {}, nuovo indice finale: {}", dayDto.getDay(), newIndex);
		} else {
			log.warn("⚠️ Indice file non valido per giorno {}: memoryImageIndex={}, fileArrayIndex={}, files.size()={}", 
				dayDto.getDay(), memoryImageIndex, fileArrayIndex, files.size());
		}
	}

	/**
	 * ✅ FIX DEFINITIVO: Processa allegati punti durante update CON metadati
	 * STESSA LOGICA delle foto ricordo:
	 * - Se index < existingFilesCount → File GIÀ ESISTENTE (preserva l'indice)
	 * - Se index >= existingFilesCount → NUOVO file da caricare dall'array files[]
	 * - Usa il tracker per calcolare correttamente la posizione nell'array files[]
	 */
	private void processPointAttachmentsForUpdateWithMetadata(
			DailyItineraryDTO dayDto, 
			List<MultipartFile> files,
			List<String> updatedFileIds,
			List<FileMetadata> updatedMetadata,
			String userId, 
			String travelId,
			int existingFilesCount,
			FileConsumptionTracker tracker) throws IOException {
		
		if (dayDto.getPoints() == null) {
			return;
		}

		for (PointDTO pointDto : dayDto.getPoints()) {
			List<Integer> attachmentIndices = pointDto.getAttachmentIndices();
			if (attachmentIndices != null && !attachmentIndices.isEmpty()) {
				List<Integer> updatedIndices = new ArrayList<>();
				
				for (Integer index : attachmentIndices) {
					// ✅ LOGICA CORRETTA:
					// Se l'indice punta a un file già esistente in updatedFileIds, preservalo
					if (index < updatedFileIds.size()) {
						updatedIndices.add(index);
						log.debug("📌 Preservo allegato esistente, indice: {}", index);
					} else {
						// ✅ Se l'indice è >= updatedFileIds.size(), è un NUOVO file da caricare
						// Calcola la posizione nell'array files[] sottraendo la dimensione degli esistenti
						int fileArrayIndex = index - updatedFileIds.size();
						
						if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
							MultipartFile fileToUpload = files.get(fileArrayIndex);
							log.debug("📤 Caricamento NUOVO allegato, indice temporaneo: {}, posizione file: {}", 
								index, fileArrayIndex);
							
							FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, userId, travelId, "point-attachment");
							
							int newIndex = updatedFileIds.size();
							updatedFileIds.add(metadata.getFileId());
							updatedMetadata.add(metadata);
							updatedIndices.add(newIndex);
							log.debug("✅ Allegato caricato, nuovo indice finale: {}", newIndex);
						} else {
							log.warn("⚠️ Indice file non valido per allegato: index={}, fileArrayIndex={}, files.size()={}", 
								index, fileArrayIndex, files.size());
						}
					}
				}
				
				pointDto.setAttachmentIndices(updatedIndices);
			}
		}
	}
	
	/**
	 * ✅ FIX: Nuova classe helper per tracciare anche i metadati
	 */
	private static class FileSyncStateWithMetadata {
	    int fileCounter = 0; 
	    final String userId;
	    final String travelId;
	    final List<String> allFileIds;
	    final List<FileMetadata> allFileMetadata;

	    FileSyncStateWithMetadata(String userId, String travelId, List<String> allFileIds, List<FileMetadata> allFileMetadata) {
	        this.userId = userId;
	        this.travelId = travelId;
	        this.allFileIds = allFileIds;
	        this.allFileMetadata = allFileMetadata;
	    }

	    int getAndIncrementIndex() {
	        return fileCounter++;
	    }
	}
 
	/**
	 * ✅ FIX: Processa immagine ricordo con metadati
	 */
	private void processDayMemoryImageWithMetadata(DailyItineraryDTO dayDto, List<MultipartFile> files, FileSyncStateWithMetadata state) throws IOException {
		Integer tempFileIndex = dayDto.getMemoryImageIndex();
		if (tempFileIndex != null && tempFileIndex >= 0 && tempFileIndex < files.size()) {
			MultipartFile fileToUpload = files.get(tempFileIndex);
			FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, state.userId, state.travelId, "day-memory");
			dayDto.setMemoryImageIndex(state.getAndIncrementIndex());
			state.allFileIds.add(metadata.getFileId());
			state.allFileMetadata.add(metadata);
		}
	}

	/**
	 * ✅ FIX: Processa allegati punti con metadati
	 */
	private void processPointAttachmentsWithMetadata(DailyItineraryDTO dayDto, List<MultipartFile> files, FileSyncStateWithMetadata state) throws IOException {
		if (dayDto.getPoints() == null) {
			return;
		}

		for (PointDTO pointDto : dayDto.getPoints()) {
			List<Integer> tempIndices = pointDto.getAttachmentIndices();
			if (tempIndices != null && !tempIndices.isEmpty()) {
				List<Integer> finalAttachmentIndices = new ArrayList<>();
				for (Integer tempFileIndex : tempIndices) {
					if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
						MultipartFile fileToUpload = files.get(tempFileIndex);
						FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, state.userId, state.travelId, "point-attachment");
						finalAttachmentIndices.add(state.getAndIncrementIndex());
						state.allFileIds.add(metadata.getFileId());
						state.allFileMetadata.add(metadata);
					}
				}
				pointDto.setAttachmentIndices(finalAttachmentIndices);
			}
		}
	}
  

    private void mergeCountryRegions(CountryVisit existing, CountryVisit newVisit) {
	    Map<String, RegionVisit> existingRegionsMap = existing.getRegions().stream()
	            .collect(Collectors.toMap(RegionVisit::getName, r -> r, (a, b) -> a));

	    for (RegionVisit newRegion : newVisit.getRegions()) {
	        String regionName = newRegion.getName();

	        if (existingRegionsMap.containsKey(regionName)) {
	            // Regione già presente: uniamo solo gli itinerari
	            RegionVisit existingRegion = existingRegionsMap.get(regionName);
	            mergeRegionItineraries(existingRegion, newRegion);
	        } else {
	            // Regione nuova: aggiungila
	            existing.getRegions().add(newRegion);
	        }
	    }
	}
    
    
    /**
     * Risolve tutti gli URL dei file nell'itinerario e valida le coordinate
     */
    public List<DailyItineraryDTO> resolveFileUrls(
            List<DailyItineraryDTO> itinerary,
            List<String> allFileIds,
            String travelId
    ) {
        if (allFileIds == null || allFileIds.isEmpty() || itinerary == null) {
            return itinerary;
        }

        for (DailyItineraryDTO dayDto : itinerary) {
            resolveMemoryImageUrl(dayDto, allFileIds, travelId);
            resolvePointAttachments(dayDto, allFileIds, travelId);
        }

        return itinerary;
    }

    /**
     * Risolve l'URL dell'immagine ricordo del giorno
     */
    private void resolveMemoryImageUrl(DailyItineraryDTO dayDto, List<String> allFileIds, String travelId) {
        if (dayDto.getMemoryImageIndex() != null) {
            try {
                int index = dayDto.getMemoryImageIndex();
                String fileId = allFileIds.get(index);
                String dayUrl = storageService.getPublicUrl(fileId);
                dayDto.setMemoryImageUrl(dayUrl);
            } catch (IndexOutOfBoundsException e) {
                log.error("Indice immagine giorno fuori limite per Travel ID: {}", travelId);
            }
        }
    }
    /**
     * ✅ NUOVO: Popola TUTTI gli URL (foto ricordo E allegati) dopo il salvataggio
     * Questo assicura che il frontend riceva tutti gli URL quando riapre il dialog di modifica
     */
    private void populateAllFileUrls(TravelDTO travelDTO, List<String> allFileIds, List<FileMetadata> fileMetadataList) {
        if (travelDTO == null || travelDTO.getItinerary() == null || allFileIds == null || allFileIds.isEmpty()) {
            return;
        }

        for (DailyItineraryDTO dayDto : travelDTO.getItinerary()) {
            // A) Popola URL foto ricordo
            if (dayDto.getMemoryImageIndex() != null) {
                try {
                    int index = dayDto.getMemoryImageIndex();
                    if (index >= 0 && index < allFileIds.size()) {
                        String fileId = allFileIds.get(index);
                        String dayUrl = storageService.getPublicUrl(fileId);
                        dayDto.setMemoryImageUrl(dayUrl);
                        log.debug("✅ URL foto ricordo popolato per giorno {}", dayDto.getDay());
                    }
                } catch (IndexOutOfBoundsException e) {
                    log.error("❌ Indice foto ricordo fuori limite per Travel ID: {}, index: {}", 
                        travelDTO.getTravelId(), dayDto.getMemoryImageIndex());
                }
            }

            // B) Popola URL allegati punti
            if (dayDto.getPoints() != null) {
                for (PointDTO pointDto : dayDto.getPoints()) {
                    if (pointDto.getAttachmentIndices() != null && !pointDto.getAttachmentIndices().isEmpty()) {
                        List<AttachmentUrlDTO> attachmentUrls = new ArrayList<>();

                        for (Integer index : pointDto.getAttachmentIndices()) {
                            try {
                                if (index >= 0 && index < allFileIds.size()) {
                                    String fileId = allFileIds.get(index);
                                    String attachmentUrl = storageService.getPublicUrl(fileId);
                                    
                                    String fileName = "file";
                                    String mimeType = "application/octet-stream";
                                    
                                    if (fileMetadataList != null && index < fileMetadataList.size()) {
                                        FileMetadata metadata = fileMetadataList.get(index);
                                        if (metadata != null) {
                                            fileName = metadata.getFileName();
                                            mimeType = metadata.getMimeType();
                                        }
                                    }
                                    
                                    attachmentUrls.add(new AttachmentUrlDTO(
                                        attachmentUrl,
                                        fileName,
                                        mimeType
                                    ));
                                    log.debug("✅ URL allegato popolato per punto {}", pointDto.getName());
                                }
                            } catch (IndexOutOfBoundsException e) {
                                log.error("❌ Indice allegato fuori limite per Travel ID: {}, index: {}", 
                                    travelDTO.getTravelId(), index);
                            }
                        }
                        
                        pointDto.setAttachmentUrls(attachmentUrls);
                    }
                }
            }
        }
    }

    /**
     * Risolve gli URL degli allegati per tutti i punti di un giorno
     */
    private void resolvePointAttachments(DailyItineraryDTO dayDto, List<String> allFileIds, String travelId) {
        if (dayDto.getPoints() == null) {
            return;
        }

        for (PointDTO pointDto : dayDto.getPoints()) {
            // Garantisce che 'coord' non sia mai null
            ensureValidCoordinates(pointDto);

            // Risolve gli allegati del punto
            resolveAttachmentUrls(pointDto, allFileIds, travelId);
        }
    }

    /**
     * Garantisce che un punto abbia coordinate valide (anche se vuote)
     */
    private void ensureValidCoordinates(PointDTO pointDto) {
        if (pointDto.getCoord() == null) {
            pointDto.setCoord(new CoordsDto(null, null));
        }
    }

    /**
     * Risolve gli URL degli allegati di un singolo punto
     * ✅ FIX: Aggiornato per usare AttachmentUrlDTO invece di String
     */
    private void resolveAttachmentUrls(PointDTO pointDto, List<String> allFileIds, String travelId) {
        if (pointDto.getAttachmentIndices() == null) {
            return;
        }

        List<AttachmentUrlDTO> attachmentUrls = new ArrayList<>();

        for (Integer index : pointDto.getAttachmentIndices()) {
            try {
                String fileId = allFileIds.get(index);
                String attachmentUrl = storageService.getPublicUrl(fileId);
                
                // ✅ FIX: Senza metadati, usa valori di fallback
                attachmentUrls.add(new AttachmentUrlDTO(
                    attachmentUrl,
                    "file", // Nome generico
                    "application/octet-stream" // Tipo MIME generico
                ));
            } catch (IndexOutOfBoundsException e) {
                log.error("Indice allegato fuori limite per Travel ID: {}", travelId);
            }
        }

        pointDto.setAttachmentUrls(attachmentUrls);
    }
    
	@Override
	public TravelDTO confirmTravelDates(String userId, String travelId) {
		// Recupera il viaggio
		Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelId, userId);
		
		if (!travelOpt.isPresent()) {
			throw new BusinessException("Viaggio non trovato o non autorizzato.");
		}
		
		TravelEty travel = travelOpt.get();
		
		// Rimuovi i flag di viaggio copiato
		travel.setIsCopied(false);
		travel.setNeedsDateConfirmation(false);
		
		// Salva le modifiche
		TravelEty savedTravel = travelRepository.save(travel);
		
		// Restituisci il DTO aggiornato
		return travelMapper.convertEtyToDTO(savedTravel);
	}

    public List<CountryVisit> getConsolidatedCountryVisits(String userId) {
        List<TravelEty> allTravels = travelRepository.findByUserId(userId);

        List<CountryVisit> countryVisitsPerTravel = allTravels.stream()
                .map(this::mapTravelToCountryVisit)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return consolidateCountryVisits(countryVisitsPerTravel);
    }

    /**
     * Consolida multiple visite allo stesso paese
     */
    private List<CountryVisit> consolidateCountryVisits(List<CountryVisit> countryVisits) {
        Map<String, CountryVisit> consolidatedMap = new HashMap<>();

        for (CountryVisit cv : countryVisits) {
            String countryIdentifier = cv.getIso();

            if (!consolidatedMap.containsKey(countryIdentifier)) {
                consolidatedMap.put(countryIdentifier, cv);
            } else {
                CountryVisit existing = consolidatedMap.get(countryIdentifier);
                mergeCountryVisits(existing, cv);
            }
        }

        return new ArrayList<>(consolidatedMap.values());
    }

    /**
     * Unisce due visite allo stesso paese
     */
    private void mergeCountryVisits(CountryVisit existing, CountryVisit newVisit) {
        // Unisci le date visitate
        existing.getVisitedDates().addAll(newVisit.getVisitedDates());

        // Unisci le regioni
        mergeCountryRegions(existing, newVisit);
    }


    /**
     * Unisce gli itinerari di due visite alla stessa regione
     */
    private void mergeRegionItineraries(RegionVisit existingRegion, RegionVisit newRegion) {
        if (newRegion.getItinerary() != null) {
            if (existingRegion.getItinerary() == null) {
                existingRegion.setItinerary(new ArrayList<>());
            }
            existingRegion.getItinerary().addAll(newRegion.getItinerary());
        }
    }

    /**
     * Mappa un TravelEty in un CountryVisit con URL delle foto risolti
     */
    private CountryVisit mapTravelToCountryVisit(TravelEty travelEty) {
        if (travelEty == null || travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
            return null;
        }

        // Risolvi gli URL dei file nell'itinerario
        List<DailyItineraryDTO> resolvedItineraries = resolveFileUrls(
                travelEty.getItinerary(),
                travelEty.getAllFileIds(),
                travelEty.getId()
        );

        // Raccogli tutti i punti
        List<PointDTO> allPoints = resolvedItineraries.stream()
                .flatMap(di -> di.getPoints().stream())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (allPoints.isEmpty()) {
            return null;
        }

        return buildCountryVisit(resolvedItineraries, allPoints, travelEty.getId(), travelEty.getTravelName());
    }

    /**
     * Costruisce un oggetto CountryVisit dai dati dell'itinerario
     */
    private CountryVisit buildCountryVisit(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints, String travelId, String travelName) {
        Optional<PointDTO> firstPoint = allPoints.stream().findFirst();

        CountryVisit cv = new CountryVisit();

        // Mappatura del Paese
        String countryName = firstPoint.map(PointDTO::getCountry).orElse("Nazione Sconosciuta");
        String countryIdentifier = countryName.replaceAll("\\s", "_").toUpperCase();
        cv.setIso(countryIdentifier);
        cv.setName(countryName);

        // Mappatura delle date visitate
        Set<String> visitedDates = itineraries.stream()
                .map(DailyItineraryDTO::getDate)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        cv.setVisitedDates(visitedDates);

        // Mappatura delle coordinate principali
        cv.setCoord(firstPoint.map(PointDTO::getCoord).orElse(null));

        // Mappatura delle regioni con travelId e travelName
        List<RegionVisit> regions = buildRegionVisits(itineraries, allPoints, travelId, travelName);
        cv.setRegions(regions);

        return cv;
    }

    /**
     * Costruisce la lista di RegionVisit raggruppando i punti per regione
     */
    private List<RegionVisit> buildRegionVisits(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints, String travelId, String travelName) {
        Map<String, List<PointDTO>> pointsByRegion = allPoints.stream()
                .filter(p -> p.getRegion() != null)
                .collect(Collectors.groupingBy(PointDTO::getRegion));

        List<RegionVisit> regions = new ArrayList<>();

        for (Map.Entry<String, List<PointDTO>> regionEntry : pointsByRegion.entrySet()) {
            String regionName = regionEntry.getKey();
            List<PointDTO> regionPoints = regionEntry.getValue();

            RegionVisit rv = buildRegionVisit(regionName, regionPoints, itineraries, travelId, travelName);
            regions.add(rv);
        }

        return regions;
    }

    /**
     * Costruisce un singolo RegionVisit per una regione specifica
     */
    private RegionVisit buildRegionVisit(String regionName, List<PointDTO> regionPoints, List<DailyItineraryDTO> allItineraries, String travelId, String travelName) {
        RegionVisit rv = new RegionVisit();
        rv.setId(UUID.randomUUID().toString());
        rv.setName(regionName);
        rv.setCoord(regionPoints.stream()
                .map(PointDTO::getCoord)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null));

        // Filtra gli itinerari per includere solo i punti di questa regione
        List<DailyItineraryDTO> regionItinerary = allItineraries.stream()
                .map(di -> filterItineraryByRegion(di, regionName))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        rv.setItinerary(regionItinerary);
        
        // Popola travelId e travelName per supportare i bookmark
        rv.setTravelId(travelId);
        rv.setTravelName(travelName);
        
        return rv;
    }

    /**
     * Filtra un itinerario giornaliero per includere solo i punti di una specifica regione
     */
    private DailyItineraryDTO filterItineraryByRegion(DailyItineraryDTO dayItinerary, String regionName) {
        List<PointDTO> filtered = dayItinerary.getPoints().stream()
                .filter(p -> regionName.equals(p.getRegion()))
                .collect(Collectors.toList());

        if (filtered.isEmpty()) {
            return null;
        }

        DailyItineraryDTO newDi = new DailyItineraryDTO();
        newDi.setDay(dayItinerary.getDay());
        newDi.setDate(dayItinerary.getDate());
        newDi.setPoints(filtered);
        newDi.setMemoryImageIndex(dayItinerary.getMemoryImageIndex());
        newDi.setMemoryImageUrl(dayItinerary.getMemoryImageUrl());

        return newDi;
    }

    /**
     * Elimina la foto ricordo di un giorno specifico
     */
    @Override
    public TravelDTO deleteMemoryPhoto(String userId, String travelId, int dayNumber) {
        // Recupera il viaggio
        Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelId, userId);
        
        if (!travelOpt.isPresent()) {
            throw new BusinessException("Viaggio non trovato o non autorizzato.");
        }
        
        TravelEty travel = travelOpt.get();
        
        // Trova il giorno nell'itinerario
        if (travel.getItinerary() == null || travel.getItinerary().isEmpty()) {
            throw new BusinessException("Itinerario non trovato.");
        }
        
        DailyItineraryDTO targetDay = null;
        for (DailyItineraryDTO day : travel.getItinerary()) {
            if (day.getDay() == dayNumber) {
                targetDay = day;
                break;
            }
        }
        
        if (targetDay == null) {
            throw new BusinessException("Giorno " + dayNumber + " non trovato nell'itinerario.");
        }
        
        // Verifica se esiste una foto ricordo per questo giorno
        if (targetDay.getMemoryImageIndex() == null) {
            throw new BusinessException("Nessuna foto ricordo da eliminare per il giorno " + dayNumber);
        }
        
        // Ottieni l'indice del file da eliminare
        Integer memoryImageIndex = targetDay.getMemoryImageIndex();
        
        // Elimina il riferimento alla foto ricordo dal giorno
        targetDay.setMemoryImageIndex(null);
        targetDay.setMemoryImageUrl(null);
        
        // Salva il viaggio aggiornato
        TravelEty savedTravel = travelRepository.save(travel);
        
        // Converti in DTO e popola gli URL rimanenti
        TravelDTO resultDTO = travelMapper.convertEtyToDTO(savedTravel);
        populateAllFileUrls(resultDTO, savedTravel.getAllFileIds(), savedTravel.getFileMetadataList());
        
        log.info("✅ Foto ricordo eliminata per il viaggio {} - giorno {}, indice file: {}", 
            travelId, dayNumber, memoryImageIndex);
        
        return resultDTO;
    }
}
