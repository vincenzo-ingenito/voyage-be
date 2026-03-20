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
import it.voyage.ms.repository.entity.DailyItineraryEty;
import it.voyage.ms.repository.entity.PointEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelFileEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class TravelService implements ITravelService {

	private final TravelRepository travelRepository;
	private final FirebaseStorageService storageService;
	private final TravelMapper travelMapper;
	private final it.voyage.ms.repository.impl.UserRepository userRepository;
	private final it.voyage.ms.repository.impl.BookmarkRepository bookmarkRepository;


	@Override
	@Transactional
	public Boolean deleteTravelById(Long travelId, String userId) {
	    Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserIdWithFiles(travelId, userId);

	    if (!travelOpt.isPresent()) {
	        log.warn("Tentativo di eliminare viaggio non esistente o non autorizzato: travelId={}, userId={}", travelId, userId);
	        return false;
	    }

	    // PRIMA elimina tutti i bookmark associati al viaggio per evitare violazione di foreign key
	    int deletedBookmarksCount = bookmarkRepository.deleteByTravelId(travelId);
	    if (deletedBookmarksCount > 0) {
	        log.info("Eliminati {} bookmark associati al viaggio {}", deletedBookmarksCount, travelId);
	    }

	    // POI elimina il viaggio
	    travelRepository.deleteById(travelId);
	    log.info("Viaggio {} eliminato dal database", travelId);

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
	public TravelDTO updateExistingTravel(String ownerUid, Long travelId, TravelDTO newTravelData, List<MultipartFile> files){
		try {
			log.info("UPDATE TRAVEL - Inizio aggiornamento viaggio ID: {}", travelId);
		 
			Optional<TravelEty> existingTravelOpt = travelRepository.findByIdAndUserId(travelId, ownerUid);
			
			if (!existingTravelOpt.isPresent()) {
				throw new BusinessException("Viaggio non trovato o non autorizzato.");
			}

			TravelEty existingTravel = existingTravelOpt.get();
			
			// Inizializza le collezioni lazy
			org.hibernate.Hibernate.initialize(existingTravel.getItinerary());
			if (existingTravel.getItinerary() != null) {
				existingTravel.getItinerary().forEach(day -> {
					org.hibernate.Hibernate.initialize(day.getPoints());
				});
			}
			org.hibernate.Hibernate.initialize(existingTravel.getFiles());

			// Salva i file esistenti PRIMA di modificare l'itinerario
			List<TravelFileEty> existingFiles = new ArrayList<>(existingTravel.getFiles());

			// Aggiorna i dati base del viaggio
			existingTravel.setTravelName(newTravelData.getTravelName());
			existingTravel.setDateFrom(newTravelData.getDateFrom());
			existingTravel.setDateTo(newTravelData.getDateTo());

			// Rimuovi il vecchio itinerario (verrà ricreato dal mapper)
			existingTravel.getItinerary().clear();

			// Converti il nuovo itinerario dal DTO
			TravelEty updatedTravelFromDto = travelMapper.convertDtoToEty(newTravelData);
			
			// LOG CRITICO: Verifica cosa esce dal mapper
			log.info("DOPO MAPPER - Verifica Entity convertita:");
			if (updatedTravelFromDto.getItinerary() != null) {
				for (DailyItineraryEty dayEty : updatedTravelFromDto.getItinerary()) {
					log.info("ENTITY DAY {} - memoryImageIndex: {}", 
						dayEty.getDay(), dayEty.getMemoryImageIndex());
				}
			}
			
			// Copia il nuovo itinerario nell'entità esistente
			if (updatedTravelFromDto.getItinerary() != null) {
				for (DailyItineraryEty newDay : updatedTravelFromDto.getItinerary()) {
					newDay.setTravel(existingTravel);
					log.info("AGGIUNTA DAY {} - memoryImageIndex finale: {}", newDay.getDay(), newDay.getMemoryImageIndex());
					existingTravel.getItinerary().add(newDay);
				}
			}

			// Gestisci i file: sia nuovi che esistenti
			int existingFilesCount = existingFiles.size();
			
			if (files != null && !files.isEmpty()) {
				log.info("Caricamento di {} nuovi file per il viaggio {}", files.size(), travelId);
				
				// Carica i nuovi file su Firebase e aggiorna gli indici nell'itinerario
				FileUploadResult uploadResult = processAndUploadAttachmentsForUpdate(
					existingTravel, 
					files, 
					existingFilesCount
				);
				
				// Aggiungi i nuovi file a quelli esistenti
				for (FileMetadata metadata : uploadResult.metadata) {
					TravelFileEty fileEty = new TravelFileEty();
					fileEty.setFileId(metadata.getFileId());
					fileEty.setFileName(metadata.getFileName());
					fileEty.setMimeType(metadata.getMimeType());
					fileEty.setUploadDate(java.time.LocalDateTime.now());
					fileEty.setTravel(existingTravel);
					existingFiles.add(fileEty);
				}
			} else {
				log.info("Nessun nuovo file, preservo solo gli indici esistenti per il viaggio {}", travelId);
				// Anche se non ci sono nuovi file, devo assicurarmi che gli indici
				// nell'itinerario siano validi (< existingFilesCount)
				validateExistingFileIndices(existingTravel, existingFilesCount);
			}
			
			// Ripristina tutti i file (vecchi + nuovi)
			existingTravel.getFiles().clear();
			existingTravel.getFiles().addAll(existingFiles);

			// Salva il viaggio aggiornato
			log.info("Salvataggio viaggio aggiornato su PostgreSQL");
			TravelEty savedTravel = travelRepository.save(existingTravel);
			
			log.info("Viaggio aggiornato con successo. ID: {}, Files: {}", savedTravel.getId(), savedTravel.getFiles().size());

			// Converti in DTO e popola gli URL
			TravelDTO resultDTO = travelMapper.convertEtyToDTO(savedTravel);
			populateAllFileUrls(resultDTO, savedTravel.getAllFileIds(), savedTravel.getFileMetadataList());

			return resultDTO;
			
		} catch (IOException e) {
			log.error("Errore durante il caricamento dei file per il viaggio {}: {}", travelId, e.getMessage(), e);
			throw new BusinessException("Errore durante il caricamento dei file.", e);
		} catch (Exception e) {
			log.error("Errore durante l'aggiornamento del viaggio {}: {}", travelId, e.getMessage(), e);
			throw new BusinessException("Errore durante l'aggiornamento del viaggio.", e);
		}
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

								pointEty.setAttachmentIndices(pointDTO.getAttachmentIndices());

								return pointEty;
							}).collect(Collectors.toList());

					dayEty.setPoints(pointEties);
					return dayEty;
				}).collect(Collectors.toList());
	}

	@Override
	@Transactional
	public List<TravelDTO> getTravelsForUser(String userId) {
		List<TravelEty> travelEntities = travelRepository.findByUserId(userId);
		List<TravelDTO> travelDTOs = new ArrayList<>();

		for (TravelEty entity : travelEntities) {
			TravelDTO dto = travelMapper.convertEtyToDTO(entity);
			List<FileMetadata> fileMetadataList = entity.getFileMetadataList();

			// OTTIMIZZAZIONE: NON generiamo gli URL signed qui per ridurre il payload
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
											
											// URL vuoto ma fileId sempre presente
											attachmentMetadata.add(new AttachmentUrlDTO(null, metadata.getFileName(), metadata.getMimeType(), metadata.getFileId()));
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

	@Transactional
	public TravelDTO getTravelWithUrls(String userId, Long travelId) {
		log.info("GET TRAVEL WITH URLS - Caricamento viaggio ID: {}", travelId);
		
		Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelId, userId);

		if (!travelOpt.isPresent()) {
			throw new BusinessException("Viaggio non trovato o non autorizzato.");
		}

		TravelEty entity = travelOpt.get();
		
		// Forza il caricamento delle collezioni lazy
		org.hibernate.Hibernate.initialize(entity.getItinerary());
		if (entity.getItinerary() != null) {
			entity.getItinerary().forEach(day -> {
				org.hibernate.Hibernate.initialize(day.getPoints());
			});
		}
		org.hibernate.Hibernate.initialize(entity.getFiles());
		
		// LOG CRITICO: Verifica cosa c'è nel DB
		if (entity.getItinerary() != null) {
			for (DailyItineraryEty dayEty : entity.getItinerary()) {
				log.info("DB DAY {} - memoryImageIndex: {}", dayEty.getDay(), dayEty.getMemoryImageIndex());
			}
		}
		
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
				if (dayDto.getMemoryImageIndex() != null) {
					try {
						int index = dayDto.getMemoryImageIndex();
						log.info("GENERAZIONE URL - Day {} memoryImageIndex: {}, allFileIds.size: {}", dayDto.getDay(), index, allFileIds.size());
						String fileId = allFileIds.get(index);
						String dayUrl = storageService.getPublicUrl(fileId);
						dayDto.setMemoryImageUrl(dayUrl);
						log.info("URL GENERATO - Day {}: {}", dayDto.getDay(), dayUrl != null ? "OK" : "NULL");
					} catch (IndexOutOfBoundsException e) {
						log.error("Indice immagine giorno fuori limite per Travel ID: {}, index: {}, size: {}", dto.getTravelId(), dayDto.getMemoryImageIndex(), allFileIds.size());
					}
				} else {
					log.warn("Day {} - memoryImageIndex è NULL, nessun URL da generare", dayDto.getDay());
				}

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

									attachmentUrls.add(new AttachmentUrlDTO(attachmentUrl, fileName, mimeType, allFileIds.get(index)));
								} catch (IndexOutOfBoundsException e) {
									log.error("Indice allegato fuori limite per Travel ID: {}", dto.getTravelId());
								}
							}
							pointDto.setAttachmentUrls(attachmentUrls);
						}
					}
				}
			}
		}

		return dto;
	}

	@Override
	@Transactional
	public TravelDTO saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
	    try {
	        // 1. Converti DTO in Entity
	        TravelEty travel = travelMapper.convertDtoToEty(travelData);

	        // 2. Se è un viaggio copiato, assicurati che tutti gli ID siano null
	        if (Boolean.TRUE.equals(travelData.getIsCopied())) {
	            log.info("Viaggio copiato rilevato, rimuovo tutti gli ID per permettere la creazione di nuove entità");
	            clearAllIds(travel);
	        }

	        // 3. Carica l'utente e setta la relazione
	        UserEty user = userRepository.findById(userDetails.getUserId())
	            .orElseThrow(() -> new BusinessException("Utente non trovato"));
	        travel.setUser(user);

	        // 4. SALVA PRIMA IL VIAGGIO PER OTTENERE L'ID
	        log.info("Salvataggio viaggio su PostgreSQL per ottenere l'ID");
	        TravelEty savedTravel = travelRepository.save(travel);
	        
	        // 5. ORA processa e carica i file su Firebase usando l'ID appena generato
	        FileUploadResult uploadResult;
	        if (files != null && !files.isEmpty()) {
	            log.info("Caricamento di {} file su Firebase Storage", files.size());
	            uploadResult = processAndUploadAttachmentsWithMetadata(savedTravel, files);
	        } else {
	            log.info("Nessun file da caricare");
	            uploadResult = new FileUploadResult(Collections.emptyList(), Collections.emptyList());
	        }

	        // 6. Crea le entità TravelFileEty e associale al viaggio
	        savedTravel.getFiles().clear();
	        for (FileMetadata metadata : uploadResult.metadata) {
	            TravelFileEty fileEty = new TravelFileEty();
	            fileEty.setFileId(metadata.getFileId());
	            fileEty.setFileName(metadata.getFileName());
	            fileEty.setMimeType(metadata.getMimeType());
	            fileEty.setUploadDate(java.time.LocalDateTime.now());
	            fileEty.setTravel(savedTravel);
	            savedTravel.getFiles().add(fileEty);
	        }

	        // 7. Salva nuovamente per aggiornare con i file
	        log.info("Aggiornamento viaggio con {} file", savedTravel.getFiles().size());
	        savedTravel = travelRepository.save(savedTravel);

	        log.info("Viaggio salvato con successo. ID: {}, Files: {}", savedTravel.getId(), savedTravel.getFiles().size());
	        TravelDTO resultDTO = travelMapper.convertEtyToDTO(savedTravel);
	        populateAllFileUrls(resultDTO, savedTravel.getAllFileIds(), savedTravel.getFileMetadataList());
	        return resultDTO;

	    } catch (IOException e) {
	        log.error("Errore durante il caricamento dei file: {}", e.getMessage(), e);
	        // ROLLBACK: cancella i file da Firebase se esistono
	        throw new BusinessException("Errore durante il caricamento dei file.", e);
	    } catch (Exception e) {
	        log.error("Errore durante il salvataggio del viaggio: {}", e.getMessage(), e);
	        throw new BusinessException("Errore durante il salvataggio del viaggio.", e);
	    }
	}
	
	private FileUploadResult processAndUploadAttachmentsWithMetadata(TravelEty travelEty, List<MultipartFile> files) throws IOException {
	    if (travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
	        return new FileUploadResult(Collections.emptyList(), Collections.emptyList());
	    }

	    List<String> allFileIds = new ArrayList<>();
	    List<FileMetadata> allFileMetadata = new ArrayList<>();
	    
	    FileSyncStateWithMetadata state = new FileSyncStateWithMetadata(travelEty.getUser().getId(), travelEty.getId(), allFileIds, allFileMetadata);

	    // Corretto: itera su DailyItineraryEty, non DTO
	    for (DailyItineraryEty dailyEty : travelEty.getItinerary()) {
	        processDayMemoryImageWithMetadata(dailyEty, files, state);
	        processPointAttachmentsWithMetadata(dailyEty, files, state);
	    }

	    return new FileUploadResult(allFileIds, allFileMetadata);
	}

	/**
	 * Rimuove tutti gli ID da un viaggio e dalle sue entità correlate
	 * Questo è necessario quando si copia un viaggio per evitare conflitti di chiavi duplicate
	 */
	private void clearAllIds(TravelEty travel) {
		// Rimuovi l'ID del viaggio
		travel.setId(null);
		
		// Rimuovi gli ID dall'itinerario
		if (travel.getItinerary() != null) {
			for (DailyItineraryEty dailyItinerary : travel.getItinerary()) {
				dailyItinerary.setId(null);
				
				// Rimuovi gli ID dai punti
				if (dailyItinerary.getPoints() != null) {
					for (PointEty point : dailyItinerary.getPoints()) {
						point.setId(null);
					}
				}
			}
		}
		
		// I file verranno creati ex novo durante il processamento, quindi non hanno ID da rimuovere
		log.debug("Rimossi tutti gli ID dal viaggio copiato per permettere la creazione di nuove entità");
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

	private static class FileSyncStateWithMetadata {
		int fileCounter = 0; 
		final String userId;
		final Long travelId;
		final List<String> allFileIds;
		final List<FileMetadata> allFileMetadata;

		FileSyncStateWithMetadata(String userId, Long travelId, List<String> allFileIds, List<FileMetadata> allFileMetadata) {
			this.userId = userId;
			this.travelId = travelId;
			this.allFileIds = allFileIds;
			this.allFileMetadata = allFileMetadata;
		}

		int getAndIncrementIndex() {
			return fileCounter++;
		}
	}
	private void processDayMemoryImageWithMetadata(DailyItineraryEty dayDto, List<MultipartFile> files, FileSyncStateWithMetadata state) throws IOException {
		Integer tempFileIndex = dayDto.getMemoryImageIndex();
		if (tempFileIndex != null && tempFileIndex >= 0 && tempFileIndex < files.size()) {
			MultipartFile fileToUpload = files.get(tempFileIndex);
			FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, state.userId, state.travelId, "day-memory");
			dayDto.setMemoryImageIndex(state.getAndIncrementIndex());
			state.allFileIds.add(metadata.getFileId());
			state.allFileMetadata.add(metadata);
		}
	}

	private void processPointAttachmentsWithMetadata(DailyItineraryEty dayEty, List<MultipartFile> files, FileSyncStateWithMetadata state) throws IOException {
		if (dayEty.getPoints() == null) {
			return;
		}

		for (PointEty pointEty : dayEty.getPoints()) {
			// Leggi gli attachment indices dal JSON
			List<Integer> tempIndices = parseAttachmentIndicesFromJson(pointEty.getAttachmentIndicesJson());
			
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
				// Riscrivi gli indici aggiornati in JSON
				pointEty.setAttachmentIndicesJson(convertIndicesToJson(finalAttachmentIndices));
			}
		}
	}

	/**
	 * Valida che tutti gli indici dei file nell'itinerario siano < existingFilesCount
	 * Questo serve quando non ci sono nuovi file da caricare ma dobbiamo preservare quelli esistenti
	 */
	private void validateExistingFileIndices(TravelEty travelEty, int existingFilesCount) {
		if (travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
			return;
		}

		for (DailyItineraryEty dayEty : travelEty.getItinerary()) {
			// Valida foto ricordo
			Integer memoryIndex = dayEty.getMemoryImageIndex();
			if (memoryIndex != null) {
				if (memoryIndex >= existingFilesCount) {
					log.warn("Foto ricordo con indice {} fuori range (max: {}), rimuovo riferimento", memoryIndex, existingFilesCount - 1);
					dayEty.setMemoryImageIndex(null);
				} else {
					log.debug("Foto ricordo valida, indice: {}", memoryIndex);
				}
			}

			// Valida allegati punti
			if (dayEty.getPoints() != null) {
				for (PointEty pointEty : dayEty.getPoints()) {
					List<Integer> indices = parseAttachmentIndicesFromJson(pointEty.getAttachmentIndicesJson());
					
					if (indices != null && !indices.isEmpty()) {
						List<Integer> validIndices = new ArrayList<>();
						
						for (Integer index : indices) {
							if (index < existingFilesCount) {
								validIndices.add(index);
								log.debug("Allegato valido, indice: {}", index);
							} else {
								log.warn("Allegato con indice {} fuori range (max: {}), rimuovo riferimento", index, existingFilesCount - 1);
							}
						}
						
						pointEty.setAttachmentIndicesJson(convertIndicesToJson(validIndices));
					}
				}
			}
		}
	}

	/**
	 * Processa e carica i nuovi file durante l'aggiornamento, preservando i file esistenti
	 */
	private FileUploadResult processAndUploadAttachmentsForUpdate(TravelEty travelEty, List<MultipartFile> files, int existingFilesCount) throws IOException {
		if (travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
			return new FileUploadResult(Collections.emptyList(), Collections.emptyList());
		}

		List<String> newFileIds = new ArrayList<>();
		List<FileMetadata> newFileMetadata = new ArrayList<>();
		
		// State per tracciare l'upload dei nuovi file
		FileSyncStateForUpdate state = new FileSyncStateForUpdate(travelEty.getUser().getId(), travelEty.getId(), newFileIds, newFileMetadata, existingFilesCount);

		// Itera sull'itinerario e processa i file
		for (DailyItineraryEty dailyEty : travelEty.getItinerary()) {
			processDayMemoryImageForUpdate(dailyEty, files, state);
			processPointAttachmentsForUpdate(dailyEty, files, state);
		}

		return new FileUploadResult(newFileIds, newFileMetadata);
	}

	/**
	 * Classe helper per gestire lo stato durante l'upload in update
	 */
	private static class FileSyncStateForUpdate {
		final String userId;
		final Long travelId;
		final List<String> newFileIds;
		final List<FileMetadata> newFileMetadata;
		final int existingFilesCount;
		int fileCounter = 0;

		FileSyncStateForUpdate(String userId, Long travelId, List<String> newFileIds, List<FileMetadata> newFileMetadata, int existingFilesCount) {
			this.userId = userId;
			this.travelId = travelId;
			this.newFileIds = newFileIds;
			this.newFileMetadata = newFileMetadata;
			this.existingFilesCount = existingFilesCount;
		}

		int getNextGlobalIndex() {
			return existingFilesCount + fileCounter++;
		}
	}

	/**
	 * Processa la foto ricordo di un giorno durante l'update
	 */
	private void processDayMemoryImageForUpdate(DailyItineraryEty dayEty, List<MultipartFile> files, FileSyncStateForUpdate state) throws IOException {
		Integer memoryImageIndex = dayEty.getMemoryImageIndex();
		
		if (memoryImageIndex == null) {
			return;
		}

		// Se l'indice è < existingFilesCount, è un file esistente da preservare
		if (memoryImageIndex < state.existingFilesCount) {
			log.debug("Preservo foto ricordo esistente, indice: {}", memoryImageIndex);
			return;
		}

		// Altrimenti è un nuovo file da caricare
		int fileArrayIndex = memoryImageIndex - state.existingFilesCount;
		
		if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
			MultipartFile fileToUpload = files.get(fileArrayIndex);
			log.debug("Caricamento nuova foto ricordo, fileArrayIndex: {}", fileArrayIndex);
			
			FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, state.userId, state.travelId, "day-memory");
			
			int newGlobalIndex = state.getNextGlobalIndex();
			state.newFileIds.add(metadata.getFileId());
			state.newFileMetadata.add(metadata);
			dayEty.setMemoryImageIndex(newGlobalIndex);
			
			log.debug("Foto ricordo caricata, nuovo indice globale: {}", newGlobalIndex);
		} else {
			log.warn("Indice file non valido per foto ricordo: memoryImageIndex={}, fileArrayIndex={}, files.size()={}", 
					memoryImageIndex, fileArrayIndex, files.size());
		}
	}

	/**
	 * Processa gli allegati dei punti durante l'update
	 */
	private void processPointAttachmentsForUpdate(DailyItineraryEty dayEty, List<MultipartFile> files, FileSyncStateForUpdate state) throws IOException {
		if (dayEty.getPoints() == null) {
			return;
		}

		for (PointEty pointEty : dayEty.getPoints()) {
			List<Integer> attachmentIndices = parseAttachmentIndicesFromJson(pointEty.getAttachmentIndicesJson());
			
			if (attachmentIndices == null || attachmentIndices.isEmpty()) {
				continue;
			}

			List<Integer> updatedIndices = new ArrayList<>();

			for (Integer index : attachmentIndices) {
				// Se l'indice è < existingFilesCount, è un file esistente da preservare
				if (index < state.existingFilesCount) {
					updatedIndices.add(index);
					log.debug("Preservo allegato esistente, indice: {}", index);
				} else {
					// Altrimenti è un nuovo file da caricare
					int fileArrayIndex = index - state.existingFilesCount;
					
					if (fileArrayIndex >= 0 && fileArrayIndex < files.size()) {
						MultipartFile fileToUpload = files.get(fileArrayIndex);
						log.debug("Caricamento nuovo allegato, fileArrayIndex: {}", fileArrayIndex);
						
						FileMetadata metadata = storageService.uploadFileWithMetadata(fileToUpload, state.userId, state.travelId, "point-attachment");
						
						int newGlobalIndex = state.getNextGlobalIndex();
						state.newFileIds.add(metadata.getFileId());
						state.newFileMetadata.add(metadata);
						updatedIndices.add(newGlobalIndex);
						
						log.debug("Allegato caricato, nuovo indice globale: {}", newGlobalIndex);
					} else {
						log.warn("Indice file non valido per allegato: index={}, fileArrayIndex={}, files.size()={}", 
								index, fileArrayIndex, files.size());
					}
				}
			}

			pointEty.setAttachmentIndicesJson(convertIndicesToJson(updatedIndices));
		}
	}
	
	/**
	 * Converte una stringa JSON in lista di indici
	 */
	private List<Integer> parseAttachmentIndicesFromJson(String json) {
		if (json == null || json.isEmpty() || "[]".equals(json)) {
			return Collections.emptyList();
		}
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			return mapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<List<Integer>>() {});
		} catch (Exception e) {
			log.error("Errore parsing attachment indices JSON: {}", json, e);
			return Collections.emptyList();
		}
	}
	
	/**
	 * Converte una lista di indici in stringa JSON
	 */
	private String convertIndicesToJson(List<Integer> indices) {
		if (indices == null || indices.isEmpty()) {
			return "[]";
		}
		try {
			com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
			return mapper.writeValueAsString(indices);
		} catch (Exception e) {
			log.error("Errore conversione indici a JSON", e);
			return "[]";
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
	public List<DailyItineraryDTO> resolveFileUrls(List<DailyItineraryDTO> itinerary, List<String> allFileIds, String travelId) {
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
						log.debug("URL foto ricordo popolato per giorno {}", dayDto.getDay());
					}
				} catch (IndexOutOfBoundsException e) {
					log.error("Indice foto ricordo fuori limite per Travel ID: {}, index: {}", travelDTO.getTravelId(), dayDto.getMemoryImageIndex());
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

									String currentFileId = allFileIds.get(index);
									attachmentUrls.add(new AttachmentUrlDTO(
											attachmentUrl,
											fileName,
											mimeType,
											currentFileId
											));
									log.debug("URL allegato popolato per punto {}", pointDto.getName());
								}
							} catch (IndexOutOfBoundsException e) {
								log.error("Indice allegato fuori limite per Travel ID: {}, index: {}", travelDTO.getTravelId(), index);
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

	private void resolveAttachmentUrls(PointDTO pointDto, List<String> allFileIds, String travelId) {
		if (pointDto.getAttachmentIndices() == null) {
			return;
		}

		List<AttachmentUrlDTO> attachmentUrls = new ArrayList<>();

		for (Integer index : pointDto.getAttachmentIndices()) {
			try {
				String fileId = allFileIds.get(index);
				String attachmentUrl = storageService.getPublicUrl(fileId);

				attachmentUrls.add(new AttachmentUrlDTO(attachmentUrl, "file", "application/octet-stream", fileId));
			} catch (IndexOutOfBoundsException e) {
				log.error("Indice allegato fuori limite per Travel ID: {}", travelId);
			}
		}

		pointDto.setAttachmentUrls(attachmentUrls);
	}

	@Override
	@Transactional
	public TravelDTO confirmTravelDates(String userId, String travelId) {
		// Recupera il viaggio
		Long travelIdLong = Long.parseLong(travelId);
		Optional<TravelEty> travelOpt = travelRepository.findByIdAndUserId(travelIdLong, userId);

		if (!travelOpt.isPresent()) {
			throw new BusinessException("Viaggio non trovato o non autorizzato.");
		}

		TravelEty travel = travelOpt.get();
		
		// Inizializza le collezioni lazy
		org.hibernate.Hibernate.initialize(travel.getItinerary());
		if (travel.getItinerary() != null) {
			travel.getItinerary().forEach(day -> {
				org.hibernate.Hibernate.initialize(day.getPoints());
			});
		}
		org.hibernate.Hibernate.initialize(travel.getFiles());

		// Rimuovi i flag di viaggio copiato
		travel.setIsCopied(false);
		travel.setNeedsDateConfirmation(false);

		// Salva le modifiche
		TravelEty savedTravel = travelRepository.save(travel);

		// Restituisci il DTO aggiornato
		return travelMapper.convertEtyToDTO(savedTravel);
	}

	@Override
	@Transactional
	public List<CountryVisit> getConsolidatedCountryVisits(String userId) {
		List<TravelEty> allTravels = travelRepository.findByUserId(userId);
		
		// Inizializza le collezioni lazy per tutti i viaggi
		allTravels.forEach(travel -> {
			org.hibernate.Hibernate.initialize(travel.getItinerary());
			if (travel.getItinerary() != null) {
				travel.getItinerary().forEach(day -> {
					org.hibernate.Hibernate.initialize(day.getPoints());
				});
			}
			org.hibernate.Hibernate.initialize(travel.getFiles());
		});
		
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

		// Converti le entità in DTO usando il mapper
		TravelDTO travelDTO = travelMapper.convertEtyToDTO(travelEty);
		List<DailyItineraryDTO> itineraryDTOs = travelDTO.getItinerary();

		// Risolvi gli URL dei file
		List<DailyItineraryDTO> resolvedItineraries = resolveFileUrls(itineraryDTOs, travelEty.getAllFileIds(), String.valueOf(travelEty.getId()));

		// Raccogli tutti i punti
		List<PointDTO> allPoints = resolvedItineraries.stream().flatMap(di -> di.getPoints().stream()).filter(Objects::nonNull).collect(Collectors.toList());

		if (allPoints.isEmpty()) {
			return null;
		}

		return buildCountryVisit(resolvedItineraries, allPoints, String.valueOf(travelEty.getId()), travelEty.getTravelName());
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
		rv.setCoord(regionPoints.stream().map(PointDTO::getCoord).filter(Objects::nonNull).findFirst().orElse(null));

		// Filtra gli itinerari per includere solo i punti di questa regione
		List<DailyItineraryDTO> regionItinerary = allItineraries.stream().map(di -> filterItineraryByRegion(di, regionName)).filter(Objects::nonNull).collect(Collectors.toList());
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
}
