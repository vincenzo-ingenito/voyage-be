package it.voyage.ms.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.mapper.FriendrelationshipMapper;
import it.voyage.ms.mapper.UserMapper;
import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.IFriendRelationshipRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.IFriendshipService;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class FriendshipService implements IFriendshipService {

	private final UserRepository userRepository;

	private final IFriendRelationshipRepository friendRelationshipRepository;

	private final FriendrelationshipMapper friendrelationShipMapper;
	
	private final UserMapper userMapper;

	
	@Override
	public boolean checkIfUserAreFriends(String userId, String friendId) {
		return friendRelationshipRepository.findFriendshipByUsersAndStatus(userId, friendId, FriendRelationshipStatusEnum.ACCEPTED.name()).isPresent();
	}
	
	@Override
	public List<UserDto> getAcceptedFriendsList(String currentUserId) {
		List<FriendRelationshipEty> relationships = friendRelationshipRepository
				.findByRequesterIdAndStatusOrReceiverIdAndStatus(
						currentUserId,
						FriendRelationshipStatusEnum.ACCEPTED.name(),
						currentUserId,
						FriendRelationshipStatusEnum.ACCEPTED.name()
						);

		List<String> userIdsToFetch = relationships.stream()
				.map(rel -> rel.getRequesterId().equals(currentUserId) ? rel.getReceiverId() : rel.getRequesterId())
				.collect(Collectors.toList());
		userIdsToFetch.add(currentUserId);

		List<UserEty> allUsers = userRepository.findAllById(userIdsToFetch);

		Map<String, UserDto> userDtoMap = allUsers.stream()
				.collect(Collectors.toMap(
						UserEty::getId,
						f -> UserDto.fromEntityWithUid(f, currentUserId)
						));

		List<UserDto> output = new ArrayList<>();

		if (userDtoMap.containsKey(currentUserId)) {
			output.add(userDtoMap.get(currentUserId));
			userDtoMap.remove(currentUserId); 
		}
		output.addAll(userDtoMap.values()); 
		return output;
	}

	@Override
	public List<FriendRelationshipDto> getPendingRequests(String receiverId) {

		List<FriendRelationshipEty> pendingRequests = friendRelationshipRepository
				.findByReceiverIdAndStatus(receiverId, FriendRelationshipStatusEnum.PENDING.name());

		if (pendingRequests.isEmpty()) {
			return Collections.emptyList();
		}

		Set<String> requesterIds = pendingRequests.stream()
				.map(FriendRelationshipEty::getRequesterId)
				.collect(Collectors.toSet());

		List<UserEty> requesters = userRepository.findAllById(requesterIds);

		// Mappa gli utenti per un accesso rapido O(1) in memoria
		Map<String, UserEty> requesterMap = requesters.stream()
				.collect(Collectors.toMap(UserEty::getId, user -> user));

		// 3. Mappa le relazioni in DTO usando il Mapper e la mappa di utenti
		List<FriendRelationshipDto> dtos = pendingRequests.stream()
				.map(relationship -> mapAndEnrich(relationship, requesterMap))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.collect(Collectors.toList());

		return dtos;
	}

	private Optional<FriendRelationshipDto> mapAndEnrich(FriendRelationshipEty relationship, Map<String, UserEty> requesterMap){
		UserEty requester = requesterMap.get(relationship.getRequesterId());

		if (requester == null) {
			return Optional.empty(); 
		}

		FriendRelationshipDto dto = friendrelationShipMapper.mapToFriendRelationshipDto(relationship, requester.getName(), requester.getAvatar());
		return Optional.of(dto);
	}
	
	
	@Override
    public List<UserSearchResult> searchUsersAndDetermineStatus(String query, String currentUserId) {
        // 1a. Trova gli ID degli utenti che hanno bloccato l'utente corrente (Receiver=currentUserId, Status=BLOCKED)
        Set<String> blockedMeIds = friendRelationshipRepository
                .findByReceiverIdAndStatus(currentUserId, FriendRelationshipStatusEnum.BLOCKED.name()).stream()
                .map(FriendRelationshipEty::getRequesterId)
                .collect(Collectors.toSet());

        // 1b. Trova gli utenti che corrispondono alla query (Query DB per ricerca)
        List<UserEty> allMatchingUsers = userRepository.findByNameRegex(query).stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .filter(user -> !blockedMeIds.contains(user.getId()))
                .collect(Collectors.toList());

        if (allMatchingUsers.isEmpty()) {
            return Collections.emptyList();
        }

        // --- 2. Preparazione per l'Arricchimento: Bulk Status Check ---
        
        // Costruisci una lista di tutti gli ID da controllare
        List<String> userIdsToCheck = allMatchingUsers.stream()
                .map(UserEty::getId)
                .collect(Collectors.toList());

        // Recupera TUTTE le relazioni tra l'utente corrente e gli utenti trovati (Bulk Query)
        // Questa query è fondamentale per evitare le N+1 query nel mapping
        List<FriendRelationshipEty> relevantRelationships = friendRelationshipRepository
                .findAllRelevantRelationships(currentUserId, userIdsToCheck);

        // Mappa le relazioni per un accesso O(1)
        Map<String, FriendRelationshipEty> relationshipMap = buildRelationshipMap(relevantRelationships, currentUserId);

        
        // Mappa gli utenti filtrati in UserSearchResult e determina lo stato utilizzando la mappa O(1)
        List<UserSearchResult> results = allMatchingUsers.stream()
                .map(user -> mapToSearchResult(user, currentUserId, relationshipMap))
                .collect(Collectors.toList());

        return results;
    }
    
    /**
     * Helper per costruire una mappa delle relazioni chiave-valore per un accesso rapido O(1).
     * La chiave è l'ID dell'altro utente.
     */
    private Map<String, FriendRelationshipEty> buildRelationshipMap(List<FriendRelationshipEty> relationships, String currentUserId) {
        return relationships.stream()
                .collect(Collectors.toMap(
                    rel -> rel.getRequesterId().equals(currentUserId) ? rel.getReceiverId() : rel.getRequesterId(),
                    rel -> rel,
                    (existing, replacement) -> existing
                ));
    }

    /**
     * Determina lo stato e mappa l'utente in UserSearchResult.
     */
    private UserSearchResult mapToSearchResult(UserEty user, String currentUserId, Map<String, FriendRelationshipEty> relationshipMap) {
        
        FriendRelationshipEty relationship = relationshipMap.get(user.getId());
        FriendRelationshipStatusEnum status = FriendRelationshipStatusEnum.AVAILABLE; // Default
        
        if (relationship != null) {
            String relationshipStatus = relationship.getStatus();
            
            if (FriendRelationshipStatusEnum.BLOCKED.name().equals(relationshipStatus)) {
                // L'utente corrente HA bloccato l'utente trovato
                status = FriendRelationshipStatusEnum.BLOCKED;
            } else if (FriendRelationshipStatusEnum.ACCEPTED.name().equals(relationshipStatus)) {
                status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
            } else if (FriendRelationshipStatusEnum.PENDING.name().equals(relationshipStatus)) {
                // Determina se la richiesta è stata inviata DA te o A te
                if (relationship.getRequesterId().equals(currentUserId)) {
                    status = FriendRelationshipStatusEnum.PENDING_REQUEST_SENT;
                } else {
                    status = FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED;
                }
            }
        }
        
        // Mappa i campi base dell'utente e aggiunge lo stato
        return userMapper.mapToSearchResult(user, status); 
    }
	
}
