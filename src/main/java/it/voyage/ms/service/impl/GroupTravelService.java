package it.voyage.ms.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.request.ParticipantInviteRequest;
import it.voyage.ms.dto.response.ParticipantDTO;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.repository.entity.ParticipantRole;
import it.voyage.ms.repository.entity.ParticipantStatus;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.TravelParticipantEty;
import it.voyage.ms.repository.entity.TravelType;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.TravelParticipantRepository;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.service.IFriendshipService;
import it.voyage.ms.service.IGroupTravelService;
import it.voyage.ms.service.INotificationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@AllArgsConstructor
public class GroupTravelService implements IGroupTravelService {

    private final TravelRepository travelRepository;
    private final TravelParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final IFriendshipService friendshipService;
    private final INotificationService notificationService;

    @Override
    @Transactional
    public List<ParticipantDTO> inviteParticipants(Long travelId, List<ParticipantInviteRequest> invites, String invitedBy) {
        
        log.info("Invito {} partecipanti al viaggio {}", invites.size(), travelId);
        
        // Verifica che il viaggio esista e sia di tipo GROUP
        TravelEty travel = travelRepository.findById(travelId).orElseThrow(() -> new BusinessException("Viaggio non trovato"));
        
        if (travel.getTravelType() != TravelType.GROUP) {
            throw new BusinessException("Solo i viaggi di gruppo possono avere partecipanti");
        }
        
        // Verifica che l'utente che invita sia l'owner
        if (!travel.getUser().getId().equals(invitedBy)) {
            throw new BusinessException("Solo il proprietario del viaggio può invitare partecipanti");
        }
        
        List<ParticipantDTO> invitedParticipants = new ArrayList<>();
        
        for (ParticipantInviteRequest invite : invites) {
            // Verifica che l'utente da invitare non sia l'owner stesso
            if (invite.getUserId().equals(invitedBy)) {
                log.warn("Tentativo di auto-invito ignorato per utente {}", invitedBy);
                continue;
            }
            
            // Verifica che l'utente non sia già partecipante
            if (participantRepository.existsByTravelIdAndUserId(travelId, invite.getUserId())) {
                log.warn("Utente {} già partecipante al viaggio {}", invite.getUserId(), travelId);
                continue;
            }
            
            // Verifica che ci sia amicizia
            if (!friendshipService.checkIfUserAreFriends(invitedBy, invite.getUserId())) {
                log.warn("Tentativo di invitare non-amico {} al viaggio {}", invite.getUserId(), travelId);
                throw new BusinessException("Puoi invitare solo amici ai tuoi viaggi");
            }
            
            // Crea il partecipante
            TravelParticipantEty participant = new TravelParticipantEty();
            participant.setTravel(travel);
            participant.setUserId(invite.getUserId());
            participant.setRole(invite.getRole());
            participant.setStatus(ParticipantStatus.PENDING);
            participant.setInvitedBy(invitedBy);
            participant.setInvitedAt(LocalDateTime.now());
            
            TravelParticipantEty saved = participantRepository.save(participant);
            invitedParticipants.add(toDTO(saved));
            
            log.info("Invitato utente {} come {} al viaggio {}", 
                    invite.getUserId(), invite.getRole(), travelId);
            
            // FIX: Invia notifica all'utente invitato
            try {
                UserEty inviter = userRepository.findById(invitedBy).orElse(null);
                String inviterName = inviter != null ? inviter.getName() : "Un amico";
                notificationService.sendGroupTravelInviteNotification(invite.getUserId(), inviterName, travel.getTravelName(), travelId);
            } catch (Exception e) {
                log.error("Errore invio notifica per invito", e);
                // Non blocchiamo il flusso se la notifica fallisce
            }
        }
        
        return invitedParticipants;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getParticipants(Long travelId) {
        log.info("Recupero partecipanti per viaggio {}", travelId);
        
        // FIX N+1: Usa query ottimizzata con fetch join
        List<TravelParticipantEty> participants = 
            participantRepository.findByTravelIdWithTravelAndOwner(travelId);
        
        // FIX N+1: Batch load di tutti gli utenti necessari (invitati + invitanti)
        Set<String> userIds = participants.stream()
            .flatMap(p -> Stream.of(p.getUserId(), p.getInvitedBy()))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<String, UserEty> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            userMap = userRepository.findAllById(userIds)
                .stream()
                .collect(Collectors.toMap(UserEty::getId, u -> u));
        }
        
        log.debug("Caricati {} utenti in batch per {} partecipanti", userMap.size(), participants.size());
        
        // Usa il metodo toDTO ottimizzato con mappa pre-caricata
        final Map<String, UserEty> finalUserMap = userMap;
        return participants.stream()
            .map(p -> toDTO(p, finalUserMap))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipantDTO respondToInvite(Long travelId, String userId, boolean accept) {
        log.info("Risposta invito viaggio {}: utente {}, accept={}", travelId, userId, accept);
        
        TravelParticipantEty participant = participantRepository
                .findByTravelIdAndUserId(travelId, userId)
                .orElseThrow(() -> new BusinessException("Invito non trovato"));
        
        if (participant.getStatus() != ParticipantStatus.PENDING) {
            throw new BusinessException("L'invito è già stato processato");
        }
        
        participant.setStatus(accept ? ParticipantStatus.ACCEPTED : ParticipantStatus.DECLINED);
        participant.setRespondedAt(LocalDateTime.now());
        
        TravelParticipantEty saved = participantRepository.save(participant);
        log.info("Invito {} per viaggio {}", accept ? "accettato" : "rifiutato", travelId);
        
        // FIX: Invia notifica al proprietario se l'invito è stato accettato
        if (accept) {
            try {
                TravelEty travel = participant.getTravel();
                UserEty accepter = userRepository.findById(userId).orElse(null);
                String accepterName = accepter != null ? accepter.getName() : "Un utente";
                
                notificationService.sendInviteAcceptedNotification(travel.getUser().getId(), accepterName, travel.getTravelName());
            } catch (Exception e) {
                log.error("Errore invio notifica accettazione invito", e);
                // Non blocchiamo il flusso se la notifica fallisce
            }
        }
        
        return toDTO(saved);
    }

    @Override
    @Transactional
    public ParticipantDTO updateParticipantRole(
            Long travelId,
            String userId,
            ParticipantRole newRole,
            String requesterId) {
        
        log.info("Cambio ruolo partecipante {} a {} per viaggio {}", userId, newRole, travelId);
        
        // Verifica che il richiedente sia l'owner
        TravelEty travel = travelRepository.findById(travelId).orElseThrow(() -> new BusinessException("Viaggio non trovato"));
        
        if (!travel.getUser().getId().equals(requesterId)) {
            throw new BusinessException("Solo il proprietario può cambiare i ruoli");
        }
        
        // Trova e aggiorna il partecipante
        TravelParticipantEty participant = participantRepository
                .findByTravelIdAndUserId(travelId, userId)
                .orElseThrow(() -> new BusinessException("Partecipante non trovato"));
        
        participant.setRole(newRole);
        TravelParticipantEty saved = participantRepository.save(participant);
        
        log.info("Ruolo aggiornato a {} per partecipante {}", newRole, userId);
        return toDTO(saved);
    }

    @Override
    @Transactional
    public void removeParticipant(Long travelId, String userId, String requesterId) {
        log.info("Rimozione partecipante {} da viaggio {}", userId, travelId);
        
        // Verifica che il viaggio esista
        TravelEty travel = travelRepository.findById(travelId).orElseThrow(() -> new BusinessException("Viaggio non trovato"));
        
        // Permetti la rimozione se:
        // 1. Il richiedente è l'owner (può rimuovere chiunque)
        // 2. Il richiedente sta rimuovendo se stesso (può lasciare il gruppo)
        boolean isOwner = travel.getUser().getId().equals(requesterId);
        boolean isLeavingGroup = userId.equals(requesterId);
        
        if (!isOwner && !isLeavingGroup) {
            throw new BusinessException("Solo il proprietario può rimuovere partecipanti");
        }
        
        // Verifica che il partecipante esista
        if (!participantRepository.existsByTravelIdAndUserId(travelId, userId)) {
            throw new BusinessException("Partecipante non trovato");
        }
        
        participantRepository.deleteByTravelIdAndUserId(travelId, userId);
        
        if (isLeavingGroup) {
            log.info("Partecipante {} ha lasciato il viaggio {}", userId, travelId);
        } else {
            log.info("Partecipante {} rimosso dal viaggio {} dal proprietario", userId, travelId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDTO> getUserGroupTravelParticipations(String userId) {
        log.info("Recupero viaggi di gruppo per utente {}", userId);
        
        // FIX N+1: Usa query ottimizzata con fetch join (travel + owner già caricati)
        List<TravelParticipantEty> participations = 
            participantRepository.findByUserIdWithTravelAndOwner(userId);
        
        // Filtra solo PENDING e ACCEPTED (esclude DECLINED)
        List<TravelParticipantEty> filtered = participations.stream()
            .filter(p -> p.getStatus() == ParticipantStatus.PENDING || 
                        p.getStatus() == ParticipantStatus.ACCEPTED)
            .collect(Collectors.toList());
        
        // FIX N+1: Batch load degli utenti invitanti
        Set<String> inviterIds = filtered.stream()
            .map(TravelParticipantEty::getInvitedBy)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Map<String, UserEty> userMap = new HashMap<>();
        if (!inviterIds.isEmpty()) {
            userMap = userRepository.findAllById(inviterIds)
                .stream()
                .collect(Collectors.toMap(UserEty::getId, u -> u));
        }
        
        log.debug("Caricati {} invitanti in batch per {} partecipazioni", userMap.size(), filtered.size());
        
        // Usa il metodo toDTO ottimizzato con mappa pre-caricata
        final Map<String, UserEty> finalUserMap = userMap;
        return filtered.stream()
            .map(p -> toDTOForUserParticipations(p, finalUserMap))
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public boolean canUserEditTravel(Long travelId, String userId) {
        // Verifica se è l'owner
        TravelEty travel = travelRepository.findById(travelId).orElse(null);
        if (travel == null) {
            return false;
        }
        
        if (travel.getUser().getId().equals(userId)) {
            return true;
        }
        
        // Verifica se è un editor accettato
        return participantRepository.findByTravelIdAndUserId(travelId, userId)
                .map(p -> p.getStatus() == ParticipantStatus.ACCEPTED && p.getRole() == ParticipantRole.EDITOR)
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public boolean canUserViewTravel(Long travelId, String userId) {
        // Verifica se è l'owner
        TravelEty travel = travelRepository.findById(travelId).orElse(null);
        if (travel == null) {
            return false;
        }
        
        if (travel.getUser().getId().equals(userId)) {
            return true;
        }
        
        // Verifica se è un partecipante accettato (viewer o editor)
        if (participantRepository.findByTravelIdAndUserId(travelId, userId)
                .map(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .orElse(false)) {
            return true;
        }
        
        // Verifica se è amico dell'owner
        return friendshipService.checkIfUserAreFriends(userId, travel.getUser().getId());
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Converte TravelParticipantEty in ParticipantDTO (per chiamate singole)
     * Usa query individuali - da usare solo per operazioni su singoli partecipanti
     */
    private ParticipantDTO toDTO(TravelParticipantEty entity) {
        ParticipantDTO dto = new ParticipantDTO();
        dto.setId(entity.getId());
        dto.setTravelId(entity.getTravel().getId());
        dto.setUserId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setInvitedBy(entity.getInvitedBy());
        dto.setInvitedAt(entity.getInvitedAt());
        dto.setRespondedAt(entity.getRespondedAt());
        
        // Popola i dati dell'utente invitato se disponibili
        UserEty user = userRepository.findById(entity.getUserId()).orElse(null);
        if (user != null) {
            dto.setUserName(user.getName());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatar(user.getAvatar());
            log.debug("toDTO: Popolato dati utente invitato: {}", user.getName());
        } else {
            log.warn("toDTO: Utente invitato {} non trovato nel database", entity.getUserId());
        }
        
        // Popola il nome dell'invitante se disponibile
        log.debug("toDTO: Ricerca invitante con ID: {}", entity.getInvitedBy());
        UserEty inviter = userRepository.findById(entity.getInvitedBy()).orElse(null);
        if (inviter != null) {
            dto.setInvitedByName(inviter.getName());
            log.info("toDTO: Popolato invitedByName con: {}", inviter.getName());
        } else {
            log.warn("toDTO: Invitante {} non trovato nel database, invitedByName rimarrà null", entity.getInvitedBy());
        }
        
        return dto;
    }
    
    /**
     * Converte TravelParticipantEty in ParticipantDTO usando mappa pre-caricata
     * OTTIMIZZATO: Evita N+1 query usando batch loading
     * @param entity Il partecipante da convertire
     * @param userMap Mappa di utenti pre-caricati (userId -> UserEty)
     */
    private ParticipantDTO toDTO(TravelParticipantEty entity, Map<String, UserEty> userMap) {
        ParticipantDTO dto = new ParticipantDTO();
        dto.setId(entity.getId());
        dto.setTravelId(entity.getTravel().getId());
        dto.setUserId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setInvitedBy(entity.getInvitedBy());
        dto.setInvitedAt(entity.getInvitedAt());
        dto.setRespondedAt(entity.getRespondedAt());
        
        // FIX N+1: Usa mappa pre-caricata invece di query individuali
        UserEty user = userMap.get(entity.getUserId());
        if (user != null) {
            dto.setUserName(user.getName());
            dto.setUserEmail(user.getEmail());
            dto.setUserAvatar(user.getAvatar());
            log.debug("toDTO(optimized): Popolato dati utente invitato: {}", user.getName());
        } else {
            log.warn("toDTO(optimized): Utente invitato {} non trovato nella mappa", entity.getUserId());
        }
        
        // FIX N+1: Usa mappa pre-caricata per l'invitante
        UserEty inviter = userMap.get(entity.getInvitedBy());
        if (inviter != null) {
            dto.setInvitedByName(inviter.getName());
            log.debug("toDTO(optimized): Popolato invitedByName con: {}", inviter.getName());
        } else {
            log.warn("toDTO(optimized): Invitante {} non trovato nella mappa", entity.getInvitedBy());
        }
        
        return dto;
    }
    
    /**
     * Converte TravelParticipantEty in ParticipantDTO per getUserGroupTravelParticipations
     * OTTIMIZZATO: Travel e owner sono già caricati tramite fetch join, carica solo invitanti
     * @param entity Il partecipante da convertire (con travel già caricato)
     * @param inviterMap Mappa degli utenti invitanti pre-caricati
     */
    private ParticipantDTO toDTOForUserParticipations(TravelParticipantEty entity, Map<String, UserEty> inviterMap) {
        ParticipantDTO dto = new ParticipantDTO();
        dto.setId(entity.getId());
        dto.setTravelId(entity.getTravel().getId());
        dto.setUserId(entity.getUserId());
        dto.setRole(entity.getRole());
        dto.setStatus(entity.getStatus());
        dto.setInvitedBy(entity.getInvitedBy());
        dto.setInvitedAt(entity.getInvitedAt());
        dto.setRespondedAt(entity.getRespondedAt());
        
        // L'utente invitato è l'utente corrente, non serve caricarlo
        // (già disponibile nel contesto della chiamata)
        
        // FIX N+1: Usa mappa pre-caricata per l'invitante
        UserEty inviter = inviterMap.get(entity.getInvitedBy());
        if (inviter != null) {
            dto.setInvitedByName(inviter.getName());
            log.debug("toDTOForUserParticipations: Popolato invitedByName con: {}", inviter.getName());
        } else {
            log.warn("toDTOForUserParticipations: Invitante {} non trovato nella mappa", entity.getInvitedBy());
        }
        
        return dto;
    }
}
