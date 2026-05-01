package it.voyage.ms.service.impl;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.response.PrivacyStatusResponse;
import it.voyage.ms.security.user.CustomUserDetails;
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

	/**
	 * Sincronizza l'utente Firebase con il database locale.
	 * Se l'utente non esiste viene creato, altrimenti viene aggiornato il lastLogin.
	 * IMPORTANTE: Preserva il token FCM esistente durante la sincronizzazione.
	 */
	@Override
	@Transactional
	public UserDto syncUserWithFirebase(CustomUserDetails customUserDetails) {
		Optional<UserEty> existingUser = userRepository.findById(customUserDetails.getUserId());
		UserEty user;

		if (existingUser.isPresent()) {
			// Utente esistente: aggiorna solo lastLogin e preserva il token FCM
			user = existingUser.get();
			user.setLastLogin(new Date());
			
			// Log per debug
			if (user.getFcmToken() != null && !user.getFcmToken().isEmpty()) {
				log.debug("✅ Token FCM preservato per utente {} durante sync", customUserDetails.getUserId());
			} else {
				log.debug("⚠️ Utente {} non ha token FCM durante sync", customUserDetails.getUserId());
			}
		} else {
			// Nuovo utente: crea record senza token FCM (verrà aggiunto dopo)
			user = new UserEty();
			user.setId(customUserDetails.getUserId());
			user.setName(customUserDetails.getFullName());
			user.setEmail(customUserDetails.getEmail());
			user.setAvatar(customUserDetails.getAvatarUrl());
			user.setCreatedAt(new Date());
			user.setLastLogin(new Date());
			user.setPrivate(true);
			user.setBio("Nessuna biografia disponibile");
			// fcmToken rimane null - verrà impostato dalla chiamata successiva
			
			log.info("🆕 Nuovo utente creato: {} - Token FCM sarà registrato dopo il login", customUserDetails.getUserId());
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

		if (userDto.getShowEmergencyFAB() != null) {
			user.setShowEmergencyFAB(userDto.getShowEmergencyFAB());
		}

		UserEty updatedUser = userRepository.save(user);
		return UserDto.fromEntity(updatedUser);
	}

	/**
	 * Restituisce lo stato di privacy e delle preferenze del profilo utente.
	 *
	 */
	@Override
	public PrivacyStatusResponse getPrivacyStatus(String firebaseId) {
		UserEty user = userRepository.findById(firebaseId)
			.orElseThrow(() -> new NotFoundException("Utente non presente"));
		return new PrivacyStatusResponse(user.isPrivate(), user.isShowEmergencyFAB());
	}


	@Transactional
	public void deleteFromDb(String userId) {
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("Utente non trovato");
		}
		userRepository.deleteById(userId);
	}

	@Override
	@Transactional
	public void updateFcmToken(String firebaseId, String fcmToken) {
		log.info("Aggiornamento token FCM per utente: {}", firebaseId);
		
		UserEty user = userRepository.findById(firebaseId)
			.orElseThrow(() -> new NotFoundException("Utente non trovato"));
		
		user.setFcmToken(fcmToken);
		userRepository.save(user);
		
		log.info("Token FCM aggiornato con successo per utente: {}", firebaseId);
	}

	@Override
	@Transactional
	public void removeFcmToken(String firebaseId) {
		log.info("Rimozione token FCM per utente: {}", firebaseId);
		
		UserEty user = userRepository.findById(firebaseId)
			.orElseThrow(() -> new NotFoundException("Utente non trovato"));
		
		user.setFcmToken(null);
		userRepository.save(user);
		
		log.info("Token FCM rimosso con successo per utente: {}", firebaseId);
	}

	@Override
	@Transactional
	public void completeLoginSetup(String firebaseId, String fcmToken) {
		log.info("🔧 Completamento setup login per utente: {}", firebaseId);
		log.debug("🔧 Token FCM da registrare (primi 30 char): {}", 
			fcmToken != null && fcmToken.length() > 30 ? fcmToken.substring(0, 30) + "..." : fcmToken);
		
		UserEty user = userRepository.findById(firebaseId)
			.orElseThrow(() -> new NotFoundException("Utente non trovato durante setup login"));
		
		// Registra il token FCM se fornito
		if (fcmToken != null && !fcmToken.isEmpty()) {
			String oldToken = user.getFcmToken();
			user.setFcmToken(fcmToken);
			userRepository.save(user);
		} else {
			log.warn("⚠️ Setup login chiamato senza token FCM per utente: {}", firebaseId);
		}
	}
}
