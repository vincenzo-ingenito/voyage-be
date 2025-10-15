package it.voyage.ms.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.PointDTO;
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
	public TravelDTO updateExistingTravel(String ownerUid, String travelId, TravelDTO newTravelData){
		Optional<TravelEty> existingTravelOpt = travelRepository.findByIdAndUserId(travelId, ownerUid);
		TravelEty existingTravel = existingTravelOpt.get();
		existingTravel.setTravelName(newTravelData.getTravelName());
		existingTravel.setDateFrom(newTravelData.getDateFrom());
		existingTravel.setDateTo(newTravelData.getDateTo());
		List<DailyItineraryDTO> updatedItinerary = mapDayDTOListToDayList(newTravelData.getItinerary());
		existingTravel.setItinerary(updatedItinerary);
		TravelEty savedTravel = travelRepository.save(existingTravel);
		return travelMapper.convertEtyToDTO(savedTravel);
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
			// NOTA: 'getAllFileIds()' è un metodo concettuale sul tuo TravelEty
			List<String> allFileIds = entity.getAllFileIds(); 

			// Se non ci sono file IDs, non c'è nulla da risolvere
			if (allFileIds == null || allFileIds.isEmpty()) {
				travelDTOs.add(dto);
				continue; 
			}

			// 3. Itera e risolvi le URL usando gli indici
			if (dto.getItinerary() != null) {
				for (DailyItineraryDTO dayDto : dto.getItinerary()) {

					// A) Risoluzione Immagine Ricordo del Giorno
					if (dayDto.getMemoryImageIndex() != null) {
						try {
							int index = dayDto.getMemoryImageIndex();
							String fileId = allFileIds.get(index);

							String dayUrl = storageService.getPublicUrl(fileId);
							dayDto.setMemoryImageUrl(dayUrl); 

						} catch (IndexOutOfBoundsException e) {
							System.err.println("Indice immagine giorno fuori limite per Travel ID: " + dto.getTravelId());
							// Imposta URL a null o lascia non risolto
						}
					}

					// B) Risoluzione Allegati Punti
					if (dayDto.getPoints() != null) {
						for (PointDTO pointDto : dayDto.getPoints()) {

							if (pointDto.getAttachmentIndices() != null) {
								List<String> attachmentUrls = new ArrayList<>();

								for (Integer index : pointDto.getAttachmentIndices()) {
									try {
										String fileId = allFileIds.get(index);
										String attachmentUrl = storageService.getPublicUrl(fileId);
										attachmentUrls.add(attachmentUrl);
									} catch (IndexOutOfBoundsException e) {
										System.err.println("Indice allegato fuori limite per Travel ID: " + dto.getTravelId());
									}
								}
								pointDto.setAttachmentUrls(attachmentUrls);
							}
						}
					}
				}
			}

			travelDTOs.add(dto);
		}

		return travelDTOs;
	}


	@Override
	public Boolean deleteTravelById(String travelId, String userId) {
		long deletedCount = travelRepository.deleteByIdAndUserId(travelId, userId);
		return deletedCount > 0;
	}

	//	@Override
	//	public TravelEty saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) { 
	//	    TravelEty travel =  travelMapper.convertDtoToEty(travelData);  
	//	    String userId = userDetails.getUserId();
	//
	//	    if (StringUtils.isBlank(travel.getId())) {
	//	        travel.setId(UUID.randomUUID().toString());
	//	    }
	//	    travel.setUserId(userId);
	//
	//	    try {
	//	        List<String> uploadedFileIds = processAndUploadAttachments(travel, files);
	//	        travel.setAllFileIds(uploadedFileIds); 
	//
	//	    } catch (Exception e) {
	//	        log.error("Errore generico durante l'upload per il viaggio {}: {}", travel.getId(), e.getMessage(), e);
	//	        throw new BusinessException("Errore sconosciuto durante l'elaborazione dei file.", e);
	//	    }
	//
	//	    return travelRepository.save(travel);
	//	}

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

			List<String> uploadedFileIds = processAndUploadAttachments(savedTravel, files);
			savedTravel.setAllFileIds(uploadedFileIds); 

			return travelMapper.convertEtyToDTO(travelRepository.save(savedTravel)); 

		} catch (Exception e) {
			log.error("Errore generico durante il salvataggio del viaggio {}: {}", travel.getId(), e.getMessage(), e);
			throw new BusinessException("Errore sconosciuto durante il salvataggio del viaggio.", e);
		}
	}

	//	private List<String> processAndUploadAttachments(TravelEty travelEty, List<MultipartFile> files) throws IOException {
	//
	//		List<String> allFileIds = new ArrayList<>();
	//		int fileCounter = 0; // Contatore globale per l'indice finale (0, 1, 2, ...)
	//
	//		String userId = travelEty.getUserId();
	//		String travelId = travelEty.getId();
	//
	//		List<DailyItineraryDTO> itinerary = travelEty.getItinerary();
	//
	//		if (itinerary != null) {
	//
	//			for (DailyItineraryDTO dayDto : itinerary) {
	//
	//				// 1. Processa l'Immagine Ricordo del Giorno
	//				if (dayDto.getMemoryImageIndex() != null) {
	//
	//					int tempFileIndex = dayDto.getMemoryImageIndex();
	//
	//					if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
	//						MultipartFile fileToUpload = files.get(tempFileIndex);
	//
	//						// 🌟 CHIAMATA ALL'UPLOAD: Restituisce il PATH (ID) del file in Storage
	//						String fileId = storageService.uploadFile(fileToUpload, userId, travelId, "day-memory"); 
	//
	//						// 🌟 IMPOSTA L'INDICE FINALE
	//						dayDto.setMemoryImageIndex(fileCounter);
	//
	//						allFileIds.add(fileId);
	//						fileCounter++;
	//					}
	//				}
	//
	//				// 2. Processa gli Allegati dei Punti
	//				if (dayDto.getPoints() != null) {
	//					for (PointDTO pointDto : dayDto.getPoints()) {
	//
	//						if (pointDto.getAttachmentIndices() != null && !pointDto.getAttachmentIndices().isEmpty()) {
	//
	//							List<Integer> finalAttachmentIndices = new ArrayList<>();
	//
	//							for (Integer tempFileIndex : pointDto.getAttachmentIndices()) {
	//
	//								if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
	//									MultipartFile fileToUpload = files.get(tempFileIndex);
	//
	//									String fileId = storageService.uploadFile(fileToUpload, userId, travelId, "point-attachment"); 
	//
	//									finalAttachmentIndices.add(fileCounter);
	//
	//									allFileIds.add(fileId);
	//									fileCounter++;
	//								}
	//							}
	//							// Sostituisce la lista di indici temporanei con quelli permanenti
	//							pointDto.setAttachmentIndices(finalAttachmentIndices);
	//						}
	//					}
	//				}
	//			}
	//		}
	//
	//		return allFileIds;
	//	}

	private List<String> processAndUploadAttachments(TravelEty travelEty, List<MultipartFile> files) throws IOException {

		if (travelEty.getItinerary() == null) {
			return Collections.emptyList();
		}

		List<String> allFileIds = new ArrayList<>();
		FileSyncState state = new FileSyncState(travelEty.getUserId(), travelEty.getId(), allFileIds);

		for (DailyItineraryDTO dayDto : travelEty.getItinerary()) {

			// 1. Delega il processamento dell'immagine ricordo (SRP)
			processDayMemoryImage(dayDto, files, state);

			// 2. Delega il processamento degli allegati dei punti (SRP)
			processPointAttachments(dayDto, files, state);
		}

		return allFileIds;
	}

	private void processDayMemoryImage(DailyItineraryDTO dayDto, List<MultipartFile> files, FileSyncState state) throws IOException {
		Integer tempFileIndex = dayDto.getMemoryImageIndex();
		if (tempFileIndex != null && tempFileIndex >= 0 && tempFileIndex < files.size()) {
			MultipartFile fileToUpload = files.get(tempFileIndex);
			String fileId = storageService.uploadFile(fileToUpload, state.userId, state.travelId, "day-memory"); 
			dayDto.setMemoryImageIndex(state.getAndIncrementIndex());
			state.allFileIds.add(fileId);
		}
	}

	private void processPointAttachments(DailyItineraryDTO dayDto, List<MultipartFile> files, FileSyncState state) throws IOException {

		if (dayDto.getPoints() == null) {
			return;
		}

		for (PointDTO pointDto : dayDto.getPoints()) {
			List<Integer> tempIndices = pointDto.getAttachmentIndices();
			if (tempIndices != null && !tempIndices.isEmpty()) {
				List<Integer> finalAttachmentIndices = new ArrayList<>();
				for (Integer tempFileIndex : tempIndices) {
					// Controlla se l'indice è valido
					if (tempFileIndex >= 0 && tempFileIndex < files.size()) {
						MultipartFile fileToUpload = files.get(tempFileIndex);
						String fileId = storageService.uploadFile(fileToUpload, state.userId, state.travelId, "point-attachment"); 
						finalAttachmentIndices.add(state.getAndIncrementIndex());
						state.allFileIds.add(fileId);
					}
				}
				pointDto.setAttachmentIndices(finalAttachmentIndices);
			}
		}
	}
	
	private static class FileSyncState {
	    int fileCounter = 0; 
	    final String userId;
	    final String travelId;
	    final List<String> allFileIds;

	    FileSyncState(String userId, String travelId, List<String> allFileIds) {
	        this.userId = userId;
	        this.travelId = travelId;
	        this.allFileIds = allFileIds;
	    }

	    int getAndIncrementIndex() {
	        return fileCounter++;
	    }
	}
}
