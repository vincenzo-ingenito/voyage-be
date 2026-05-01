package it.voyage.ms.service;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.response.PrivacyStatusResponse;
import it.voyage.ms.security.user.CustomUserDetails;

public interface IUserService {

	UserDto syncUserWithFirebase(CustomUserDetails customUserDetail);
	
	UserDto update(String firebaseId,UserDto userDto);
	
	PrivacyStatusResponse getPrivacyStatus(String firebaseId);
	
	void deleteFromDb(String userId);
	
	/**
	 * Registra o aggiorna il token FCM dell'utente per le notifiche push
	 * @param firebaseId ID Firebase dell'utente
	 * @param fcmToken Token FCM del dispositivo
	 */
	void updateFcmToken(String firebaseId, String fcmToken);
	
	/**
	 * Rimuove il token FCM dell'utente (logout o disinstallazione app)
	 * @param firebaseId ID Firebase dell'utente
	 */
	void removeFcmToken(String firebaseId);
	
	/**
	 * Completa il setup post-login registrando il token FCM.
	 * Questo metodo risolve le race condition tra syncUserWithFirebase e updateFcmToken.
	 * 
	 * @param firebaseId ID Firebase dell'utente
	 * @param fcmToken Token FCM del dispositivo
	 */
	void completeLoginSetup(String firebaseId, String fcmToken);
	
}
