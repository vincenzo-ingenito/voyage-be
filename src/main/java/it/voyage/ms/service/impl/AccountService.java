package it.voyage.ms.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IAccountService;
import it.voyage.ms.service.IFirebaseAuthService;
import it.voyage.ms.service.IFirebaseStorageService;
import it.voyage.ms.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService implements IAccountService {

    private final TravelRepository travelRepository;
    private final IUserService userService;
    private final IFirebaseStorageService storageService;
    private final IFirebaseAuthService firebaseAuthService;

    /**
     * Elimina l'account utente in modo sicuro seguendo questo ordine:
     * 1. Raccolta fileId (prima di qualsiasi delete)
     * 2. Eliminazione dal DB con cascade (transazionale, rollbackabile)
     * 3. Eliminazione da Firebase Auth (non rollbackabile, quindi dopo il DB)
     * 4. Eliminazione file da Storage (best-effort)
     *
     * @param userId ID dell'utente da eliminare
     * @throws AccountDeletionException se l'eliminazione dal DB fallisce
     */
    @Override
    public void deleteAccount(String userId) {
        log.info("Avvio eliminazione account per utente {}", userId);

        List<String> fileIds = collectUserFileIds(userId);
        log.debug("Trovati {} file da eliminare per utente {}", fileIds.size(), userId);

        // Step 2: Elimina dal DB (transazionale con cascade su viaggi, bookmark, amicizie)
        // Se fallisce qui, Firebase è ancora intatto → stato consistente garantito
        userService.deleteFromDb(userId);

        // Step 3: Elimina da Firebase Auth
        // Eseguito DOPO il DB: se fallisce, logghiamo ma il DB è già pulito
        deleteFromFirebaseAuth(userId);

        // Step 4: Elimina i file da Storage (best-effort, non bloccante)
        deleteStorageFiles(fileIds);

        log.info("Account {} eliminato con successo", userId);
    }

    /**
     * Raccoglie tutti i fileId associati ai viaggi dell'utente.
     * Deve essere chiamato PRIMA di qualsiasi operazione di delete,
     * per evitare entity detached dopo la cancellazione.
     */
    private List<String> collectUserFileIds(String userId) {
        try {
            List<TravelEty> travels = travelRepository.findByUserIdWithFiles(userId);
            if (travels == null || travels.isEmpty()) {
                return Collections.emptyList();
            }
            return travels.stream().flatMap(travel -> travel.getAllFileIds().stream()).collect(Collectors.toList());
        } catch (Exception e) {
            // Non blocchiamo la cancellazione: al peggio i file restano orfani su Storage
            log.warn("Impossibile recuperare i fileId per utente {}: {}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Elimina l'utente da Firebase Auth.
     * Chiamato dopo l'eliminazione dal DB: se fallisce, logghiamo
     * senza interrompere il flusso (l'account DB è già pulito).
     */
    private void deleteFromFirebaseAuth(String userId) {
        try {
            firebaseAuthService.deleteUser(userId);
            log.info("Utente {} eliminato da Firebase Auth con successo", userId);
        } catch (Exception e) {
            // Non rilancio: il DB è pulito, questo è un residuo su Firebase
            // Considera un job di cleanup periodico per questi casi
            log.error("Errore eliminazione utente {} da Firebase Auth: {}. " + "L'utente è già stato rimosso dal DB.", userId, e.getMessage());
        }
    }

    /**
     * Elimina i file da Firebase Storage in modalità best-effort.
     * Ogni errore viene loggato ma non interrompe il processo.
     */
    private void deleteStorageFiles(List<String> fileIds) {
        if (fileIds.isEmpty()) {
            log.debug("Nessun file da eliminare da Storage");
            return;
        }

        fileIds.forEach(fileId -> {
            try {
                storageService.getBlob(fileId).delete();
                log.info("File {} eliminato da Storage", fileId);
            } catch (Exception e) {
                // Best-effort: logghiamo ma non blocchiamo
                // Considera un job di cleanup periodico per i file orfani
                log.warn("Errore eliminazione file {} da Storage: {}", fileId, e.getMessage());
            }
        });
    }
}
