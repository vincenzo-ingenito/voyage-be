package it.voyage.ms.controller.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.google.firebase.auth.FirebaseToken;

import it.voyage.ms.controller.IFriendRelationshipCtl;
import it.voyage.ms.dto.response.FriendRequestDto;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFriendshipService;


@RestController
public class FriendRelationshipCtl implements IFriendRelationshipCtl {

	@Autowired
	private IFriendshipService friendshipService;

	@Override
	public ResponseEntity<String> sendFriendRequest(FriendRequestDto friendRequestDto, CustomUserDetails userFirebase) {
		String requesterId = userFirebase.getUserId();
		String receiverId = friendRequestDto.getReceiverId();
		
		if (requesterId.equals(receiverId)) {
			throw new IllegalArgumentException("Non puoi inviare una richiesta di amicizia a te stesso.");
		}
		String message = friendshipService.sendFriendRequest(requesterId, receiverId);
		return ResponseEntity.ok(message);

	}

	@Override
	public ResponseEntity<String> handleFriendRequest(String requesterId, String action, CustomUserDetails user) {
		String output = friendshipService.handleFriendRequest(requesterId, user.getUserId(), action);
		return ResponseEntity.ok(output);
	}

	@DeleteMapping("/remove/{friendId}")
	public ResponseEntity<String> deleteFriend(@PathVariable String friendId, @AuthenticationPrincipal FirebaseToken user) {
		friendshipService.deleteFriendship(user.getUid(), friendId);
		return ResponseEntity.ok("Amico rimosso con successo.");
	}


}