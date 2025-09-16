package it.voyage.ms.controller.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.Coords;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.DailyItinerary;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.FriendRequestDto;
import it.voyage.ms.dto.response.PointOfInterest;
import it.voyage.ms.dto.response.RegionVisit;
import it.voyage.ms.dto.response.SearchRequest;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.FriendStatus;
import it.voyage.ms.repository.entity.FriendRelationship;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.FriendRelationshipRepository;
import it.voyage.ms.repository.impl.UserRepository;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/friends")
public class FriendCtl {

	@Autowired
	private FriendRelationshipRepository friendRelationshipRepository;

	@Autowired
	private UserRepository userRepository;

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

	/**
	 * Estrae l'ID utente dal token di autenticazione Firebase.
	 * @param request La richiesta HTTP contenente l'header di autorizzazione.
	 * @return L'ID utente o null se il token non è valido o mancante.
	 */
	private String getUserIdFromToken(HttpServletRequest request) {
		String authHeader = request.getHeader("Authorization");
		if (authHeader != null && authHeader.startsWith("Bearer ")) {
			String idToken = authHeader.substring(7);
			try {
				FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
				return decodedToken.getUid();
			} catch (FirebaseAuthException e) {
				// Il token non è valido o scaduto
				return null;
			}
		}
		return null;
	}

	@PostMapping("/add")
	public ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDTO requestDTO, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		if (currentUserId == null) {
			return new ResponseEntity<>("Token di autenticazione non valido.", HttpStatus.UNAUTHORIZED);
		}

		Optional<FriendRelationship> existingRelationship = friendRelationshipRepository
				.findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(
						currentUserId, requestDTO.getFriendId(),
						requestDTO.getFriendId(), currentUserId
						);

		if (existingRelationship.isPresent()) {
			return ResponseEntity.badRequest().body("La richiesta di amicizia o la relazione esiste già.");
		}

		FriendRelationship newRelationship = new FriendRelationship();
		newRelationship.setRequesterId(currentUserId);
		newRelationship.setReceiverId(requestDTO.getFriendId());
		newRelationship.setStatus(FriendshipStatus.PENDING.name());
		newRelationship.setCreatedAt(new Date());

		friendRelationshipRepository.save(newRelationship);
		return ResponseEntity.ok("Richiesta di amicizia inviata con successo.");
	}
	
	@PutMapping("/{requesterId}/{action}")
    public ResponseEntity<?> handleFriendRequest(@PathVariable String requesterId, @PathVariable String action, HttpServletRequest request) {
        try {
        	String currentUserId = getUserIdFromToken(request);
    		if (currentUserId == null) {
    			return new ResponseEntity<>("Token di autenticazione non valido.", HttpStatus.UNAUTHORIZED);
    		}

            if ("accept".equals(action)) {
            	friendRelationshipRepository.updateRequestStatus(requesterId, currentUserId, "ACCEPTED");
            } else if ("decline".equals(action)) {
            	
            }
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to handle friend request. " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
        return null;
    }

	@PostMapping("/accept")
	public ResponseEntity<String> acceptFriendRequest(@RequestBody FriendRequestDTO requestDTO, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		if (currentUserId == null) {
			return new ResponseEntity<>("Token di autenticazione non valido.", HttpStatus.UNAUTHORIZED);
		}

		Optional<FriendRelationship> relationship = friendRelationshipRepository
				.findByRequesterIdAndReceiverId(requestDTO.getFriendId(), currentUserId);

		if (relationship.isPresent() && relationship.get().getStatus().equals(FriendshipStatus.PENDING.name())) {
			FriendRelationship rel = relationship.get();
			rel.setStatus(FriendshipStatus.ACCEPTED.name());
			friendRelationshipRepository.save(rel);
			return ResponseEntity.ok("Richiesta di amicizia accettata.");
		}

		return ResponseEntity.badRequest().body("Richiesta di amicizia non trovata o non in stato di attesa.");
	}

	@GetMapping("/accepted")
	public ResponseEntity<List<UserEty>> getAcceptedFriends(HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		if (currentUserId == null) {
			return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
		}

		List<FriendRelationship> relationships = friendRelationshipRepository.findByRequesterIdAndStatusOrReceiverIdAndStatus(
				currentUserId, FriendshipStatus.ACCEPTED.name(),
				currentUserId, FriendshipStatus.ACCEPTED.name()
				);

		List<String> friendIds = relationships.stream()
				.map(rel -> rel.getRequesterId().equals(currentUserId) ? rel.getReceiverId() : rel.getRequesterId())
				.collect(Collectors.toList());

		List<UserEty> friends = userRepository.findAllById(friendIds);
		return ResponseEntity.ok(friends);
	}

	@GetMapping("/requests/pending")
	public ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		List<FriendRelationship> pendingRequests = friendRelationshipRepository.findByReceiverIdAndStatus(currentUserId, "PENDING");
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

	@DeleteMapping("/remove/{friendId}")
	public ResponseEntity<String> deleteFriend(@PathVariable String friendId, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		if (currentUserId == null) {
			return new ResponseEntity<>("Token di autenticazione non valido.", HttpStatus.UNAUTHORIZED);
		}

		try {
			friendRelationshipRepository.deleteByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(
					currentUserId, friendId, friendId, currentUserId
					);
			return ResponseEntity.ok("Amico rimosso con successo.");
		} catch (Exception e) {
			return new ResponseEntity<>("Errore durante la rimozione dell'amico.", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping("/search")
	public ResponseEntity<List<UserSearchResult>> searchUsers(@RequestBody SearchRequest searchRequest, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		String query = searchRequest.getQuery();

		if (currentUserId == null || query == null || query.trim().isEmpty()) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		// Trova tutti gli ID degli utenti che hanno bloccato l'utente corrente
		List<String> blockedByUserIds = friendRelationshipRepository.findByReceiverIdAndStatus(currentUserId, "BLOCKED").stream()
				.map(FriendRelationship::getRequesterId)
				.collect(Collectors.toList());

		// Trova tutti gli utenti che corrispondono alla query, escludendo l'utente corrente e quelli che lo hanno bloccato
		List<UserEty> users = userRepository.findByNameRegex(query).stream()
				.filter(user -> !user.getId().equals(currentUserId))
				.filter(user -> !blockedByUserIds.contains(user.getId()))
				.collect(Collectors.toList());

		// Per ogni utente trovato, determina lo stato della relazione con l'utente corrente
		List<UserSearchResult> results = users.stream()
				.map(user -> {
					FriendStatus status;

					// CONTROLLO AGGIUNTO: Se l'utente corrente ha bloccato la persona trovata, imposta lo stato a BLOCKED.
					if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(currentUserId, user.getId(), "BLOCKED").size() > 0) {
						status = FriendStatus.BLOCKED;
					}
					// Controlla se sono già amici
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(currentUserId, user.getId(), "ACCEPTED").size() > 0 ||
							friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(user.getId(), currentUserId, "ACCEPTED").size() > 0) {
						status = FriendStatus.ALREADY_FRIENDS;
					} 
					// Controlla se c'è una richiesta in sospeso inviata dall'utente corrente
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(currentUserId, user.getId(), "PENDING").size() > 0) {
						status = FriendStatus.PENDING_REQUEST_SENT;
					}
					// Controlla se c'è una richiesta in sospeso ricevuta dall'utente corrente
					else if (friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(user.getId(), currentUserId, "PENDING").size() > 0) {
						status = FriendStatus.PENDING_REQUEST_RECEIVED;
					} 
					// Se non ci sono relazioni esistenti, l'utente è disponibile
					else {
						status = FriendStatus.AVAILABLE;
					}

					return new UserSearchResult(user.getId(), user.getName(), user.getAvatar(), status);
				})
				.collect(Collectors.toList());

		return ResponseEntity.ok(results);
	}

	// Endpoint per inviare una richiesta di amicizia
	@PostMapping("/send-request")
	public ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);
		String receiverId = friendRequestDto.getReceiverId();

		// Controllo per evitare che un utente invii una richiesta a se stesso
		if (currentUserId.equals(receiverId)) {
			return ResponseEntity.badRequest().body("Non puoi inviare una richiesta di amicizia a te stesso.");
		}

		// Verifica se una relazione (pending o accepted) esiste già tra i due utenti
		Optional<FriendRelationship> existingRelationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(currentUserId, receiverId, currentUserId, receiverId);
		if (!existingRelationships.isEmpty()) {
			return ResponseEntity.status(409).body("Una richiesta di amicizia con questo utente esiste già.");
		}

		try {
			FriendRelationship newRequest = new FriendRelationship();
			newRequest.setRequesterId(currentUserId);
			newRequest.setReceiverId(receiverId);
			newRequest.setStatus("PENDING");
			newRequest.setCreatedAt(new Date());

			friendRelationshipRepository.save(newRequest);
			return ResponseEntity.ok("Richiesta di amicizia inviata con successo.");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Errore nell'invio della richiesta: " + e.getMessage());
		}
	}

	@PostMapping("/unblock")
	public ResponseEntity<?> unblockUser(@RequestBody String userIdToUnblock, HttpServletRequest request) {
		String currentUserId = getUserIdFromToken(request);

		if (currentUserId == null || userIdToUnblock == null || userIdToUnblock.trim().isEmpty()) {
			return ResponseEntity.badRequest().build();
		}

		// Trova la relazione di blocco
		List<FriendRelationship> relationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(currentUserId, userIdToUnblock, "BLOCKED");

		if (relationships.isEmpty()) {
			return ResponseEntity.notFound().build();
		}

		// Elimina la relazione di blocco
		friendRelationshipRepository.delete(relationships.get(0));

		return ResponseEntity.ok().build();
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
}