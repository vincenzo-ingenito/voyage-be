package it.voyage.ms.controller.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
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
import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.IFriendRelationshipRepository;
import it.voyage.ms.repository.impl.UserRepository;


@RestController
@RequestMapping("/api/friendrelationship")
public class FriendRelationshipCtl {

	@Autowired
	private IFriendRelationshipRepository friendRelationshipRepository;

	@Autowired
	private UserRepository userRepository;


	@PostMapping("/send-request")
	public ResponseEntity<String> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto, @AuthenticationPrincipal FirebaseToken userFirebase) {
		String receiverId = friendRequestDto.getReceiverId();

		if (userFirebase.getUid().equals(receiverId)) {
			return ResponseEntity.badRequest().body("Non puoi inviare una richiesta di amicizia a te stesso.");
		}

		Optional<FriendRelationshipEty> existingRelationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(userFirebase.getUid(), receiverId, userFirebase.getUid(), receiverId);
		if (!existingRelationships.isEmpty()) {
			return ResponseEntity.status(409).body("Una richiesta di amicizia con questo utente esiste già.");
		}

		Optional<UserEty> receiverUser = userRepository.findById(receiverId);
		if (receiverUser.isEmpty()) {
			return ResponseEntity.status(404).body("Utente destinatario non trovato.");
		}


		FriendRelationshipEty newRequest = new FriendRelationshipEty();
		newRequest.setRequesterId(userFirebase.getUid());
		newRequest.setReceiverId(receiverId);
		newRequest.setCreatedAt(new Date());

		if (receiverUser.get().isPrivate()) {
			newRequest.setStatus(FriendRelationshipStatusEnum.PENDING.name());
			friendRelationshipRepository.save(newRequest);
			return ResponseEntity.ok("Richiesta di amicizia inviata con successo.");
		} else {
			newRequest.setStatus(FriendRelationshipStatusEnum.ACCEPTED.name());
			friendRelationshipRepository.save(newRequest);
			return ResponseEntity.ok("Amico aggiunto con successo! Il profilo è pubblico.");
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
		friendRelationshipRepository.deleteFriendship(user.getUid(), friendId);
		return ResponseEntity.ok("Amico rimosso con successo.");
	}
	
	
	
	
	
	
//TODO - Sembra che non venga usato
//
//	@PostMapping("/unblock")
//	public ResponseEntity<?> unblockUser(@RequestBody String userIdToUnblock, @AuthenticationPrincipal FirebaseToken userFirebase) {
//
//		if (userIdToUnblock == null || userIdToUnblock.trim().isEmpty()) {
//			return ResponseEntity.badRequest().build();
//		}
//
//		// Trova la relazione di blocco
//		List<FriendRelationshipEty> relationships = friendRelationshipRepository.findByRequesterIdAndReceiverIdAndStatus(userFirebase.getUid(), userIdToUnblock, FriendRelationshipStatusEnum.BLOCKED.name());
//
//		if (relationships.isEmpty()) {
//			return ResponseEntity.notFound().build();
//		}
//
//		// Elimina la relazione di blocco
//		friendRelationshipRepository.delete(relationships.get(0));
//
//		return ResponseEntity.ok().build();
//	}
 


}