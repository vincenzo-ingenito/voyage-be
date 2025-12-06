package it.voyage.ms.controller.impl;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

import it.voyage.ms.controller.IFriendCtl;
import it.voyage.ms.dto.response.CountryVisit;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.SearchRequest;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFriendshipService;
import it.voyage.ms.service.ITravelService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class FriendCtl implements IFriendCtl {

	@Autowired
	private IFriendshipService friendshipService;
	
	@Autowired
	private ITravelService travelService;


	@Override
	public ResponseEntity<List<UserDto>> getAcceptedFriends(CustomUserDetails user) {
		log.info("Called get accepted friend ep");
		List<UserDto> output = friendshipService.getAcceptedFriendsList(user.getUserId());
		return ResponseEntity.ok(output);
	}


	@Override
	public ResponseEntity<List<FriendRelationshipDto>> getPendingRequests(CustomUserDetails user) {
		log.info("Called get pending requests ep");
		List<FriendRelationshipDto> pendingRequests = friendshipService.getPendingRequests(user.getUserId());
		return ResponseEntity.ok(pendingRequests);
	}

	@Override
	public ResponseEntity<List<UserSearchResult>> searchUsers(SearchRequest searchRequest, CustomUserDetails userFirebase) {
		log.info("Called search users ep");
		String query = searchRequest.getQuery();

		if (query == null || query.trim().isEmpty()) {
			return ResponseEntity.ok(Collections.emptyList());
		}

		List<UserSearchResult> results = friendshipService.searchUsersAndDetermineStatus(query, userFirebase.getUserId());

		return ResponseEntity.ok(results);
	}

	@Override
	public ResponseEntity<List<CountryVisit>> getVisitedCountries(String friendId, CustomUserDetails user) {
		log.info("Called get visited countries");
		if (!friendId.equals(user.getUserId())) {
			boolean areFriends = friendshipService.checkIfUserAreFriends(user.getUserId(), friendId);
			if (!areFriends) {
				throw new AccessDeniedException("Accesso ai dati di viaggio negato. Non siete amici.");
			}
		}
		List<CountryVisit> countryVisits = travelService.getConsolidatedCountryVisits(friendId);
		return ResponseEntity.ok(countryVisits);
	}
	 
}