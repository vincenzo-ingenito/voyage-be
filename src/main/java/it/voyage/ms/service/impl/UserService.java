package it.voyage.ms.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.exceptions.BusinessException;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IFirebaseStorageService;
import it.voyage.ms.service.IUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementazione del servizio utente.
 * Gestisce la sincronizzazione con Firebase, l'aggiornamento del profilo
 * e la cancellazione completa dell'account.
 */
@Service
@AllArgsConstructor
@Slf4j
public class UserService implements IUserService {

	private final UserRepository userRepository;
	private final TravelRepository travelRepository;
	private final IFirebaseStorageService storageService; 

	/**
	 * Sincronizza l'utente Firebase con il database locale.
	 * Se l'utente non esiste viene creato, altrimenti viene aggiornato il lastLogin.
	 */
	@Override
	@Transactional
	public UserDto syncUserWithFirebase(CustomUserDetails customUserDetails) {
		Optional<UserEty> existingUser = userRepository.findById(customUserDetails.getUserId());
		UserEty user;

		if (existingUser.isPresent()) {
			user = existingUser.get();
			user.setLastLogin(new Date());
		} else {
			user = new UserEty();
			user.setId(customUserDetails.getUserId());
			user.setName(customUserDetails.getFullName());
			user.setEmail(customUserDetails.getEmail());
			user.setAvatar(customUserDetails.getAvatarUrl());
			user.setCreatedAt(new Date());
			user.setLastLogin(new Date());
			user.setPrivate(true);
			user.setBio("Nessuna biografia disponibile");
		}

		UserEty savedUser = userRepository.save(user);
		return UserDto.fromEntity(savedUser);
	}

	/**
	 * Aggiorna i dati del profilo utente (bio e visibilità).
	 */
	@Override
	@Transactional
	public UserDto update(String firebaseId, UserDto userDto) {
		UserEty user = userRepository.findById(firebaseId).orElseThrow(() -> new NotFoundException("Utente non presente"));

		if (StringUtils.isNotBlank(userDto.getBio())) {
			user.setBio(userDto.getBio());
		}

		if (userDto.getPrivateProfile() != null) {
			user.setPrivate(userDto.getPrivateProfile());
		}

		UserEty updatedUser = userRepository.save(user);
		return UserDto.fromEntity(updatedUser);
	}

	/**
	 * Restituisce lo stato di privacy del profilo utente.
	 *
	 */
	@Override
	public boolean getPrivacyStatus(String firebaseId) {
	    return userRepository.findPrivacyStatusById(firebaseId).orElseThrow(() -> new NotFoundException("Utente non presente"));
	}

	/**
	 * Elimina completamente l'account utente:
	 * 1. Verifica esistenza utente
	 * 2. Elimina file da Firebase Storage
	 * 3. Elimina l'utente da Firebase Authentication (prima del DB, così in caso di
	 *    errore il DB rimane integro e l'operazione è ripetibile)
	 * 4. Elimina l'utente dal DB (il CASCADE PostgreSQL si occupa delle entità figlie)
	 *
	 */
	@Override
	@Transactional
	public boolean deleteAccount(String userId) {
		log.info("Iniziando eliminazione account per userId: {}", userId);

		// 1. Verifica esistenza utente
		if (!userRepository.existsById(userId)) {
			log.warn("Utente non trovato: {}", userId);
			throw new NotFoundException("Utente non trovato");
		}

		// 2. Elimina foto da Firebase Storage tramite StorageService dedicato
		List<TravelEty> userTravels = travelRepository.findByUserId(userId);
		log.info("Trovati {} viaggi con foto da eliminare", userTravels.size());

		userTravels.forEach(storageService::deletePhotosForTravel);

		try {
			FirebaseAuth.getInstance().deleteUser(userId);
		} catch (FirebaseAuthException e) {
			throw new BusinessException("Errore durante l'eliminazione da firebase");
		}

		// 4. Elimina l'utente dal DB (CASCADE automatico su PostgreSQL)
		userRepository.deleteById(userId);

		log.info("Account eliminato con successo per userId: {}", userId);
		return true;

	}
}