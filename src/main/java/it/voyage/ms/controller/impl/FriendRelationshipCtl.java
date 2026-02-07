package it.voyage.ms.controller.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IFriendRelationshipCtl;
import it.voyage.ms.dto.response.BlockedUserDTO;
import it.voyage.ms.dto.response.FriendRequestDto;
import it.voyage.ms.enums.BlockActionEnum;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFriendshipService;
import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
public class FriendRelationshipCtl implements IFriendRelationshipCtl {

	@Autowired
	private IFriendshipService friendshipService;

	@Override
	public ResponseEntity<String> sendFriendRequest(FriendRequestDto friendRequestDto, CustomUserDetails userFirebase) {
		log.info("Called send friend request");
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
		log.info("Called handle friend request");
		String output = friendshipService.handleFriendRequest(requesterId, user.getUserId(), action);
		return ResponseEntity.ok(output);
	}

	@Override
	public ResponseEntity<String> deleteFriend(String friendId, CustomUserDetails user) {
		log.info("Called delete friend");
		friendshipService.deleteFriendship(user.getUserId(), friendId);
		return ResponseEntity.ok("Amico rimosso con successo.");
	}

	@Override
	public ResponseEntity<String> manageBlockStatus(String friendId, BlockActionEnum action, CustomUserDetails user) {
		log.info("Called manage block status");
		friendshipService.executeBlockAction(user.getUserId(),friendId, action);
		return ResponseEntity.ok("Successo.");

	}

	@Override
	public ResponseEntity<List<BlockedUserDTO>> getBlockedUsers(CustomUserDetails user) {
		log.info("Called get blocked users");
		return ResponseEntity.ok(friendshipService.getBlockedUsers(user.getUserId()));

	}

}