package it.voyage.ms.controller.impl;

import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.dto.response.FriendRequestDto;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.repository.entity.FriendRelationship;
import it.voyage.ms.repository.impl.IFriendRelationshipRepository;


@RestController
@RequestMapping("/api/friendrelationship")
public class FriendRelationshipCtl {

	@Autowired
	private IFriendRelationshipRepository friendRelationshipRepository;


	@PostMapping("/send-request")
	public ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto, @AuthenticationPrincipal FirebaseToken userFirebase) {
		String receiverId = friendRequestDto.getReceiverId();

		if (userFirebase.getUid().equals(receiverId)) {
			return ResponseEntity.badRequest().body("Non puoi inviare una richiesta di amicizia a te stesso.");
		}

		Optional<FriendRelationship> existingRelationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(userFirebase.getUid(), receiverId, userFirebase.getUid(), receiverId);
		if (!existingRelationships.isEmpty()) {
			return ResponseEntity.status(409).body("Una richiesta di amicizia con questo utente esiste già.");
		}

		try {
			FriendRelationship newRequest = new FriendRelationship();
			newRequest.setRequesterId(userFirebase.getUid());
			newRequest.setReceiverId(receiverId);
			newRequest.setStatus(FriendRelationshipStatusEnum.PENDING_REQUEST_SENT.name());
			newRequest.setCreatedAt(new Date());

			friendRelationshipRepository.save(newRequest);
			return ResponseEntity.ok("Richiesta di amicizia inviata con successo.");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Errore nell'invio della richiesta: " + e.getMessage());
		}
	}

	@PutMapping("/{requesterId}/{action}")
	public ResponseEntity<String> handleFriendRequest(@PathVariable String requesterId, @PathVariable String action, @AuthenticationPrincipal FirebaseToken user) {

		String receiverId = user.getUid();

		if ("accept".equalsIgnoreCase(action)) {
			friendRelationshipRepository.updateRequestStatus(requesterId, receiverId, FriendRelationshipStatusEnum.ACCEPTED.name());
			return ResponseEntity.ok("Richiesta di amicizia accettata.");
		} else if ("decline".equalsIgnoreCase(action)) {
			friendRelationshipRepository.updateRequestStatus(requesterId, receiverId, FriendRelationshipStatusEnum.DECLINED.name());
			return ResponseEntity.ok("Richiesta di amicizia rifiutata.");
		} else {
			return ResponseEntity.badRequest()
					.body("Azione non valida. Usa 'accept' o 'decline'.");
		}

	}

	@DeleteMapping("/remove/{friendId}")
	public ResponseEntity<String> deleteFriend(@PathVariable String friendId, @AuthenticationPrincipal FirebaseToken user) {
	    try {
	        friendRelationshipRepository.deleteFriendship(user.getUid(), friendId);
	        return ResponseEntity.ok("Amico rimosso con successo.");
	    } catch (Exception e) {
	        return new ResponseEntity<>("Errore durante la rimozione dell'amico.", HttpStatus.INTERNAL_SERVER_ERROR);
	    }
	}


}