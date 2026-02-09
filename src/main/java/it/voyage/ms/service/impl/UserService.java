package it.voyage.ms.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.cloud.StorageClient;

import it.voyage.ms.dto.response.UserDto;
import it.voyage.ms.exceptions.NotFoundException;
import it.voyage.ms.repository.entity.TravelEty;
import it.voyage.ms.repository.entity.UserEty;
import it.voyage.ms.repository.impl.TravelRepository;
import it.voyage.ms.repository.impl.UserRepository;
import it.voyage.ms.security.user.CustomUserDetails;
import it.voyage.ms.service.IUserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@AllArgsConstructor
@Slf4j
public class UserService implements IUserService {

	private final UserRepository userRepository;
	private final TravelRepository travelRepository;

	@Override
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

	@Override
	public UserDto update(String firebaseId,UserDto userDto) {
		Optional<UserEty> optionalUser = userRepository.findById(firebaseId);

		if (optionalUser.isEmpty()) {
			throw new NotFoundException("Utente non presente");
		}

		UserEty user = optionalUser.get();

		if (StringUtils.isNotBlank(userDto.getBio())) {
			user.setBio(userDto.getBio());
		}

		if (userDto.getPrivateProfile() != null) {
			user.setPrivate(userDto.getPrivateProfile()); 
		}

		UserEty updatedUser = userRepository.save(user);
		return UserDto.fromEntity(updatedUser);
	}

	@Override
	public boolean getPrivacyStatus(String firebaseId) {
		Optional<UserEty> user = userRepository.findById(firebaseId);
		if (user.isEmpty()) {
			throw new NotFoundException("Utente non presente");
		}

		return user.get().isPrivate();
	}
	
	@Override
	@Transactional
	public boolean deleteAccount(String userId) {
	    log.info("Iniziando eliminazione account per userId: {}", userId);

	    try {
	        // 1. Verifica che l'utente esista
	        if (!userRepository.existsById(userId)) {
	            log.warn("Utente non trovato: {}", userId);
	            throw new NotFoundException("Utente non trovato");
	        }

	        // 2. Recupera i viaggi SOLO per eliminare le foto da Firebase Storage
	        List<TravelEty> userTravels = travelRepository.findByUserId(userId);
	        log.info("Trovati {} viaggi con foto da eliminare", userTravels.size());

	        // Elimina foto da Firebase Storage (operazione esterna al DB)
	        for (TravelEty travel : userTravels) {
	            deletePhotosFromStorage(travel);
	        }

	        // 3. Elimina l'utente - PostgreSQL CASCADE farà il resto automaticamente
	        userRepository.deleteById(userId);
	        log.info("Eliminato utente dal database PostgreSQL (con CASCADE automatico)");

	        // 4. Elimina l'utente da Firebase Authentication
	        try {
	            FirebaseAuth.getInstance().deleteUser(String.valueOf(userId));
	            log.info("Eliminato utente da Firebase Authentication");
	        } catch (FirebaseAuthException e) {
	            log.error("Errore durante l'eliminazione da Firebase Auth: {}", e.getMessage());
	        }

	        log.info("Account eliminato con successo per userId: {}", userId);
	        return true;

	    } catch (NotFoundException e) {
	        throw e;
	    } catch (Exception e) {
	        log.error("Errore durante l'eliminazione dell'account: {}", e.getMessage(), e);
	        throw new RuntimeException("Errore durante l'eliminazione dell'account", e);
	    }
	}
	
	/**
	 * Elimina le foto di un viaggio dal Firebase Storage
	 */
	private void deletePhotosFromStorage(TravelEty travel) {
		try {
			Bucket bucket = StorageClient.getInstance().bucket();
			
			
			// Elimina foto dei ricordi giornalieri
			if (travel.getItinerary()!= null) {
				travel.getItinerary().forEach(day -> {
					if (day.getMemoryImageUrl() != null && !day.getMemoryImageUrl().isEmpty()) {
						String photoPath = extractStoragePath(day.getMemoryImageUrl());
						if (photoPath != null) {
							Blob blob = bucket.get(photoPath);
							if (blob != null) {
								blob.delete();
								log.info("Eliminata foto ricordo giorno {}: {}", day.getDay(), photoPath);
							}
						}
					}
				});
			}
			
			// Elimina tutte le altre foto caricate (usando allFileIds o fileMetadataList)
			if (travel.getAllFileIds() != null && !travel.getAllFileIds().isEmpty()) {
				for (String fileId : travel.getAllFileIds()) {
					try {
						Blob blob = bucket.get(fileId);
						if (blob != null) {
							blob.delete();
							log.info("Eliminato file: {}", fileId);
						}
					} catch (Exception e) {
						log.warn("Impossibile eliminare file {}: {}", fileId, e.getMessage());
					}
				}
			}
			
		} catch (Exception e) {
			log.error("Errore durante l'eliminazione delle foto dal storage: {}", e.getMessage());
			// Non blocchiamo l'operazione se la cancellazione delle foto fallisce
		}
	}
	
	/**
	 * Estrae il path dello storage da un URL Firebase
	 */
	private String extractStoragePath(String url) {
		try {
			if (url == null || url.isEmpty()) {
				return null;
			}
			
			// Formato URL Firebase Storage: 
			// https://firebasestorage.googleapis.com/v0/b/BUCKET/o/PATH?alt=media&token=TOKEN
			if (url.contains("/o/")) {
				String[] parts = url.split("/o/");
				if (parts.length > 1) {
					String pathWithParams = parts[1];
					String path = pathWithParams.split("\\?")[0];
					// Decodifica URL encoding
					return java.net.URLDecoder.decode(path, "UTF-8");
				}
			}
			return null;
		} catch (Exception e) {
			log.error("Errore durante l'estrazione del path: {}", e.getMessage());
			return null;
		}
	}

}