package it.voyage.ms.service;

import java.util.List;

import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;

public interface IFriendshipService {
	
	List<UserDto> getAcceptedFriendsList(String currentUserId);
	
	List<FriendRelationshipDto> getPendingRequests(String receiverId);
	
	List<UserSearchResult> searchUsersAndDetermineStatus(String query, String currentUserId);
	
	boolean checkIfUserAreFriends(String userId, String friendId);
}