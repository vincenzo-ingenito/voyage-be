package it.voyage.ms.controller.impl;

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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.CoordsDto;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.RegionVisit;
import it.voyage.ms.dto.response.SearchRequest;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.IFriendRelationshipRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.impl.FirebaseStorageService;

@RestController
@RequestMapping("/api/friends")
public class FriendCtl {

	@Autowired
	private IFriendRelationshipRepository friendRelationshipRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TravelRepository travelRepo;
	
	@Autowired
	private FirebaseStorageService storageService; 


	@GetMapping("/accepted")
	public ResponseEntity<List<UserDto>> getAcceptedFriends(@AuthenticationPrincipal CustomUserDetails user) {

		// Recupera le relazioni accettate
		List<FriendRelationshipEty> relationships = friendRelationshipRepository
				.findByRequesterIdAndStatusOrReceiverIdAndStatus(
						user.getUserId(),
						FriendRelationshipStatusEnum.ACCEPTED.name(),
						user.getUserId(),
						FriendRelationshipStatusEnum.ACCEPTED.name()
						);

		// Costruisce la lista degli ID amici
		List<String> friendIds = relationships.stream()
				.map(rel -> rel.getRequesterId().equals(user.getUserId()) ? rel.getReceiverId() : rel.getRequesterId())
				.collect(Collectors.toList());

		// Recupera gli utenti amici
		List<UserEty> friends = userRepository.findAllById(friendIds);

		// Converte in DTO gli amici
		List<UserDto> friendDtos = friends.stream()
				.map(f -> UserDto.fromEntityWithUid(f, user.getUserId()))
				.collect(Collectors.toList());

		// Recupera l'utente loggato e lo mette sempre primo
		UserEty currentUser = userRepository.findById(user.getUserId()).orElse(null);
		List<UserDto> output = new ArrayList<>();
		if (currentUser != null) {
			output.add(UserDto.fromEntityWithUid(currentUser, user.getUserId())); // Primo elemento
		}
		output.addAll(friendDtos); // Seguono tutti gli amici

		return ResponseEntity.ok(output);
	}



	@GetMapping("/requests/pending")
	public ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(@AuthenticationPrincipal CustomUserDetails user) {
		List<FriendRelationshipEty> pendingRequests = friendRelationshipRepository.findByReceiverIdAndStatus(user.getUserId(), FriendRelationshipStatusEnum.PENDING.name());
		List<FriendRelationshipDto> dtos = new ArrayList<>();
		for(FriendRelationshipEty f:pendingRequests) {
			FriendRelationshipDto dto = new FriendRelationshipDto();
			dto.setCreatedAt(f.getCreatedAt());
			dto.setReceiverId(f.getReceiverId());
			dto.setRequesterId(f.getRequesterId());
			Optional<UserEty> userEty = userRepository.findById(f.getRequesterId());
			dto.setAvatar(userEty.get().getAvatar());
			dto.setName(userEty.get().getName());
			dtos.add(dto);
		}

		return ResponseEntity.ok(dtos);
	}


	@PostMapping("/search")
	public ResponseEntity<List<UserSearchResult>> searchUsers(@RequestBody SearchRequest searchRequest, @AuthenticationPrincipal FirebaseToken userFirebase) {
		String query = searchRequest.getQuery();

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		// Trova tutti gli ID degli utenti che hanno bloccato l'utente corrente
		List<String> blockedByUserIds = friendRelationshipRepository.findByReceiverIdAndStatus(userFirebase.getUid(), "BLOCKED").stream()
				.map(FriendRelationshipEty::getRequesterId)
				.collect(Collectors.toList());

		// Trova tutti gli utenti che corrispondono alla query, escludendo l'utente corrente e quelli che lo hanno bloccato
		List<UserEty> users = userRepository.findByNameRegex(query).stream()
				.filter(user -> !user.getId().equals(userFirebase.getUid()))
				.filter(user -> !blockedByUserIds.contains(user.getId()))
				.collect(Collectors.toList());

		// Per ogni utente trovato, determina lo stato della relazione con l'utente corrente
		List<UserSearchResult> results = users.stream()
				.map(user -> {
					FriendRelationshipStatusEnum status;

					// Controllo aggiunto per vedere se l'utente corrente ha bloccato la persona trovata
					if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "BLOCKED").size() > 0) {
						status = FriendRelationshipStatusEnum.BLOCKED;
					} else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "ACCEPTED").size() > 0 ||
							friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(user.getId(), userFirebase.getUid(), "ACCEPTED").size() > 0) {
						status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
					} else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "PENDING").size() > 0) {
						FriendRelationshipEty pendingRequest = friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "PENDING").stream().findFirst().orElse(null);
						if (pendingRequest != null) {
							status = FriendRelationshipStatusEnum.PENDING_REQUEST_SENT;
						} else {
							status = FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED;
						}
					} else {
						status = FriendRelationshipStatusEnum.AVAILABLE;
					}

					return new UserSearchResult(user.getId(), user.getName(), user.getAvatar(), status);
				})
				.collect(Collectors.toList());

		return ResponseEntity.ok(results);
	}



	@PostMapping("/unblock")
	public ResponseEntity<?> unblockUser(@RequestBody String userIdToUnblock, @AuthenticationPrincipal FirebaseToken userFirebase) {

		if (userIdToUnblock == null || userIdToUnblock.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		// Trova la relazione di blocco
		List<FriendRelationshipEty> relationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), userIdToUnblock, "BLOCKED");

		if (relationships.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Elimina la relazione di blocco
		friendRelationshipRepository.delete(relationships.get(0));

		return ResponseEntity.ok().build();
	}
 

	@GetMapping("/{friendId}/visited")
	public ResponseEntity<List<CountryVisit>> getVisitedCountries(@PathVariable String friendId, @AuthenticationPrincipal CustomUserDetails user) {

	    if (!friendId.equals(user.getUserId())) {
	        List<FriendRelationshipEty> relationships = friendRelationshipRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
	        		user.getUserId(), FriendRelationshipStatusEnum.ACCEPTED.name(),
	        		user.getUserId(), FriendRelationshipStatusEnum.ACCEPTED.name()
	        );

	        if (relationships.isEmpty()) {
	            throw new NotFoundException("");
	        }
	    }
	    // Devi passare friendId al metodo di consolidamento.
	    List<CountryVisit> countryVisit = getUniqueConsolidatedCountryVisits(friendId); 
	    return ResponseEntity.ok(countryVisit);
	}
	
	public List<CountryVisit> getUniqueConsolidatedCountryVisits(String userId) { // Aggiunto parametro userId

	    // Filtra per utente/amico
	    List<TravelEty> allTravels = travelRepo.findByUserId(userId); 

	    List<CountryVisit> countryVisitsPerTravel = allTravels.stream()
	            // Usa il metodo di mappatura istanza per risolvere gli URL e correggere i bug
	            .map(this::mapTravelEtyToCountryVisitWithPhotoUrl) 
	            .filter(Objects::nonNull)
	            .collect(Collectors.toList());

	    Map<String, CountryVisit> consolidatedMap = new HashMap<>();

	    for (CountryVisit cv : countryVisitsPerTravel) {
	        String countryIdentifier = cv.getIso(); 

	        if (!consolidatedMap.containsKey(countryIdentifier)) {
	            consolidatedMap.put(countryIdentifier, cv);
	        } else {
	            CountryVisit existing = consolidatedMap.get(countryIdentifier);

	            // Unisci le date visitate
	            existing.getVisitedDates().addAll(cv.getVisitedDates());

	            // Unisci le regioni usando gli helper per gestire gli itinerari
	            mergeCountryRegions(existing, cv); 
	        }
	    }

	    return new ArrayList<>(consolidatedMap.values());
	}
	
	private List<DailyItineraryDTO> resolveTravelFileUrlsAndValidateCoords(
		    List<DailyItineraryDTO> itinerary, 
		    List<String> allFileIds,
		    String travelId
		) {
		    if (allFileIds == null || allFileIds.isEmpty() || itinerary == null) {
		        return itinerary; 
		    }
		    
		    for (DailyItineraryDTO dayDto : itinerary) {
		        
		        // Risoluzione Immagine Ricordo del Giorno
		        if (dayDto.getMemoryImageIndex() != null) {
		            try {
		                int index = dayDto.getMemoryImageIndex();
		                String fileId = allFileIds.get(index);
		                String dayUrl = storageService.getPublicUrl(fileId);
		                dayDto.setMemoryImageUrl(dayUrl); 
		            } catch (IndexOutOfBoundsException e) {
		                System.err.println("Indice immagine giorno fuori limite per Travel ID: " + travelId);
		            }
		        }
		        
		        if (dayDto.getPoints() != null) {
		            for (PointDTO pointDto : dayDto.getPoints()) {
		                
		                // CORREZIONE BUG COORD: Garantisce che 'coord' non sia mai null
		                if (pointDto.getCoord() == null) {
		                    pointDto.setCoord(new CoordsDto(null, null)); 
		                }

		                // Risoluzione Allegati Punti
		                if (pointDto.getAttachmentIndices() != null) {
		                    List<String> attachmentUrls = new ArrayList<>();
		                    
		                    for (Integer index : pointDto.getAttachmentIndices()) {
		                        try {
		                            String fileId = allFileIds.get(index);
		                            String attachmentUrl = storageService.getPublicUrl(fileId);
		                            attachmentUrls.add(attachmentUrl);
		                        } catch (IndexOutOfBoundsException e) {
		                            System.err.println("Indice allegato fuori limite per Travel ID: " + travelId);
		                        }
		                    }
		                    pointDto.setAttachmentUrls(attachmentUrls);
		                }
		            }
		        }
		    }
		    
		    return itinerary;
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

	private void mergeRegionItineraries(RegionVisit existingRegion, RegionVisit newRegion) {
	    if (newRegion.getItinerary() != null) {
	        if (existingRegion.getItinerary() == null) {
	            existingRegion.setItinerary(new ArrayList<>());
	        }
	        existingRegion.getItinerary().addAll(newRegion.getItinerary());
	    }
	}
	
	private CountryVisit mapTravelEtyToCountryVisitWithPhotoUrl(TravelEty travelEty) {
	    if (travelEty == null || travelEty.getItinerary() == null || travelEty.getItinerary().isEmpty()) {
	        return null;
	    }

	    // 1. Pulizia dei dati e risoluzione degli URL (chiama l'helper)
	    List<DailyItineraryDTO> resolvedItineraries = this.resolveTravelFileUrlsAndValidateCoords(
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

	    Optional<PointDTO> firstPoint = allPoints.stream().findFirst();

	    CountryVisit cv = new CountryVisit();

	    // Mappatura del Paese
	    String countryName = firstPoint.map(PointDTO::getCountry).orElse("Nazione Sconosciuta");
	    String countryIdentifier = countryName.replaceAll("\\s", "_").toUpperCase();
	    cv.setIso(countryIdentifier);
	    cv.setName(countryName);

	    // Mappatura delle date visitate
	    Set<String> visitedDates = resolvedItineraries.stream()
	            .map(DailyItineraryDTO::getDate)
	            .filter(Objects::nonNull)
	            .collect(Collectors.toCollection(LinkedHashSet::new));
	    cv.setVisitedDates(visitedDates);

	    // Mappatura delle coordinate principali
	    cv.setCoord(firstPoint.map(PointDTO::getCoord).orElse(null));

	    // 2. Logica di Raggruppamento per Regione
	    Map<String, List<PointDTO>> pointsByRegion = allPoints.stream()
	            .filter(p -> p.getRegion() != null)
	            .collect(Collectors.groupingBy(PointDTO::getRegion));

	    List<RegionVisit> regions = new ArrayList<>();
	    for (Map.Entry<String, List<PointDTO>> regionEntry : pointsByRegion.entrySet()) {
	        
	        String regionName = regionEntry.getKey();
	        List<PointDTO> regionPoints = regionEntry.getValue();

	        RegionVisit rv = new RegionVisit();
	        rv.setId(UUID.randomUUID().toString());
	        rv.setName(regionName);
	        rv.setCoord(regionPoints.stream()
	                .map(PointDTO::getCoord)
	                .filter(Objects::nonNull)
	                .findFirst()
	                .orElse(null));

	        // Ricostruisco gli itinerari filtrati per la regione
	        List<DailyItineraryDTO> regionItinerary = resolvedItineraries.stream()
	                .map(di -> {
	                    List<PointDTO> filtered = di.getPoints().stream()
	                            .filter(p -> regionName.equals(p.getRegion()))
	                            .collect(Collectors.toList());
	                    if (filtered.isEmpty()) return null;

	                    DailyItineraryDTO newDi = new DailyItineraryDTO();
	                    newDi.setDay(di.getDay());
	                    newDi.setDate(di.getDate());
	                    newDi.setPoints(filtered);
	                    
	                    // Copiamo i campi risolti
	                    newDi.setMemoryImageIndex(di.getMemoryImageIndex());
	                    newDi.setMemoryImageUrl(di.getMemoryImageUrl()); 
	                    
	                    return newDi;
	                })
	                .filter(Objects::nonNull)
	                .collect(Collectors.toList());

	        rv.setItinerary(regionItinerary);
	        regions.add(rv);
	    }

	    cv.setRegions(regions);

	    return cv;
	}

}