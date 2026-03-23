package it.voyage.ms.service.impl;

import java.util.List;

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
        List<TravelEty> travels = travelRepository.findByUserId(userId);

        userService.deleteFromDb(userId); 

        travels.forEach(travel -> {
            try { storageService.deletePhotosForTravel(travel); }
            catch (Exception e) { log.error("Errore Storage: {}", e.getMessage()); }
        });

        try {
            FirebaseAuth.getInstance().deleteUser(userId);
        } catch (FirebaseAuthException e) {
            log.error("Errore Firebase Auth per {}: {}", userId, e.getMessage());
        }

        return true;
    }
}
