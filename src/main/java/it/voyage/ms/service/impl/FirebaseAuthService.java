package it.voyage.ms.service.impl;

import org.springframework.stereotype.Service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.service.IFirebaseAuthService;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FirebaseAuthService implements IFirebaseAuthService{

    /**
     * Elimina utente da Firebase Auth (FAIL-FAST)
     */
	@Override
    public void deleteUser(String userId) {
        try {
            FirebaseAuth.getInstance().deleteUser(userId);
            log.info("Utente {} eliminato da Firebase Auth", userId);
        } catch (FirebaseAuthException e) {
            log.error("Errore eliminazione utente {} da Firebase Auth: {}", userId, e.getMessage());
            throw new BusinessException("Errore durante l'eliminazione dell'utente da Firebase", e);
        }
    }
}