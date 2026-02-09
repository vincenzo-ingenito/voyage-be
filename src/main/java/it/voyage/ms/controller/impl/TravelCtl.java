package it.voyage.ms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import it.voyage.ms.controller.ITravelCtl;
import it.voyage.ms.dto.response.TravelDTO;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFriendshipService;
import it.voyage.ms.service.ITravelService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class TravelCtl implements ITravelCtl {

	@Autowired
	private ITravelService travelService;
	
	@Autowired
	private IFriendshipService friendshipService;

	@Override
	public ResponseEntity<TravelDTO> saveTravel(TravelDTO travelData, List<MultipartFile> files, CustomUserDetails userDetails) {
		log.info("Called save travel ep");
		
		if (travelData == null) {
			log.error("Nessun dato viaggio fornito");
			return ResponseEntity.badRequest().build();
		}
		
		TravelDTO savedEntity = travelService.saveTravel(travelData, files, userDetails);
		return ResponseEntity.ok(savedEntity); 
	}
	
	@Override
	public ResponseEntity<Void> deleteTravelById(Long travelId, CustomUserDetails userDetails) {
	    log.info("Called delete travel by id ep");
	    Boolean deleted = travelService.deleteTravelById(travelId, userDetails.getUserId()); 
	    
	    if (deleted) {
	        return ResponseEntity.noContent().build();
	    } else {
	        return ResponseEntity.notFound().build();
	    }
	}
	
	@Override
	public ResponseEntity<List<TravelDTO>> getTravels(CustomUserDetails userDetails) {
		log.info("Called get travels ep");
		List<TravelDTO> travels = travelService.getTravelsForUser(userDetails.getUserId());
		return ResponseEntity.ok(travels);
	}
	
	@Override
	public ResponseEntity<List<TravelDTO>> getFriendTravels(String friendId, CustomUserDetails userDetails) {
		log.info("Called get friend travels ep for friendId: {}", friendId);
		
		// Se l'utente richiede i propri viaggi, reindirizza al metodo standard
		if (friendId.equals(userDetails.getUserId())) {
			log.info("User requesting own travels, redirecting to getTravels");
			return getTravels(userDetails);
		}
		
		// Verifica che ci sia amicizia tra gli utenti
		boolean areFriends = friendshipService.checkIfUserAreFriends(userDetails.getUserId(), friendId);
		if (!areFriends) {
			log.warn("User {} is not friends with {}", userDetails.getUserId(), friendId);
			throw new org.springframework.security.access.AccessDeniedException("Non sei amico di questo utente");
		}
		
		// Recupera i viaggi dell'amico
		List<TravelDTO> friendTravels = travelService.getTravelsForUser(friendId);
		log.info("Retrieved {} travels for friend {}", friendTravels.size(), friendId);
		
		return ResponseEntity.ok(friendTravels);
	}
	
	@Override
	public ResponseEntity<TravelDTO> updateTravel(@PathVariable String travelId, @RequestPart("travelData") TravelDTO travelData, @RequestPart(value = "files", required = false) List<MultipartFile> files, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called update travel ep");
		TravelDTO updatedTravel = travelService.updateExistingTravel(userDetails.getUserId(), travelId, travelData, files);
		return ResponseEntity.ok(updatedTravel); 
	}
	
	@Override
	public ResponseEntity<TravelDTO> getTravelWithUrls(@PathVariable Long travelId, String userId, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called get travel with URLs ep for travelId: {}, userId: {}", travelId, userId);
		// Use the provided userId if present (for viewing friend's travel), otherwise use authenticated user
		String targetUserId = (userId != null && !userId.isEmpty()) ? userId : userDetails.getUserId();
		TravelDTO travel = travelService.getTravelWithUrls(targetUserId, travelId);
		return ResponseEntity.ok(travel);
	}

	@Override
	public ResponseEntity<TravelDTO> confirmTravelDates(@PathVariable String travelId, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called confirm travel dates ep for travelId: {}", travelId);
		TravelDTO confirmedTravel = travelService.confirmTravelDates(userDetails.getUserId(), travelId);
		return ResponseEntity.ok(confirmedTravel);
	}

	@Override
	public ResponseEntity<TravelDTO> deleteMemoryPhoto(@PathVariable String travelId, @PathVariable int dayNumber, @AuthenticationPrincipal CustomUserDetails userDetails) {
		log.info("Called delete memory photo ep for travelId: {}, day: {}", travelId, dayNumber);
		TravelDTO updatedTravel = travelService.deleteMemoryPhoto(userDetails.getUserId(), travelId, dayNumber);
		return ResponseEntity.ok(updatedTravel);
	}
}
