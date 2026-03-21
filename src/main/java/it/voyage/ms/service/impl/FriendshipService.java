package it.voyage.ms.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import it.voyage.ms.dto.response.BlockedUserDTO;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.enums.BlockActionEnum;
import it.voyage.ms.enums.FriendRelationshipStatusEnum;
import it.voyage.ms.exceptions.ConflictException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.mapper.FriendrelationshipMapper;
import it.voyage.ms.mapper.UserMapper;
import it.voyage.ms.repository.entity.FriendRelationshipEty;
import it.voyage.ms.repository.entity.FriendRelationshipEty.Status;
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
        return friendRelationshipRepository.findFriendshipByUsersAndStatus(userId, friendId, Status.ACCEPTED).isPresent();
    }

    @Override
    public List<UserDto> getAcceptedFriendsList(String currentUserId) {
        List<UserEty> users = userRepository
            .findAcceptedFriendsAndSelf(currentUserId, Status.ACCEPTED);

        List<UserDto> output = new ArrayList<>();
        List<UserDto> others = new ArrayList<>();

        for (UserEty u : users) {
            UserDto dto = UserDto.fromEntityWithUid(u, currentUserId);
            if (u.getId().equals(currentUserId)) {
                output.add(0, dto);
            } else {
                others.add(dto);
            }
        }

        output.addAll(others);
        return output;
    }

    @Override
    public List<FriendRelationshipDto> getPendingRequests(String receiverId) {
        List<FriendRelationshipEty> pendingRequests = friendRelationshipRepository
            .findPendingRequestsWithRequester(receiverId, Status.PENDING);

        return pendingRequests.stream()
            .filter(r -> r.getRequester() != null)
            .map(r -> friendrelationShipMapper.mapToFriendRelationshipDto(
                r, r.getRequester().getName(), r.getRequester().getAvatar()))
            .collect(Collectors.toList());
    }

    @Override
    public List<UserSearchResult> searchUsersAndDetermineStatus(String query, String currentUserId) {
        // Utenti che hanno bloccato il currentUser
        Set<String> blockedMeIds = friendRelationshipRepository
            .findByReceiverIdAndStatus(currentUserId, Status.BLOCKED).stream()
            .map(FriendRelationshipEty::getRequesterId)
            .collect(Collectors.toSet());

        List<UserEty> allMatchingUsers = userRepository.findByNameRegex(query).stream()
            .filter(user -> !user.getId().equals(currentUserId))
            .filter(user -> !blockedMeIds.contains(user.getId()))
            .collect(Collectors.toList());

        if (allMatchingUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userIdsToCheck = allMatchingUsers.stream()
            .map(UserEty::getId)
            .collect(Collectors.toList());

        List<FriendRelationshipEty> relevantRelationships = friendRelationshipRepository
            .findAllRelevantRelationships(currentUserId, userIdsToCheck);

        Map<String, FriendRelationshipEty> relationshipMap =
            buildRelationshipMap(relevantRelationships, currentUserId);

        return allMatchingUsers.stream()
            .map(user -> mapToSearchResult(user, currentUserId, relationshipMap))
            .collect(Collectors.toList());
    }

    private Map<String, FriendRelationshipEty> buildRelationshipMap(
            List<FriendRelationshipEty> relationships, String currentUserId) {
        return relationships.stream()
            .collect(Collectors.toMap(
                rel -> rel.getRequesterId().equals(currentUserId)
                    ? rel.getReceiverId()
                    : rel.getRequesterId(),
                rel -> rel,
                (existing, replacement) -> existing
            ));
    }

    private UserSearchResult mapToSearchResult(UserEty user, String currentUserId,
            Map<String, FriendRelationshipEty> relationshipMap) {

        FriendRelationshipEty relationship = relationshipMap.get(user.getId());
        FriendRelationshipStatusEnum status = FriendRelationshipStatusEnum.AVAILABLE;

        if (relationship != null) {
            // relationship.getStatus() è ora FriendRelationshipEty.Status (enum)
            switch (relationship.getStatus()) {
                case BLOCKED:
                    status = FriendRelationshipStatusEnum.BLOCKED;
                    break;
                case ACCEPTED:
                    status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
                    break;
                case PENDING:
                    status = relationship.getRequesterId().equals(currentUserId)
                        ? FriendRelationshipStatusEnum.PENDING_REQUEST_SENT
                        : FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED;
                    break;
                default:
                    break;
            }
        }

        return userMapper.mapToSearchResult(user, status);
    }

    @Override
    public String sendFriendRequest(String requesterId, String receiverId) {
        UserEty receiverUser = userRepository.findById(receiverId)
            .orElseThrow(() -> new NotFoundException("Utente destinatario non trovato."));

        if (friendRelationshipRepository
                .existsByRequesterIdAndReceiverIdOrReceiverIdAndRequesterId(
                    requesterId, receiverId, requesterId, receiverId)) {
            throw new ConflictException(
                "Una richiesta o una relazione di amicizia con questo utente esiste già.");
        }

        FriendRelationshipEty newRequest = new FriendRelationshipEty();
        newRequest.setRequesterId(requesterId);
        newRequest.setReceiverId(receiverId);
        // setStatus ora accetta FriendRelationshipEty.Status (enum)
        newRequest.setStatus(receiverUser.isPrivate() ? Status.PENDING : Status.ACCEPTED);

        friendRelationshipRepository.save(newRequest);

        return receiverUser.isPrivate()
            ? "Richiesta di amicizia inviata con successo."
            : "Amico aggiunto con successo! Il profilo è pubblico.";
    }

    @Override
    public String handleFriendRequest(String requesterId, String receiverId, String action) {
        int updatedCount;
        String successMessage;

        if ("accept".equalsIgnoreCase(action)) {
            updatedCount = friendRelationshipRepository
                .updateRequestStatus(requesterId, receiverId, Status.ACCEPTED);
            successMessage = "Richiesta di amicizia accettata.";
        } else if ("decline".equalsIgnoreCase(action)) {
            updatedCount = (int) friendRelationshipRepository
                .deleteFriendship(receiverId, requesterId);
            successMessage = "Richiesta di amicizia rifiutata.";
        } else {
            throw new IllegalArgumentException("Azione non valida. Usa 'accept' o 'decline'.");
        }

        if (updatedCount == 0) {
            throw new NotFoundException("Richiesta di amicizia in sospeso non trovata.");
        }

        return successMessage;
    }

    @Override
    public void deleteFriendship(String requesterId, String friendId) {
        friendRelationshipRepository.deleteFriendship(requesterId, friendId);
    }

    @Override
    public void executeBlockAction(String currentUserId, String friendId, BlockActionEnum action) {
        if (action == BlockActionEnum.BLOCK) {
            blockUser(currentUserId, friendId, currentUserId);
        } else if (action == BlockActionEnum.UNBLOCK) {
            unblockUser(currentUserId, friendId, currentUserId);
        } else {
            throw new IllegalArgumentException("Azione di blocco non valida: " + action);
        }
    }

    private void blockUser(String currentUserId, String userToBlockId, String blockerId) {
        friendRelationshipRepository.updateRelationshipStatus(
            currentUserId, userToBlockId, Status.BLOCKED, blockerId);
    }

    private void unblockUser(String currentUserId, String userToUnblockId, String blockerId) {
        friendRelationshipRepository.deleteRelationship(
            currentUserId, userToUnblockId, blockerId);
    }

    @Override
    public List<BlockedUserDTO> getBlockedUsers(String currentUserId) {
        List<FriendRelationshipEty> etys =
            friendRelationshipRepository.findMyBlockedRelationships(currentUserId);
        return etys.stream()
            .map(e -> getDtoFromEty(e, currentUserId))
            .collect(Collectors.toList());
    }

    private BlockedUserDTO getDtoFromEty(FriendRelationshipEty ety, String currentUser) {
        String userToSearch = ety.getReceiverId().equals(currentUser)
            ? ety.getRequesterId()
            : ety.getReceiverId();

        UserEty userEty = userRepository.findById(userToSearch)
            .orElseThrow(() -> new NotFoundException("Utente bloccato non trovato: " + userToSearch));

        BlockedUserDTO out = new BlockedUserDTO();
        out.setId(userEty.getId());
        out.setAvatar(userEty.getAvatar());
        out.setName(userEty.getName());
        return out;
    }
}