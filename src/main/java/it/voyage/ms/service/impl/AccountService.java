package it.voyage.ms.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.service.IAccountService;
import it.voyage.ms.service.IFirebaseStorageService;
import it.voyage.ms.service.IUserService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountService implements IAccountService {
	
	@Autowired
	private TravelRepository travelRepository;
	
	@Autowired
	private IUserService userService;
	
	@Autowired
	private IFirebaseStorageService storageService;
	
	@Override
	public boolean deleteAccount(String userId) {
	    
	    // 1. Elimina prima l'utente da Firebase Auth
	    // Se fallisce qui, non abbiamo ancora toccato il DB → stato consistente
	    try {
	        FirebaseAuth.getInstance().deleteUser(userId);
	        log.info("Utente {} eliminato da Firebase Auth", userId);
	    } catch (FirebaseAuthException e) {
	        log.error("Errore eliminazione utente {} da Firebase Auth: {}", userId, e.getMessage());
	        return false; // interrompi, DB intatto
	    }

	    // 2. Carica i viaggi PRIMA di eliminare l'utente dal DB
	    // e inizializza le lazy collections mentre la sessione è ancora attiva
	    List<TravelEty> travels = travelRepository.findByUserIdWithFiles(userId);
	    List<String> allFileIds = travels.stream().flatMap(t -> t.getAllFileIds().stream()).collect(Collectors.toList());

	    // 3. Elimina l'utente dal DB (cascade elimina viaggi, bookmark, amicizie)
	    try {
	        userService.deleteFromDb(userId);
	        log.info("Utente {} eliminato dal DB", userId);
	    } catch (Exception e) {
	        log.error("Errore eliminazione utente {} dal DB: {}", userId, e.getMessage());
	        // Firebase già eliminato, logghiamo ma non possiamo rollbackare Firebase
	        return false;
	    }

	    // 4. Elimina i file da Firebase Storage (best-effort, DB già pulito)
	    // Usiamo i fileIds raccolti prima della delete, evitiamo entity detached
	    allFileIds.forEach(fileId -> {
	        try {
	            storageService.getBlob(fileId).delete();
	            log.info("File {} eliminato da Storage", fileId);
	        } catch (Exception e) {
	            log.warn("Errore eliminazione file {} da Storage: {}", fileId, e.getMessage());
	        }
	    });

	    log.info("Account {} eliminato con successo", userId);
	    return true;
	}
}
