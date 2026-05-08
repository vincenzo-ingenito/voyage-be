package it.voyage.ms.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.response.BlockedUserDTO;
import it.voyage.ms.dto.response.FriendRelationshipDto;
import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.dto.response.UserSearchResult;
import it.voyage.ms.dto.response.UserSuggestionDTO;
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
import it.voyage.ms.service.INotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 
@Service
@AllArgsConstructor
@Slf4j
public class FriendshipService implements IFriendshipService {

    private final UserRepository userRepository;
    private final IFriendRelationshipRepository friendRelationshipRepository;
    private final FriendrelationshipMapper friendrelationShipMapper;
    private final UserMapper userMapper;
    private final INotificationService notificationService;
 
    /**
     * True se esiste almeno una relazione ACCEPTED in qualsiasi direzione
     * (= "si conoscono"). Per la visibilità dei contenuti usa isFollowing().
     */
    @Override
    public boolean checkIfUserAreFriends(String userId, String friendId) {
        return isFollowing(userId, friendId) || isFollowing(friendId, userId);
    }

    /** X "vede" Y ⟺ esiste record X→Y con status=ACCEPTED. */
    private boolean isFollowing(String followerId, String followedId) {
        return friendRelationshipRepository
                .findByRequesterIdAndReceiverId(followerId, followedId)
                .map(r -> r.getStatus() == Status.ACCEPTED)
                .orElse(false);
    }

    @Override
    public List<UserDto> getAcceptedFriendsList(String currentUserId) {
        List<UserEty> users = userRepository.findAcceptedFriendsAndSelf(currentUserId, Status.ACCEPTED);
        List<UserDto> result = new ArrayList<>();
        List<UserDto> others = new ArrayList<>();
        for (UserEty u : users) {
            UserDto dto = UserDto.fromEntityWithUid(u, currentUserId);
            if (u.getId().equals(currentUserId)) result.add(0, dto);
            else others.add(dto);
        }
        result.addAll(others);
        return result;
    }

    @Override
    public List<FriendRelationshipDto> getPendingRequests(String receiverId) {
        return friendRelationshipRepository
                .findPendingRequestsWithRequester(receiverId, Status.PENDING).stream()
                .filter(r -> r.getRequester() != null)
                .map(r -> friendrelationShipMapper.mapToFriendRelationshipDto(
                        r, r.getRequester().getName(), r.getRequester().getAvatar()))
                .collect(Collectors.toList());
    }

    
    @Override
    public List<UserSearchResult> searchUsersAndDetermineStatus(String query, String currentUserId) {
        Set<String> blockedMeIds = friendRelationshipRepository
                .findByReceiverIdAndStatus(currentUserId, Status.BLOCKED).stream()
                .map(FriendRelationshipEty::getRequesterId)
                .collect(Collectors.toSet());

        Set<String> iBlockedIds = friendRelationshipRepository
                .findByRequesterIdAndStatus(currentUserId, Status.BLOCKED).stream()
                .map(FriendRelationshipEty::getReceiverId)
                .collect(Collectors.toSet());

        List<UserEty> matchingUsers = userRepository.findByNameRegex(query).stream()
                .filter(u -> !u.getId().equals(currentUserId))
                .filter(u -> !blockedMeIds.contains(u.getId()))
                .filter(u -> !iBlockedIds.contains(u.getId()))
                .collect(Collectors.toList());

        if (matchingUsers.isEmpty()) return Collections.emptyList();

        List<String> ids = matchingUsers.stream().map(UserEty::getId).collect(Collectors.toList());
        List<FriendRelationshipEty> rels =
                friendRelationshipRepository.findAllRelevantRelationships(currentUserId, ids);

        return matchingUsers.stream()
                .map(u -> mapToSearchResult(u, currentUserId, rels))
                .collect(Collectors.toList());
    }

    /**
     * Determina il bottone da mostrare nella UI di ricerca.
     *
     * Priorità:
     *  1. currentUser ha bloccato user          → BLOCKED
     *  2. currentUser→user ACCEPTED             → ALREADY_FRIENDS  ("Smetti di seguire")
     *  3. currentUser→user PENDING              → PENDING_REQUEST_SENT
     *  4. user→currentUser PENDING              → PENDING_REQUEST_RECEIVED
     *  5. altrimenti                            → AVAILABLE  ("Segui" / "Richiedi")
     *
     * Se user segue già currentUser (user→currentUser ACCEPTED) ma currentUser
     * non segue user, lo stato rimane AVAILABLE: currentUser può ancora inviare
     * la propria richiesta indipendentemente.
     */
    private UserSearchResult mapToSearchResult(UserEty user, String currentUserId,
            List<FriendRelationshipEty> allRels) {

        FriendRelationshipEty out = allRels.stream()
                .filter(r -> r.getRequesterId().equals(currentUserId)
                          && r.getReceiverId().equals(user.getId()))
                .findFirst().orElse(null);

        FriendRelationshipEty in = allRels.stream()
                .filter(r -> r.getRequesterId().equals(user.getId())
                          && r.getReceiverId().equals(currentUserId))
                .findFirst().orElse(null);

        if (out != null) {
            if (out.getStatus() == Status.BLOCKED) {
                return userMapper.mapToSearchResult(user, FriendRelationshipStatusEnum.BLOCKED);
            } else if (out.getStatus() == Status.ACCEPTED) {
                return userMapper.mapToSearchResult(user, FriendRelationshipStatusEnum.ALREADY_FRIENDS);
            } else {
                // PENDING
                return userMapper.mapToSearchResult(user, FriendRelationshipStatusEnum.PENDING_REQUEST_SENT);
            }
        }

        if (in != null && in.getStatus() == Status.PENDING) {
            return userMapper.mapToSearchResult(user, FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED);
        }

        return userMapper.mapToSearchResult(user, FriendRelationshipStatusEnum.AVAILABLE);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // INVIA RICHIESTA / FOLLOW
    // ─────────────────────────────────────────────────────────────────────────

    
    @Override
    @Transactional
    public String sendFriendRequest(String requesterId, String receiverId) {
        UserEty requester = userRepository.findById(requesterId).orElseThrow(() -> new NotFoundException("Utente richiedente non trovato."));
        UserEty receiver = userRepository.findById(receiverId).orElseThrow(() -> new NotFoundException("Utente destinatario non trovato."));

        Optional<FriendRelationshipEty> outOpt = friendRelationshipRepository.findByRequesterIdAndReceiverId(requesterId, receiverId);

        if (outOpt.isPresent()) {
            Status existing = outOpt.get().getStatus();
            if (existing == Status.ACCEPTED) 
            	throw new ConflictException("Stai già seguendo questo utente.");
            if (existing == Status.PENDING)  
            	throw new ConflictException("Hai già inviato una richiesta a questo utente");
            throw new ConflictException("Non puoi inviare richieste a questo utente.");
        }

        Optional<FriendRelationshipEty> inversePendingOpt = friendRelationshipRepository.findByRequesterIdAndReceiverId(receiverId, requesterId);

        if (inversePendingOpt.isPresent() && inversePendingOpt.get().getStatus() == Status.PENDING) {
            FriendRelationshipEty inv = inversePendingOpt.get();
            inv.setStatus(Status.ACCEPTED);
            friendRelationshipRepository.save(inv);
            saveRelation(requesterId, receiverId, Status.ACCEPTED);
            log.info("Auto-accept: {} e {} si seguono ora a vicenda", requesterId, receiverId);
            return "Richiesta accettata automaticamente. Vi seguite ora a vicenda!";
        }

        // ── Creazione normale ────────────────────────────────────────────────
        if (receiver.isPrivate()) {
            saveRelation(requesterId, receiverId, Status.PENDING);
            sendNotificationSafely(() -> notificationService.sendFriendRequestNotification(receiverId, requester.getName(), requesterId, requester.getAvatar()), "richiesta amicizia", receiverId, requesterId);
            return "Richiesta di amicizia inviata con successo.";
        } else {
            saveRelation(requesterId, receiverId, Status.ACCEPTED);
            sendNotificationSafely(() -> notificationService.sendNewFollowerNotification(receiverId, requester.getName(), requesterId, requester.getAvatar()), "nuovo follower", receiverId, requesterId);
            return "Ora segui questo utente!";
        }
    }

    private void saveRelation(String requesterId, String receiverId, Status status) {
        FriendRelationshipEty rel = new FriendRelationshipEty();
        rel.setRequesterId(requesterId);
        rel.setReceiverId(receiverId);
        rel.setStatus(status);
        friendRelationshipRepository.save(rel);
    }
 
    /**
     * accept: il record requesterId→receiverId passa da PENDING ad ACCEPTED.
     *
     *   IMPORTANTE: NON si crea nessun record inverso automaticamente.
     *   Dopo l'accettazione, requesterId vede receiverId ma NON viceversa.
     *   (TC1: B accetta A→B; B non vede A finché non invia B→A da sé)
     *   (TC3: B accetta A→B; stessa logica)
     *
     * decline: il record viene eliminato, requester torna AVAILABLE.
     */
    @Override
    @Transactional
    public String handleFriendRequest(String requesterId, String receiverId, String action) {
        return switch (action.toLowerCase()) {
            case "accept" -> {
                FriendRelationshipEty pending = friendRelationshipRepository
                        .findByRequesterIdAndReceiverId(requesterId, receiverId)
                        .filter(r -> r.getStatus() == Status.PENDING)
                        .orElseThrow(() -> new NotFoundException(
                                "Richiesta di amicizia in sospeso non trovata."));

                pending.setStatus(Status.ACCEPTED);
                friendRelationshipRepository.save(pending);
                log.info("Accettata: {} ora vede {}. {} NON vede ancora {} (nessun record inverso)",
                        requesterId, receiverId, receiverId, requesterId);
                yield "Richiesta di amicizia accettata.";
            }
            case "decline" -> {
                long deleted = friendRelationshipRepository.deleteFriendship(receiverId, requesterId);
                if (deleted == 0) throw new NotFoundException("Richiesta di amicizia in sospeso non trovata.");
                yield "Richiesta di amicizia rifiutata.";
            }
            default -> throw new IllegalArgumentException("Azione non valida. Usa 'accept' o 'decline'.");
        };
    }

     
    @Override
    @Transactional
    public void deleteFriendship(String requesterId, String friendId) {
        Optional<FriendRelationshipEty> directOpt =
                friendRelationshipRepository.findByRequesterIdAndReceiverId(requesterId, friendId);

        if (directOpt.isPresent()) {
            friendRelationshipRepository.delete(directOpt.get());
            log.info("Eliminato follow: {} non segue più {}", requesterId, friendId);
            return;
        }

        Optional<FriendRelationshipEty> inverseOpt =
                friendRelationshipRepository.findByRequesterIdAndReceiverId(friendId, requesterId);

        if (inverseOpt.isPresent()) {
            friendRelationshipRepository.delete(inverseOpt.get());
            log.info("Rimosso follower: {} tolto dai follower di {}", friendId, requesterId);
            return;
        }

        throw new NotFoundException("Relazione di amicizia non trovata.");
    }

   
    @Override
    public void executeBlockAction(String currentUserId, String targetUserId, BlockActionEnum action) {
        switch (action) {
            case BLOCK -> friendRelationshipRepository
                    .updateRelationshipStatus(currentUserId, targetUserId, Status.BLOCKED);
            case UNBLOCK -> friendRelationshipRepository
                    .deleteRelationship(currentUserId, targetUserId);
            default -> throw new IllegalArgumentException("Azione di blocco non valida: " + action);
        }
    }

    @Override
    public List<BlockedUserDTO> getBlockedUsers(String currentUserId) {
        return friendRelationshipRepository.findMyBlockedRelationships(currentUserId).stream()
                .map(e -> buildBlockedUserDTO(e, currentUserId))
                .collect(Collectors.toList());
    }

    private BlockedUserDTO buildBlockedUserDTO(FriendRelationshipEty ety, String currentUserId) {
        String otherId = ety.getReceiverId().equals(currentUserId)
                ? ety.getRequesterId() : ety.getReceiverId();
        UserEty user = userRepository.findById(otherId)
                .orElseThrow(() -> new NotFoundException("Utente bloccato non trovato: " + otherId));
        BlockedUserDTO dto = new BlockedUserDTO();
        dto.setId(user.getId());
        dto.setAvatar(user.getAvatar());
        dto.setName(user.getName());
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUGGERIMENTI
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<UserSuggestionDTO> getFriendSuggestions(String currentUserId, int limit) {
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));

        Set<String> exclude = new java.util.HashSet<>();
        exclude.add(currentUserId);
        friendRelationshipRepository.findByRequesterIdOrReceiverId(currentUserId)
                .forEach(r -> {
                    exclude.add(r.getRequesterId());
                    exclude.add(r.getReceiverId());
                });

        int safeLimit = Math.min(Math.max(limit, 1), 20);
        List<UserEty> candidates = fetchCandidates(exclude);

        if (candidates.isEmpty()) {
            log.warn("[suggestions] Nessun candidato per {}", currentUserId);
            return Collections.emptyList();
        }

        Collections.shuffle(candidates);

        return candidates.stream()
                .limit(safeLimit)
                .map(u -> UserSuggestionDTO.builder()
                        .id(u.getId())
                        .name(u.getName())
                        .avatar(u.getAvatar() != null ? u.getAvatar()
                                : "https://ui-avatars.com/api/?name="
                                  + u.getName().replace(" ", "+") + "&background=random")
                        .bio(null)
                        .travelsCount(0)
                        .mutualFriendsCount(0)
                        .mutualFriendsPreview(null)
                        .reason("Nuovo utente da scoprire")
                        .build())
                .collect(Collectors.toList());
    }

    private List<UserEty> fetchCandidates(Set<String> exclude) {
        PageRequest page = PageRequest.of(0, 100);
        try {
            if (exclude.isEmpty()) {
                return userRepository.findAll(page).getContent();
            }
            return userRepository
                    .findPotentialSuggestionsOptimized(new ArrayList<>(exclude), page).stream()
                    .limit(100)
                    .map(arr -> {
                        UserEty u = new UserEty();
                        u.setId((String) arr[0]);
                        u.setName((String) arr[1]);
                        u.setAvatar((String) arr[2]);
                        return u;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("[suggestions] Fallback: {}", e.getMessage());
            return userRepository.findAll(page).getContent().stream()
                    .filter(u -> !exclude.contains(u.getId()))
                    .collect(Collectors.toList());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY
    // ─────────────────────────────────────────────────────────────────────────

    private void sendNotificationSafely(Runnable task, String type, String to, String from) {
        try {
            task.run();
            log.info("Notifica [{}] inviata a {} da {}", type, to, from);
        } catch (Exception e) {
            log.error("Errore notifica [{}] → {} da {}: {}", type, to, from, e.getMessage(), e);
        }
    }
}
