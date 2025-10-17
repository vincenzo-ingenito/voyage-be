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

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItineraryDTO;
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
     */
    private void resolveAttachmentUrls(PointDTO pointDto, List<String> allFileIds, String travelId) {
        if (pointDto.getAttachmentIndices() == null) {
            return;
        }

        List<String> attachmentUrls = new ArrayList<>();

        for (Integer index : pointDto.getAttachmentIndices()) {
            try {
                String fileId = allFileIds.get(index);
                String attachmentUrl = storageService.getPublicUrl(fileId);
                attachmentUrls.add(attachmentUrl);
            } catch (IndexOutOfBoundsException e) {
                log.error("Indice allegato fuori limite per Travel ID: {}", travelId);
            }
        }

        pointDto.setAttachmentUrls(attachmentUrls);
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

        return buildCountryVisit(resolvedItineraries, allPoints);
    }

    /**
     * Costruisce un oggetto CountryVisit dai dati dell'itinerario
     */
    private CountryVisit buildCountryVisit(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints) {
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

        // Mappatura delle regioni
        List<RegionVisit> regions = buildRegionVisits(itineraries, allPoints);
        cv.setRegions(regions);

        return cv;
    }

    /**
     * Costruisce la lista di RegionVisit raggruppando i punti per regione
     */
    private List<RegionVisit> buildRegionVisits(List<DailyItineraryDTO> itineraries, List<PointDTO> allPoints) {
        Map<String, List<PointDTO>> pointsByRegion = allPoints.stream()
                .filter(p -> p.getRegion() != null)
                .collect(Collectors.groupingBy(PointDTO::getRegion));

        List<RegionVisit> regions = new ArrayList<>();

        for (Map.Entry<String, List<PointDTO>> regionEntry : pointsByRegion.entrySet()) {
            String regionName = regionEntry.getKey();
            List<PointDTO> regionPoints = regionEntry.getValue();

            RegionVisit rv = buildRegionVisit(regionName, regionPoints, itineraries);
            regions.add(rv);
        }

        return regions;
    }

    /**
     * Costruisce un singolo RegionVisit per una regione specifica
     */
    private RegionVisit buildRegionVisit(String regionName, List<PointDTO> regionPoints, List<DailyItineraryDTO> allItineraries) {
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
