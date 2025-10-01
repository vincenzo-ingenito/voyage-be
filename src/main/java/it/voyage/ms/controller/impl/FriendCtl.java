package it.voyage.ms.controller.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
import it.voyage.ms.dto.response.TravelDTO;
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

@RestController
@RequestMapping("/api/friends")
public class FriendCtl {

	@Autowired
	private IFriendRelationshipRepository friendRelationshipRepository;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TravelRepository travelRepo;

	@GetMapping("/accepted")
	public ResponseEntity<List<UserDto>> getAcceptedFriends(@AuthenticationPrincipal FirebaseToken user) {

		// Recupera le relazioni accettate
		List<FriendRelationshipEty> relationships = friendRelationshipRepository
				.findByRequesterIdAndStatusOrReceiverIdAndStatus(
						user.getUid(),
						FriendRelationshipStatusEnum.ACCEPTED.name(),
						user.getUid(),
						FriendRelationshipStatusEnum.ACCEPTED.name()
						);

		// Costruisce la lista degli ID amici
		List<String> friendIds = relationships.stream()
				.map(rel -> rel.getRequesterId().equals(user.getUid()) ? rel.getReceiverId() : rel.getRequesterId())
				.collect(Collectors.toList());

		// Recupera gli utenti amici
		List<UserEty> friends = userRepository.findAllById(friendIds);

		// Converte in DTO gli amici
		List<UserDto> friendDtos = friends.stream()
				.map(f -> UserDto.fromEntityWithUid(f, user.getUid()))
				.collect(Collectors.toList());

		// Recupera l'utente loggato e lo mette sempre primo
		UserEty currentUser = userRepository.findById(user.getUid()).orElse(null);
		List<UserDto> output = new ArrayList<>();
		if (currentUser != null) {
			output.add(UserDto.fromEntityWithUid(currentUser, user.getUid())); // Primo elemento
		}
		output.addAll(friendDtos); // Seguono tutti gli amici

		return ResponseEntity.ok(output);
	}



	@GetMapping("/requests/pending")
	public ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(@AuthenticationPrincipal FirebaseToken user) {
		List<FriendRelationshipEty> pendingRequests = friendRelationshipRepository.findByReceiverIdAndStatus(user.getUid(), FriendRelationshipStatusEnum.PENDING.name());
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

	@PostMapping
	public ResponseEntity<String> saveTravel(@RequestBody TravelDTO travelData,@AuthenticationPrincipal FirebaseToken userFirebase) {
		TravelEty travel = convertToDocument(travelData);
		travel.setUserId(userFirebase.getUid()); 
		travelRepo.save(travel);


		return ResponseEntity.ok("Travel data saved successfully!");
	}


	@GetMapping("/{friendId}/visited")
	public ResponseEntity<List<CountryVisit>> getVisitedCountries(@PathVariable String friendId,@AuthenticationPrincipal FirebaseToken userFirebase) {

		if(!friendId.equals(userFirebase.getUid())) {
			List<FriendRelationshipEty> relationships = friendRelationshipRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
					userFirebase.getUid(), FriendRelationshipStatusEnum.ACCEPTED.name(),
					userFirebase.getUid(), FriendRelationshipStatusEnum.ACCEPTED.name()
					);

			if(relationships.isEmpty()) {
				throw new NotFoundException("");
			}
		} 

//		List<TravelEty> travels = travelRepo.findByUserId(friendId);
//		List<CountryVisit> output = new ArrayList<>();
//		for(TravelEty travel : travels) {
//			output.add(CountryVisit.mapToCountryVisit(travel));	
//		}

		return ResponseEntity.ok(getUniqueConsolidatedCountryVisits());
	}

	//START VI
	public List<CountryVisit> getUniqueConsolidatedCountryVisits() {

		// 1. Ottieni tutti i viaggi dal database
		// Assumiamo che travelRepository.findAll() restituisca List<TravelEty>
		List<TravelEty> allTravels = travelRepo.findAll(); 

		// 2. Mappa tutti i viaggi in oggetti CountryVisit (identificati dal nome del Paese)
		List<CountryVisit> countryVisitsPerTravel = allTravels.stream()
				.map(CountryVisit::mapToCountryVisit) // Usa il tuo metodo fornito
				.filter(Objects::nonNull)
				.collect(Collectors.toList());

		// 3. RAGGRUPPAMENTO E CONSOLIDAMENTO (RISOLUZIONE DEL PROBLEMA DELLE CHIAVI DUPLICATE!)
		Map<String, CountryVisit> consolidatedMap = new HashMap<>();

		for (CountryVisit cv : countryVisitsPerTravel) {
			// La chiave di raggruppamento è l'identificatore del Paese creato nel mapper (es. "FRANCIA")
			String countryIdentifier = cv.getIso(); 

			if (!consolidatedMap.containsKey(countryIdentifier)) {
				// Primo incontro con questo Paese: lo aggiungo
				consolidatedMap.put(countryIdentifier, cv);
			} else {
				// Paese già presente: unisco i dati (consolidamento)
				CountryVisit existing = consolidatedMap.get(countryIdentifier);

				// Unisci le date visitate
				existing.getVisitedDates().addAll(cv.getVisitedDates());

				// Unisci le regioni, evitando duplicati
				Map<String, RegionVisit> existingRegions = existing.getRegions().stream()
						.collect(Collectors.toMap(RegionVisit::getName, r -> r, (a, b) -> a)); 

				for (RegionVisit newRegion : cv.getRegions()) {
					if (!existingRegions.containsKey(newRegion.getName())) {
						existing.getRegions().add(newRegion);
					}
				}
			}
		}

		// 4. Restituisci la lista di Paesi unici al frontend
		return new ArrayList<>(consolidatedMap.values());
	}
	//END VI


	private TravelEty convertToDocument(TravelDTO dto) {
		TravelEty travel = new TravelEty();
		travel.setTravelName(dto.getTravelName());

		List<DailyItineraryDTO> itineraryDocuments = dto.getItinerary().stream()
				.map(dayDTO -> {
					DailyItineraryDTO day = new DailyItineraryDTO();
					day.setDay(dayDTO.getDay());
					day.setDate(dayDTO.getDate());


					List<PointDTO> pointDocuments = dayDTO.getPoints().stream()
							.map(pointDTO -> {
								PointDTO point = new PointDTO();
								point.setName(pointDTO.getName());
								point.setType(pointDTO.getType());
								point.setDescription(pointDTO.getDescription());
								point.setCost(pointDTO.getCost());
								point.setCoord(new CoordsDto(pointDTO.getCoord().getLat(),pointDTO.getCoord().getLng()));
								point.setCountry(pointDTO.getCountry());
								point.setRegion(pointDTO.getRegion());
								point.setCity(pointDTO.getCity());
								return point;
							}).collect(Collectors.toList());

					day.setPoints(pointDocuments);
					return day;
				}).collect(Collectors.toList());

		travel.setItinerary(itineraryDocuments);
		return travel;
	}

}