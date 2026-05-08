package it.voyage.ms.controller.impl;

import it.voyage.ms.controller.ITravelLockCtl;
import it.voyage.ms.dto.response.TravelLockDTO;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.ITravelLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST per gestire i lock sui viaggi di gruppo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TravelLockCtl implements ITravelLockCtl {

    private final ITravelLockService lockService;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<TravelLockDTO> acquireLock(Long travelId, CustomUserDetails userDetails) {
        log.info("POST /api/travel/{}/lock - Acquisizione lock da utente {}", travelId, userDetails.getUserId());
        
        // Recupera il nome utente dal database
        String userName = userRepository.findById(userDetails.getUserId())
            .map(UserEty::getName)
            .orElse("Utente");
        
        TravelLockDTO lockDTO = lockService.acquireLock(
            travelId, 
            userDetails.getUserId(), 
            userName
        );
        
        return ResponseEntity.ok(lockDTO);
    }

    @Override
    public ResponseEntity<Void> releaseLock(Long travelId, CustomUserDetails userDetails) {
        log.info("DELETE /api/travel/{}/lock - Rilascio lock da utente {}", travelId, userDetails.getUserId());
        
        lockService.releaseLock(travelId, userDetails.getUserId());
        
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<TravelLockDTO> heartbeat(Long travelId, CustomUserDetails userDetails) {
        log.debug("PUT /api/travel/{}/lock/heartbeat - Heartbeat da utente {}", travelId, userDetails.getUserId());
        
        TravelLockDTO lockDTO = lockService.heartbeat(travelId, userDetails.getUserId());
        
        return ResponseEntity.ok(lockDTO);
    }

    @Override
    public ResponseEntity<TravelLockDTO> getLockStatus(Long travelId, CustomUserDetails userDetails) {
        log.debug("GET /api/travel/{}/lock/status - Verifica stato lock", travelId);
        
        TravelLockDTO lockDTO = lockService.getLockStatus(travelId, userDetails.getUserId());
        
        return ResponseEntity.ok(lockDTO);
    }
}