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


	@Transactional
	public void deleteFromDb(String userId) {
		if (!userRepository.existsById(userId)) {
			throw new NotFoundException("Utente non trovato");
		}
		userRepository.deleteById(userId);
	}
}