package it.voyage.ms.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.Coords;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItinerary;
import it.voyage.ms.dto.response.DailyItineraryDTO;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.PointDTO;
import it.voyage.ms.dto.response.PointOfInterest;
import it.voyage.ms.dto.response.RegionVisit;
import it.voyage.ms.dto.response.SearchRequest;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.repository.entity.FriendRelationship;
import it.voyage.ms.repository.entity.Point;
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

	private enum FriendshipStatus {
		PENDING, ACCEPTED, BLOCKED
	}

	static class FriendRequestDTO {
		private String friendId;

		public String getFriendId() {
			return friendId;
		}

		public void setFriendId(String friendId) {
			this.friendId = friendId;
		}
	}
 

//	@PostMapping("/accept")
//	public ResponseEntity<String> acceptFriendRequest(@RequestBody FriendRequestDTO requestDTO, @AuthenticationPrincipal FirebaseToken user) {
//		Optional<FriendRelationship> relationship = friendRelationshipRepository
//				.findByRequesterIdAndReceiverId(requestDTO.getFriendId(), user.getUid());
//
//		if (relationship.isPresent() && relationship.get().getStatus().equals(FriendshipStatus.PENDING.name())) {
//			FriendRelationship rel = relationship.get();
//			rel.setStatus(FriendshipStatus.ACCEPTED.name());
//			friendRelationshipRepository.save(rel);
//			return ResponseEntity.ok("Richiesta di amicizia accettata.");
//		}
//
//		return ResponseEntity.badRequest().body("Richiesta di amicizia non trovata o non in stato di attesa.");
//	}

	@GetMapping("/accepted")
	public ResponseEntity<List<UserEty>> getAcceptedFriends(@AuthenticationPrincipal FirebaseToken user) {

		List<FriendRelationship> relationships = friendRelationshipRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
				user.getUid(), FriendshipStatus.ACCEPTED.name(),
				user.getUid(), FriendshipStatus.ACCEPTED.name()
				);

		List<String> friendIds = relationships.stream()
				.map(rel -> rel.getRequesterId().equals(user.getUid()) ? rel.getReceiverId() : rel.getRequesterId())
				.collect(Collectors.toList());

		List<UserEty> friends = userRepository.findAllById(friendIds);
		return ResponseEntity.ok(friends);
	}

	@GetMapping("/requests/pending")
	public ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(@AuthenticationPrincipal FirebaseToken user) {
		List<FriendRelationship> pendingRequests = friendRelationshipRepository.findByReceiverIdAndStatus(user.getUid(), "PENDING");
		List<FriendRelationshipDto> dtos = new ArrayList<>();
		for(FriendRelationship f:pendingRequests) {
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
				.map(FriendRelationship::getRequesterId)
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

					// CONTROLLO AGGIUNTO: Se l'utente corrente ha bloccato la persona trovata, imposta lo stato a BLOCKED.
					if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "BLOCKED").size() > 0) {
						status = FriendRelationshipStatusEnum.BLOCKED;
					}
					// Controlla se sono già amici
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "ACCEPTED").size() > 0 ||
							friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(user.getId(), userFirebase.getUid(), "ACCEPTED").size() > 0) {
						status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
					} 
					// Controlla se c'è una richiesta in sospeso inviata dall'utente corrente
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), user.getId(), "PENDING_REQUEST_SENT").size() > 0) {
						status = FriendRelationshipStatusEnum.PENDING_REQUEST_SENT;
					}
					// Controlla se c'è una richiesta in sospeso ricevuta dall'utente corrente
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(user.getId(), userFirebase.getUid(), "PENDING_REQUEST_RECEIVED").size() > 0) {
						status = FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED;
					} 
					// Se non ci sono relazioni esistenti, l'utente è disponibile
					else {
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
		List<FriendRelationship> relationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), userIdToUnblock, "BLOCKED");

		if (relationships.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Elimina la relazione di blocco
		friendRelationshipRepository.delete(relationships.get(0));

		return ResponseEntity.ok().build();
	}

	@PostMapping
	public ResponseEntity<String> saveTravel(@RequestBody TravelDTO travelData,@AuthenticationPrincipal FirebaseToken userFirebaset) {
		TravelEty travel = convertToDocument(travelData);
		travel.setUserId(userFirebaset.getUid()); 
		travelRepo.save(travel);


		return ResponseEntity.ok("Travel data saved successfully!");
	}

	@GetMapping("/travels")
	public ResponseEntity<List<TravelDTO>> getTravels(@AuthenticationPrincipal FirebaseToken userFirebaset) {
		List<TravelEty> travelEty = travelRepo.findByUserId(userFirebaset.getUid());
		List<TravelDTO> output = new ArrayList<>();
		for(TravelEty t : travelEty) {
			output.add(convertToDTO(t));
		}
		return ResponseEntity.ok(output);
	}

	@GetMapping("/{friendId}/visited")
	public ResponseEntity<List<CountryVisit>> getVisitedCountries(@PathVariable String friendId) {
		List<CountryVisit> mockVisitedCountries = new ArrayList<>();

		CountryVisit italy = new CountryVisit();
		italy.setIso("IT");
		italy.setName("Italia");
		italy.setVisitedDates(Set.of("2023-09-01", "2024-05-15"));
		italy.setCoord(new Coords());
		italy.getCoord().setLat(41.9028);
		italy.getCoord().setLng(12.4964);

		RegionVisit lazio = new RegionVisit();
		lazio.setId("IT-LA");
		lazio.setName("Lazio");
		lazio.setCoord(new Coords());
		lazio.getCoord().setLat(41.9028);
		lazio.getCoord().setLng(12.4964);

		DailyItinerary itineraryDay1 = new DailyItinerary();
		itineraryDay1.setDay(1);
		itineraryDay1.setDate("2023-09-01");
		itineraryDay1.setPoints(Arrays.asList(
				createPoint("Colosseo", "attraction", 41.8902, 12.4922, "Visita del Colosseo."),
				createPoint("Trattoria Da Nennella", "restaurant", 41.8967, 12.4735, "Pranzo tipico.")
				));

		lazio.setItinerary(Arrays.asList(itineraryDay1));
		italy.setRegions(Arrays.asList(lazio));
		mockVisitedCountries.add(italy);

		CountryVisit france = new CountryVisit();
		france.setIso("FR");
		france.setName("Francia");
		france.setVisitedDates(Set.of("2022-03-20"));
		france.setCoord(new Coords());
		france.getCoord().setLat(48.8566);
		france.getCoord().setLng(2.3522);

		RegionVisit paris = new RegionVisit();
		paris.setId("FR-IDF");
		paris.setName("Île-de-France");
		paris.setCoord(new Coords());
		paris.getCoord().setLat(48.8566);
		paris.getCoord().setLng(2.3522);

		france.setRegions(Arrays.asList(paris));
		mockVisitedCountries.add(france);

		return ResponseEntity.ok(mockVisitedCountries);
	}

	private PointOfInterest createPoint(String name, String type, double lat, double lng, String description) {
		PointOfInterest point = new PointOfInterest();
		point.setName(name);
		point.setType(type);
		point.setDescription(description);
		point.setCoord(new Coords());
		point.getCoord().setLat(lat);
		point.getCoord().setLng(lng);
		return point;
	}

	private TravelEty convertToDocument(TravelDTO dto) {
		TravelEty travel = new TravelEty();
		travel.setTravelName(dto.getTravelName());

		List<DailyItinerary> itineraryDocuments = dto.getItinerary().stream()
				.map(dayDTO -> {
					DailyItinerary day = new DailyItinerary();
					day.setDay(dayDTO.getDay());
					day.setDate(dayDTO.getDate());

					List<Point> pointDocuments = dayDTO.getPoints().stream()
							.map(pointDTO -> {
								Point point = new Point();
								point.setName(pointDTO.getName());
								point.setType(pointDTO.getType());
								point.setDescription(pointDTO.getDescription());
								point.setCost(pointDTO.getCost());
								point.setLat(pointDTO.getCoord().getLat());
								point.setLng(pointDTO.getCoord().getLng());
								point.setCountry(pointDTO.getCountry());
								point.setRegion(pointDTO.getRegion());
								point.setCity(pointDTO.getCity());
								return point;
							}).collect(Collectors.toList());

					day.setPointss(pointDocuments);
					return day;
				}).collect(Collectors.toList());

		travel.setItinerary(itineraryDocuments);
		return travel;
	}

	private TravelDTO convertToDTO(TravelEty travel) {
		TravelDTO dto = new TravelDTO();
		dto.setTravelName(travel.getTravelName());

		List<DailyItineraryDTO> dayDTOs = travel.getItinerary().stream()
				.map(day -> {
					DailyItineraryDTO dayDTO = new DailyItineraryDTO();
					dayDTO.setDay(day.getDay());
					dayDTO.setDate(day.getDate());

					List<PointDTO> pointDTOs = day.getPointss().stream()
							.map(point -> {
								PointDTO pointDTO = new PointDTO();
								pointDTO.setName(point.getName());
								pointDTO.setType(point.getType());
								pointDTO.setDescription(point.getDescription());
								pointDTO.setCost(point.getCost());
								Coords coords = new Coords();
								coords.setLat(point.getLat());
								coords.setLng(point.getLng());
								pointDTO.setCoord(coords);
								pointDTO.setCountry(point.getCountry());
								pointDTO.setRegion(point.getRegion());
								pointDTO.setCity(point.getCity());
								return pointDTO;
							}).collect(Collectors.toList());

					dayDTO.setPoints(pointDTOs);
					return dayDTO;
				}).collect(Collectors.toList());

		dto.setItinerary(dayDTOs);
		return dto;
	}
}