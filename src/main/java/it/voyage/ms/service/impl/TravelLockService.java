package it.voyage.ms.service.impl;

import it.voyage.ms.dto.response.TravelLockDTO;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.repository.entity.TravelLockEty;
import it.voyage.ms.repository.impl.TravelLockRepository;
import it.voyage.ms.service.ITravelLockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelLockService implements ITravelLockService {

    private final TravelLockRepository lockRepository;

    // Durata del lock: 5 minuti
    private static final int LOCK_DURATION_MINUTES = 5;
    
    // Timeout heartbeat: se non riceve heartbeat per 2 minuti, il lock scade
    private static final int HEARTBEAT_TIMEOUT_MINUTES = 2;

    @Override
    @Transactional
    public TravelLockDTO acquireLock(Long travelId, String userId, String userName) {
        log.info("🔒 Tentativo acquisizione lock per viaggio {} da utente {}", travelId, userId);

        LocalDateTime now = LocalDateTime.now();

        // Verifica se esiste già un lock attivo
        Optional<TravelLockEty> existingLockOpt = lockRepository.findByTravelId(travelId);

        if (existingLockOpt.isPresent()) {
            TravelLockEty existingLock = existingLockOpt.get();

            // Se il lock è scaduto, lo eliminiamo
            if (existingLock.isExpired()) {
                log.info("🧹 Lock scaduto, rimozione...");
                lockRepository.delete(existingLock);
            } else if (existingLock.isOwnedBy(userId)) {
                // L'utente ha già il lock, lo rinnoviamo
                log.info("🔄 Utente ha già il lock, rinnovo...");
                existingLock.setExpiresAt(now.plusMinutes(LOCK_DURATION_MINUTES));
                existingLock.setLastHeartbeatAt(now);
                TravelLockEty renewed = lockRepository.save(existingLock);
                return toLockDTO(renewed, userId);
            } else {
                // Un altro utente ha il lock
                log.warn("❌ Lock già acquisito da altro utente: {}", existingLock.getLockedByUserName());
                throw new BusinessException(
                    String.format("Il viaggio è in modifica da %s. Riprova tra qualche minuto.", 
                        existingLock.getLockedByUserName())
                );
            }
        }

        // Crea nuovo lock
        TravelLockEty newLock = new TravelLockEty();
        newLock.setTravelId(travelId);
        newLock.setLockedByUserId(userId);
        newLock.setLockedByUserName(userName);
        newLock.setLockedAt(now);
        newLock.setExpiresAt(now.plusMinutes(LOCK_DURATION_MINUTES));
        newLock.setLastHeartbeatAt(now);

        TravelLockEty saved = lockRepository.save(newLock);
        log.info("✅ Lock acquisito con successo per viaggio {}", travelId);

        return toLockDTO(saved, userId);
    }

    @Override
    @Transactional
    public void releaseLock(Long travelId, String userId) {
        log.info("🔓 Rilascio lock per viaggio {} da utente {}", travelId, userId);

        Optional<TravelLockEty> lockOpt = lockRepository.findByTravelId(travelId);

        if (lockOpt.isEmpty()) {
            log.warn("⚠️ Nessun lock trovato per viaggio {}", travelId);
            return;
        }

        TravelLockEty lock = lockOpt.get();

        if (!lock.isOwnedBy(userId)) {
            log.warn("❌ Utente {} non è proprietario del lock per viaggio {}", userId, travelId);
            throw new BusinessException("Non puoi rilasciare un lock che non possiedi");
        }

        lockRepository.delete(lock);
        log.info("✅ Lock rilasciato con successo");
    }

    @Override
    @Transactional
    public TravelLockDTO heartbeat(Long travelId, String userId) {
        log.debug("💓 Heartbeat per viaggio {} da utente {}", travelId, userId);

        Optional<TravelLockEty> lockOpt = lockRepository.findByTravelId(travelId);

        if (lockOpt.isEmpty()) {
            log.warn("⚠️ Nessun lock trovato per heartbeat");
            throw new BusinessException("Lock non trovato");
        }

        TravelLockEty lock = lockOpt.get();

        if (!lock.isOwnedBy(userId)) {
            log.warn("❌ Heartbeat da utente non proprietario del lock");
            throw new BusinessException("Non sei il proprietario del lock");
        }

        // Aggiorna heartbeat e rinnova scadenza
        LocalDateTime now = LocalDateTime.now();
        lock.setLastHeartbeatAt(now);
        lock.setExpiresAt(now.plusMinutes(LOCK_DURATION_MINUTES));

        TravelLockEty updated = lockRepository.save(lock);
        log.debug("💚 Heartbeat aggiornato, lock rinnovato fino a {}", updated.getExpiresAt());

        return toLockDTO(updated, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public TravelLockDTO getLockStatus(Long travelId, String currentUserId) {
        log.debug("🔍 Verifica stato lock per viaggio {}", travelId);

        Optional<TravelLockEty> lockOpt = lockRepository.findByTravelId(travelId);

        if (lockOpt.isEmpty()) {
            log.debug("✅ Nessun lock attivo per viaggio {}", travelId);
            return TravelLockDTO.unlocked(travelId);
        }

        TravelLockEty lock = lockOpt.get();

        // Se il lock è scaduto, lo consideriamo come non presente
        if (lock.isExpired()) {
            log.debug("⏰ Lock scaduto per viaggio {}", travelId);
            return TravelLockDTO.unlocked(travelId);
        }

        return toLockDTO(lock, currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean canEdit(Long travelId, String userId) {
        Optional<TravelLockEty> lockOpt = lockRepository.findByTravelId(travelId);

        if (lockOpt.isEmpty()) {
            // Nessun lock = può modificare
            return true;
        }

        TravelLockEty lock = lockOpt.get();

        // Se il lock è scaduto, può modificare
        if (lock.isExpired()) {
            return true;
        }

        // Può modificare solo se è il proprietario del lock
        return lock.isOwnedBy(userId);
    }

    @Override
    @Transactional
    @Scheduled(fixedRate = 60000) // Ogni minuto
    public void cleanupExpiredLocks() {
        log.debug("🧹 Pulizia lock scaduti...");
        LocalDateTime now = LocalDateTime.now();
        lockRepository.deleteExpiredLocks(now);
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    /**
     * Converte TravelLockEty in TravelLockDTO
     */
    private TravelLockDTO toLockDTO(TravelLockEty entity, String currentUserId) {
        TravelLockDTO dto = new TravelLockDTO();
        dto.setTravelId(entity.getTravelId());
        dto.setLocked(true);
        dto.setLockedByUserId(entity.getLockedByUserId());
        dto.setLockedByUserName(entity.getLockedByUserName());
        dto.setLockedAt(entity.getLockedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setOwnedByCurrentUser(entity.isOwnedBy(currentUserId));
        return dto;
    }
}