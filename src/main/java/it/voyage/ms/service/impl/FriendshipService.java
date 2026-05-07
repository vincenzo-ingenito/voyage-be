package it.voyage.ms.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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

    @Override
    public boolean checkIfUserAreFriends(String userId, String friendId) {
        // Con amicizie asimmetriche, verifica se esiste almeno una relazione ACCEPTED
        // in qualsiasi direzione tra i due utenti
        Optional<FriendRelationshipEty> relation1 = friendRelationshipRepository.findByRequesterIdAndReceiverId(userId, friendId);
        
        if (relation1.isPresent() && relation1.get().getStatus() == Status.ACCEPTED) {
            return true;
        }
        
        // Controlla anche nella direzione opposta
        Optional<FriendRelationshipEty> relation2 = friendRelationshipRepository.findByRequesterIdAndReceiverId(friendId, userId);
        
        return relation2.isPresent() && relation2.get().getStatus() == Status.ACCEPTED;
    }

    @Override
    public List<UserDto> getAcceptedFriendsList(String currentUserId) {
        List<UserEty> users = userRepository.findAcceptedFriendsAndSelf(currentUserId, Status.ACCEPTED);

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
        List<FriendRelationshipEty> pendingRequests = friendRelationshipRepository.findPendingRequestsWithRequester(receiverId, Status.PENDING);

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

        // Utenti che il currentUser ha bloccato
        Set<String> iBlockedIds = friendRelationshipRepository
            .findByRequesterIdAndStatus(currentUserId, Status.BLOCKED).stream()
            .map(FriendRelationshipEty::getReceiverId)
            .collect(Collectors.toSet());

        List<UserEty> allMatchingUsers = userRepository.findByNameRegex(query).stream()
            .filter(user -> !user.getId().equals(currentUserId))
            .filter(user -> !blockedMeIds.contains(user.getId())) // Escludi chi mi ha bloccato
            .filter(user -> !iBlockedIds.contains(user.getId())) // Escludi chi ho bloccato
            .collect(Collectors.toList());

        if (allMatchingUsers.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> userIdsToCheck = allMatchingUsers.stream()
            .map(UserEty::getId)
            .collect(Collectors.toList());

        List<FriendRelationshipEty> relevantRelationships = friendRelationshipRepository
            .findAllRelevantRelationships(currentUserId, userIdsToCheck);

        return allMatchingUsers.stream()
            .map(user -> mapToSearchResult(user, currentUserId, relevantRelationships))
            .collect(Collectors.toList());
    }

    private UserSearchResult mapToSearchResult(UserEty user, String currentUserId,
            List<FriendRelationshipEty> allRelationships) {

        // Trova TUTTE le relazioni con questo utente
        FriendRelationshipEty outgoingRel = allRelationships.stream()
            .filter(r -> r.getRequesterId().equals(currentUserId) && r.getReceiverId().equals(user.getId()))
            .findFirst()
            .orElse(null);
            
        FriendRelationshipEty incomingRel = allRelationships.stream()
            .filter(r -> r.getRequesterId().equals(user.getId()) && r.getReceiverId().equals(currentUserId))
            .findFirst()
            .orElse(null);

        FriendRelationshipStatusEnum status = FriendRelationshipStatusEnum.AVAILABLE;

        // Priorità 1: Verifica se currentUser ha bloccato l'utente
        if (outgoingRel != null && outgoingRel.getStatus() == Status.BLOCKED) {
            status = FriendRelationshipStatusEnum.BLOCKED;
        }
        // Priorità 2: PENDING ha sempre precedenza su ACCEPTED
        // Verifica se currentUser ha una richiesta PENDING in uscita
        else if (outgoingRel != null && outgoingRel.getStatus() == Status.PENDING) {
            status = FriendRelationshipStatusEnum.PENDING_REQUEST_SENT;
        }
        // Priorità 3: Verifica se c'è una richiesta PENDING in entrata
        else if (incomingRel != null && incomingRel.getStatus() == Status.PENDING) {
            status = FriendRelationshipStatusEnum.PENDING_REQUEST_RECEIVED;
        }
        // Priorità 4: Verifica relazioni ACCEPTED
        else if (outgoingRel != null && outgoingRel.getStatus() == Status.ACCEPTED) {
            // Entrambe le relazioni ACCEPTED → ALREADY_FRIENDS
            if (incomingRel != null && incomingRel.getStatus() == Status.ACCEPTED) {
                status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
            }
            // Solo outgoingRel ACCEPTED
            else {
                // Se la relazione è unidirezionale, currentUser segue user ma non viceversa
                if (outgoingRel.isUnidirectional()) {
                    status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
                } else {
                    // Relazione bidirezionale → amici
                    status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
                }
            }
        }
        else if (incomingRel != null && incomingRel.getStatus() == Status.ACCEPTED) {
            // C'è solo una relazione in entrata ACCEPTED
            // Verifica se è unidirezionale (user può vedere currentUser, ma non viceversa)
            if (incomingRel.isUnidirectional()) {
                // Relazione unidirezionale: user vede currentUser
                // CurrentUser NON vede ancora user → può inviare richiesta
                status = FriendRelationshipStatusEnum.AVAILABLE;
            } else {
                // Relazione bidirezionale in entrata
                if (user.isPrivate()) {
                    status = FriendRelationshipStatusEnum.AVAILABLE;
                } else {
                    // Utente pubblico con relazione bidirezionale in entrata → ALREADY_FRIENDS
                    status = FriendRelationshipStatusEnum.ALREADY_FRIENDS;
                }
            }
        }

        return userMapper.mapToSearchResult(user, status);
    }

    @Override
    public String sendFriendRequest(String requesterId, String receiverId) {
        UserEty requesterUser = userRepository.findById(requesterId)
            .orElseThrow(() -> new NotFoundException("Utente richiedente non trovato."));
        UserEty receiverUser = userRepository.findById(receiverId)
            .orElseThrow(() -> new NotFoundException("Utente destinatario non trovato."));

        // Controlla se esiste già una relazione da requester → receiver
        Optional<FriendRelationshipEty> existingRelation = 
            friendRelationshipRepository.findByRequesterIdAndReceiverId(requesterId, receiverId);
        
        if (existingRelation.isPresent()) {
            FriendRelationshipEty relation = existingRelation.get();
            
            if (relation.getStatus() == Status.ACCEPTED) {
                return relation.isUnidirectional() 
                    ? "Stai già seguendo questo utente." 
                    : "Sei già amico di questo utente.";
            }
            
            if (relation.getStatus() == Status.PENDING) {
                return "Hai già inviato una richiesta a questo utente.";
            }
            
            if (relation.getStatus() == Status.BLOCKED) {
                throw new ConflictException("Non puoi inviare richieste a questo utente.");
            }
        }
        
        // Controlla se esiste una relazione inversa (receiver → requester)
        Optional<FriendRelationshipEty> inverseRelation = 
            friendRelationshipRepository.findByRequesterIdAndReceiverId(receiverId, requesterId);
        
        if (inverseRelation.isPresent()) {
            FriendRelationshipEty relation = inverseRelation.get();
            
            // CASO SPECIALE: Esiste già un follow unidirezionale inverso
            if (relation.getStatus() == Status.ACCEPTED && relation.isUnidirectional()) {
                // Receiver aveva già un follower (requester)
                // Ora il requester vuole inviare richiesta/follow al receiver
                
                // CASO SPECIALE: Entrambi privati con relazione unidirezionale inversa
                // Receiver (privato) aveva accettato la richiesta di Requester (privato)
                // Ora Requester vuole inviare richiesta a Receiver
                if (requesterUser.isPrivate() && receiverUser.isPrivate()) {
                    // Crea una nuova richiesta PENDING (non accettare automaticamente)
                    FriendRelationshipEty newRequest = new FriendRelationshipEty();
                    newRequest.setRequesterId(requesterId);
                    newRequest.setReceiverId(receiverId);
                    newRequest.setStatus(Status.PENDING);
                    newRequest.setUnidirectional(false); // Sarà unidirezionale quando accettata
                    friendRelationshipRepository.save(newRequest);
                    
                    // Invia notifica FCM per richiesta di amicizia
                    try {
                        notificationService.sendFriendRequestNotification(
                            receiverId,
                            requesterUser.getName(),
                            requesterId,
                            requesterUser.getAvatar()
                        );
                        log.info("Notifica richiesta amicizia inviata a {} da {}", receiverId, requesterId);
                    } catch (Exception e) {
                        log.error("Errore invio notifica richiesta amicizia: {}", e.getMessage(), e);
                    }
                    
                    return "Richiesta di amicizia inviata con successo.";
                }
                else if (receiverUser.isPrivate()) {
                    // Il receiver è privato (ma requester è pubblico) → crea richiesta PENDING bidirezionale
                    FriendRelationshipEty newRequest = new FriendRelationshipEty();
                    newRequest.setRequesterId(requesterId);
                    newRequest.setReceiverId(receiverId);
                    newRequest.setStatus(Status.PENDING);
                    newRequest.setUnidirectional(false); // Sarà bidirezionale quando accettata
                    friendRelationshipRepository.save(newRequest);
                    
                    // Invia notifica FCM per richiesta di amicizia
                    try {
                        notificationService.sendFriendRequestNotification(
                            receiverId,
                            requesterUser.getName(),
                            requesterId,
                            requesterUser.getAvatar()
                        );
                        log.info("Notifica richiesta amicizia inviata a {} da {}", receiverId, requesterId);
                    } catch (Exception e) {
                        log.error("Errore invio notifica richiesta amicizia: {}", e.getMessage(), e);
                    }
                    
                    return "Richiesta di amicizia inviata con successo.";
                } else {
                    // Il receiver è pubblico → crea follow unidirezionale
                    FriendRelationshipEty newFollow = new FriendRelationshipEty();
                    newFollow.setRequesterId(requesterId);
                    newFollow.setReceiverId(receiverId);
                    newFollow.setStatus(Status.ACCEPTED);
                    newFollow.setUnidirectional(true);
                    friendRelationshipRepository.save(newFollow);
                    
                    // Invia notifica al seguito
                    try {
                        notificationService.sendNewFollowerNotification(
                            receiverId,
                            requesterUser.getName(),
                            requesterId,
                            requesterUser.getAvatar()
                        );
                        log.info("Notifica nuovo follower inviata a {} da {}", receiverId, requesterId);
                    } catch (Exception e) {
                        log.error("Errore invio notifica nuovo follower: {}", e.getMessage(), e);
                    }
                    
                    return "Ora segui questo utente!";
                }
            }
            
            // CASO: Esiste richiesta PENDING inversa (receiver aveva chiesto amicizia a requester)
            if (relation.getStatus() == Status.PENDING) {
                // Accetta automaticamente la richiesta esistente trasformandola in amicizia bidirezionale
                relation.setStatus(Status.ACCEPTED);
                relation.setUnidirectional(false); // Amicizia bidirezionale
                friendRelationshipRepository.save(relation);
                return "Richiesta di amicizia accettata automaticamente!";
            }
            
            // Se la relazione inversa è BLOCKED, non facciamo nulla (lasciamo che il requester crei la sua)
        }

        // Nessuna relazione esistente → crea nuova
        FriendRelationshipEty newRequest = new FriendRelationshipEty();
        newRequest.setRequesterId(requesterId);
        newRequest.setReceiverId(receiverId);
        
        // LOGICA CORRETTA:
        // - Se RECEIVER è PRIVATO → sempre PENDING (richiede approvazione), indipendentemente dal tipo di requester
        // - Se RECEIVER è PUBBLICO → sempre ACCEPTED immediato (follow unidirezionale), indipendentemente dal tipo di requester
        
        if (receiverUser.isPrivate()) {
            // Receiver privato → sempre richiesta PENDING (sia da pubblico che da privato)
            newRequest.setStatus(Status.PENDING);
            newRequest.setUnidirectional(false);
            friendRelationshipRepository.save(newRequest);
            
            // Invia notifica FCM per richiesta di amicizia
            try {
                notificationService.sendFriendRequestNotification(
                    receiverId,
                    requesterUser.getName(),
                    requesterId,
                    requesterUser.getAvatar()
                );
                log.info("Notifica richiesta amicizia inviata a {} da {}", receiverId, requesterId);
            } catch (Exception e) {
                log.error("Errore invio notifica richiesta amicizia: {}", e.getMessage(), e);
            }
            
            return "Richiesta di amicizia inviata con successo.";
        } else {
            // Receiver è pubblico → follow unidirezionale immediato (sia da pubblico che da privato)
            newRequest.setStatus(Status.ACCEPTED);
            newRequest.setUnidirectional(true);
            friendRelationshipRepository.save(newRequest);
            
            // Invia notifica al seguito
            try {
                notificationService.sendNewFollowerNotification(
                    receiverId,
                    requesterUser.getName(),
                    requesterId,
                    requesterUser.getAvatar()
                );
                log.info("Notifica nuovo follower inviata a {} da {}", receiverId, requesterId);
            } catch (Exception e) {
                log.error("Errore invio notifica nuovo follower: {}", e.getMessage(), e);
            }
            
            return "Ora segui questo utente! Il profilo è pubblico.";
        }
    }

    @Override
    public String handleFriendRequest(String requesterId, String receiverId, String action) {
        if ("accept".equalsIgnoreCase(action)) {
            // Trova la richiesta pending
            Optional<FriendRelationshipEty> pendingRequest = 
                friendRelationshipRepository.findByRequesterIdAndReceiverId(requesterId, receiverId);
            
            if (pendingRequest.isEmpty() || pendingRequest.get().getStatus() != Status.PENDING) {
                throw new NotFoundException("Richiesta di amicizia in sospeso non trovata.");
            }
            
            // Recupera gli utenti per verificare i loro tipi di profilo
            UserEty requesterUser = userRepository.findById(requesterId)
                .orElseThrow(() -> new NotFoundException("Utente richiedente non trovato."));
            UserEty receiverUser = userRepository.findById(receiverId)
                .orElseThrow(() -> new NotFoundException("Utente destinatario non trovato."));
            
            FriendRelationshipEty request = pendingRequest.get();
            request.setStatus(Status.ACCEPTED);
            
            // LOGICA CORRETTA PER UNIDIREZIONALITÀ:
            // - Se ENTRAMBI sono privati → unidirezionale (requester vede receiver, ma non viceversa)
            // - Altrimenti → bidirezionale (amicizia reciproca)
            boolean bothPrivate = requesterUser.isPrivate() && receiverUser.isPrivate();
            request.setUnidirectional(bothPrivate);
            friendRelationshipRepository.save(request);
            
            if (bothPrivate) {
                return "Richiesta di amicizia accettata. L'amico potrà vedere i tuoi contenuti.";
            } else {
                return "Richiesta di amicizia accettata.";
            }
            
        } else if ("decline".equalsIgnoreCase(action)) {
            int updatedCount = (int) friendRelationshipRepository.deleteFriendship(receiverId, requesterId);
            
            if (updatedCount == 0) {
                throw new NotFoundException("Richiesta di amicizia in sospeso non trovata.");
            }
            
            return "Richiesta di amicizia rifiutata.";
            
        } else {
            throw new IllegalArgumentException("Azione non valida. Usa 'accept' o 'decline'.");
        }
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
        friendRelationshipRepository.updateRelationshipStatus(currentUserId, userToBlockId, Status.BLOCKED, blockerId);
    }

    private void unblockUser(String currentUserId, String userToUnblockId, String blockerId) {
        friendRelationshipRepository.deleteRelationship(currentUserId, userToUnblockId, blockerId);
    }

    @Override
    public List<BlockedUserDTO> getBlockedUsers(String currentUserId) {
        List<FriendRelationshipEty> etys = friendRelationshipRepository.findMyBlockedRelationships(currentUserId);
        return etys.stream().map(e -> getDtoFromEty(e, currentUserId)).collect(Collectors.toList());
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

    @Override
    @Transactional(readOnly = true)
    public List<UserSuggestionDTO> getFriendSuggestions(String currentUserId, int limit) {
        // 1. Recupera l'utente corrente per ottenere anche la sua email
        UserEty currentUser = userRepository.findById(currentUserId)
            .orElseThrow(() -> new NotFoundException("Utente corrente non trovato"));
        
        // 2. Recupera gli amici dell'utente corrente
        List<String> currentUserFriendIds = friendRelationshipRepository
            .findFriendshipsByUserIdAndStatus(currentUserId, Status.ACCEPTED).stream()
            .map(rel -> rel.getRequesterId().equals(currentUserId) 
                ? rel.getReceiverId() 
                : rel.getRequesterId())
            .collect(Collectors.toList());
        
        // 3. Recupera utenti con cui esiste già una relazione (amici, bloccati, pending)
        Set<String> usersToExclude = new java.util.HashSet<>(currentUserFriendIds);
        usersToExclude.add(currentUserId); // Escludi se stesso per UUID
        
        // Aggiungi TUTTE le relazioni esistenti (ACCEPTED, BLOCKED, PENDING) per sicurezza
        friendRelationshipRepository.findByRequesterIdOrReceiverId(currentUserId).stream()
            .forEach(rel -> {
                usersToExclude.add(rel.getRequesterId());
                usersToExclude.add(rel.getReceiverId());
            });
        

        // PROTEZIONE OOM: Limite massimo forzato
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        
        log.info("[getFriendSuggestions] RANDOM MODE: Fetching {} random suggestions for user {} (excluding {} users)", 
                 safeLimit, currentUserId, usersToExclude.size());
        
        // PROTEZIONE OOM: Usa query ottimizzata invece di findAll()
        List<UserEty> candidates;
        
        try {
            if (usersToExclude.isEmpty()) {
                // Se non ci sono esclusioni, prendi i primi 100 utenti
                log.debug("[getFriendSuggestions] No exclusions, fetching first 100 users");
                candidates = userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 100))
                    .getContent();
            } else {
                // Usa la query ottimizzata con esclusioni
                List<Object[]> potentialCandidates = userRepository.findPotentialSuggestionsOptimized(
                    new ArrayList<>(usersToExclude),
                    org.springframework.data.domain.PageRequest.of(0, 100)
                );
                
                log.debug("[getFriendSuggestions] Found {} potential candidates from optimized query", 
                         potentialCandidates.size());

                // PROTEZIONE OOM: Converti solo i risultati necessari (max 100)
                candidates = potentialCandidates.stream()
                    .limit(100)
                    .map(arr -> {
                        UserEty user = new UserEty();
                        user.setId((String) arr[0]);
                        user.setName((String) arr[1]);
                        user.setAvatar((String) arr[2]);
                        return user;
                    })
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("[getFriendSuggestions] Error fetching candidates, falling back to simple query: {}", e.getMessage());
            // Fallback: usa query semplice senza ottimizzazioni
            candidates = userRepository.findAll(org.springframework.data.domain.PageRequest.of(0, 100))
                .getContent().stream()
                .filter(user -> !usersToExclude.contains(user.getId()))
                .collect(Collectors.toList());
        }

        if (candidates.isEmpty()) {
            log.warn("[getFriendSuggestions] No candidates found for user {}", currentUserId);
            return Collections.emptyList();
        }

        log.debug("[getFriendSuggestions] Found {} candidates before shuffle", candidates.size());

        // RANDOMIZZAZIONE: Shuffle per casualità
        Collections.shuffle(candidates);
        
        // PROTEZIONE OOM: Prendi solo il limite richiesto
        List<UserSuggestionDTO> result = candidates.stream()
            .limit(safeLimit)
            .map(user -> UserSuggestionDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .avatar(user.getAvatar() != null ? user.getAvatar() : 
                    "https://ui-avatars.com/api/?name=" + user.getName().replace(" ", "+") + "&background=random")
                .bio(null)
                .travelsCount(0)
                .mutualFriendsCount(0)
                .mutualFriendsPreview(null)
                .reason("Nuovo utente da scoprire")
                .build())
            .collect(Collectors.toList());

        log.info("[getFriendSuggestions] Returning {} random suggestions (OOM-safe) for user {}", 
                 result.size(), currentUserId);
        
        return result;
    }
 
}
